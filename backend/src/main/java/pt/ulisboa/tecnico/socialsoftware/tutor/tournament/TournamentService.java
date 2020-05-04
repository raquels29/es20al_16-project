package pt.ulisboa.tecnico.socialsoftware.tutor.tournament;

import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseExecutionRepository;


import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Topic;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.QuizService;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.repository.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.statement.dto.StatementQuizDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.tournament.domain.Tournament;
import pt.ulisboa.tecnico.socialsoftware.tutor.tournament.dto.TournamentDto;

import pt.ulisboa.tecnico.socialsoftware.tutor.tournament.repository.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.*;

@Service
public class TournamentService {
   @Autowired
   private CourseExecutionRepository courseExecutionRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private TournamentRepository tournamentRepository;

   @Autowired
   private TopicRepository topicRepository;

   @Autowired
   private QuizRepository quizRepository;

   @Autowired
   private QuizService quizService;

   @Autowired
   private QuestionRepository questionRepository;

   @Transactional(isolation = Isolation.REPEATABLE_READ)
   public TournamentDto createTournament(int executionId, int creatorId, TournamentDto tournamentDto){
      CourseExecution courseExecution = courseExecutionRepository.findById(executionId).orElseThrow(() -> new TutorException(COURSE_EXECUTION_NOT_FOUND, executionId));

      //Quiz quiz = getQuiz(tournamentDto);
      User creatorUser = getCreator(creatorId);
      checkCreatorCourse(courseExecution, creatorUser);

      Set<Topic> topics = getTopics(tournamentDto);


      Tournament tournament = saveTournament(tournamentDto, courseExecution, creatorUser, topics);
      creatorUser.addTournament(tournament);
      //if(quiz!=null)quiz.addTournament(tournament);
      courseExecution.addTournament(tournament);

      pickRandomQuestions(tournament);//throws error if not enough questions

      return new TournamentDto(new UserDto(creatorUser), tournamentDto.getTopics(), tournament);

   }
   /*
   private Quiz getQuiz(TournamentDto tournamentDto) {
      if(tournamentDto.getQuiz()==null){
         return null;
         //throw new TutorException(TOURNAMENT_QUIZ_NOT_FOUND);
      }
      return quizRepository.findById(tournamentDto.getQuiz().getId()).orElseThrow(() -> new TutorException(QUIZ_NOT_FOUND, tournamentDto.getQuiz().getId()));
   }*/

   private Set<Topic> getTopics(TournamentDto tournamentDto) {
      if(tournamentDto.getTopics() == null){ tournamentDto.setTopics(new ArrayList<>(Arrays.asList())); }

      Set<Topic> topics = new HashSet<>();
      List<TopicDto> topicsDto = tournamentDto.getTopics();
      for(TopicDto topicDto: topicsDto){
         Topic topic = topicRepository.findById(topicDto.getId()).orElseThrow(() -> new TutorException(TOPIC_NOT_FOUND, topicDto.getId()));
         topics.add(topic);
      }

      return topics;
   }

   private Tournament saveTournament(TournamentDto tournamentDto, CourseExecution courseExecution, User creatorUser, Set<Topic> topics) {
      Tournament tournament = new Tournament(creatorUser, courseExecution, tournamentDto);
      tournament.setTopics(topics);

      tournamentRepository.save(tournament);
      return tournament;
   }

   private void checkCreatorCourse(CourseExecution courseExecution, User creatorUser) {
      if (!creatorUser.getCourseExecutions().stream().anyMatch(ce -> ce.getId().equals(courseExecution.getId()))) {
         throw new TutorException(TOURNAMENT_CREATOR_NOT_ENROLLED);
      }
   }

   private User getCreator(int creatorId) {
      if(creatorId<=0){
         throw new TutorException(TOURNAMENT_NO_CREATOR);
      }
      User creatorUser = userRepository.findById(creatorId).orElseThrow(() -> new TutorException(USER_NOT_FOUND, creatorId));
      if(creatorUser.getRole() != User.Role.STUDENT){
         throw new TutorException(TOURNAMENT_CREATOR_NOT_STUDENT);
      }
      return creatorUser;
   }


   @Transactional(isolation = Isolation.REPEATABLE_READ)
   public TournamentDto enrollStudent(int tournamentId, int studentId) {
      Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new TutorException(TOURNAMENT_NOT_FOUND, tournamentId));
      User user = getUser(tournament, studentId);

      checkUserInCourseExecution(user, tournament.getCourseExecution(), USER_NOT_IN_COURSE_EXECUTION);
      if (tournament.getStatus() == Tournament.Status.CLOSED) { throw new TutorException(TOURNAMENT_CLOSED); }

      enrollStud(tournament, user);

      if(tournament.getQuiz()==null && tournament.getEnrolled().size() > 1){
         generateQuiz(tournament, user);
      }

      List<UserDto> enrolled = getUserDtos(tournament);
      TournamentDto tDto = new TournamentDto(tournament);
      //tDto.setEnrolled(enrolled);

      return tDto;

   }

   private void generateQuiz(Tournament tournament, User user){
      Quiz quiz = new Quiz();
      quiz.setKey(quizService.getMaxQuizKey() + 1);
      quiz.setType(Quiz.QuizType.TOURNAMENT);
      quiz.setCreationDate(LocalDateTime.now());

      int executionId = tournament.getCourseExecution().getId();
      CourseExecution courseExecution = courseExecutionRepository.findById(executionId).orElseThrow(() -> new TutorException(COURSE_EXECUTION_NOT_FOUND, executionId));

      List<Question> availableQuestions = pickRandomQuestions(tournament);

      quiz.generate(availableQuestions);
      quiz.setCourseExecution(courseExecution);
      quizRepository.save(quiz);
      tournament.setQuiz(quiz);
   }



   public List<Question> pickRandomQuestions(Tournament tournament){
      List<Question> availableQuestions = questionRepository.findAvailableQuestions(tournament.getCourseExecution().getCourse().getId());

      Set<Topic> topics = tournament.getTopics();

      availableQuestions = availableQuestions.stream().filter(question -> question.hasAnyTopics(topics)).collect(Collectors.toList());

      if (availableQuestions.size() < tournament.getNumberOfQuestions()) {
         throw new TutorException(NOT_ENOUGH_QUESTIONS);
      }

      List<Question> copy = new ArrayList<>(availableQuestions);
      Collections.shuffle(copy);
      return copy.subList(0, tournament.getNumberOfQuestions());
   }

   private void enrollStud(Tournament tournament, User user) {
      if(tournament.getEnrolled().stream().anyMatch(u -> u.getId().equals(user.getId()))){
         throw new TutorException(STUDENT_ALREADY_ENROLLED);
      }
      tournament.addStudent(user);
      user.enrollTournament(tournament);
   }

   private void checkUserInCourseExecution(User user, CourseExecution courseExecution, ErrorMessage em) {
      if (!user.getCourseExecutions().stream()
              .anyMatch(t -> t.getId().equals(courseExecution.getId()))) {
         throw new TutorException(em);
      }
   }

   private List<UserDto> getUserDtos(Tournament tournament) {
      List<UserDto> enrolled = new ArrayList<>();
      Set<User> users = tournament.getEnrolled();
      for(User u: users){
         UserDto userDto = new UserDto(u);
         enrolled.add(userDto);
      }
      return enrolled;
   }

   private User getUser(Tournament tournament, int studentId) {
      if(studentId<=0){ throw new TutorException(TOURNAMENT_NO_STUDENT_TO_ENROLL);}
      User user = userRepository.findById(studentId).orElseThrow(() -> new TutorException(USER_NOT_FOUND, tournament.getCreator().getId()));
      if (user.getRole() != User.Role.STUDENT) {
         throw new TutorException(TOURNAMENT_ENROLLED_NOT_STUDENT);
      }
      if(user.getUsername() == null || user.getUsername().trim().isEmpty()){
         throw new TutorException(INVALID_USERNAME);
      }
      return user;
   }

   @Transactional(isolation = Isolation.REPEATABLE_READ)
   public List<TournamentDto> listOpenTournaments(int executionId){
      CourseExecution courseExecution = courseExecutionRepository.findById(executionId).orElseThrow(() -> new TutorException(COURSE_EXECUTION_NOT_FOUND, executionId));

      return courseExecution.getTournaments().stream()
              .filter(tournament -> tournament.getStatus() == Tournament.Status.CREATED)
              .sorted(Comparator.comparing(Tournament::getStartDate))
              .map(TournamentDto::new)
              .collect(toList());

   }
}
