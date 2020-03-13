package pt.ulisboa.tecnico.socialsoftware.tutor.question;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.Course;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.ProposedQuestion;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Topic;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.ProposedQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User;
import pt.ulisboa.tecnico.socialsoftware.tutor.user.UserRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class QuestionProposalService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private QuestionService questionService;

    @PersistenceContext
    EntityManager entityManager;

    private User getStudent(ProposedQuestionDto proposedQuestionDto) {
        Integer studentId = proposedQuestionDto.getStudentId();
        if (studentId == null) {
            throw new TutorException(ErrorMessage.USER_IS_EMPTY);
        }
        return userRepository.findById(studentId).orElseThrow(() -> new TutorException(ErrorMessage.USER_NOT_FOUND, studentId));
    }

    private Question createQuestion(int courseId, ProposedQuestionDto proposedQuestionDto) {
        if (proposedQuestionDto.getQuestion() == null) {
            throw new TutorException(ErrorMessage.PROPQUESTION_MISSING_QUESTION);
        }
        QuestionDto questionDto = questionService.createQuestion(courseId, proposedQuestionDto.getQuestion());
        Question question = questionRepository.findByKey(questionDto.getKey()).orElseThrow(() -> new TutorException(ErrorMessage.QUESTION_NOT_FOUND));
        addTopicsToQuestion(proposedQuestionDto, question);
        return question;
    }

    private void addTopicsToQuestion(ProposedQuestionDto proposedQuestionDto, Question question) {
        if (proposedQuestionDto.getQuestion().getTopics() != null) {
            for (TopicDto topicDto: proposedQuestionDto.getQuestion().getTopics()) {
                Topic topic = topicRepository.findTopicByName(question.getCourse().getId(), topicDto.getName());
                question.addTopic(topic);
            }
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ProposedQuestionDto studentSubmitQuestion(int courseId, ProposedQuestionDto proposedQuestionDto) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new TutorException(ErrorMessage.COURSE_NOT_FOUND, courseId));

        User student = getStudent(proposedQuestionDto);
        ProposedQuestion proposedQuestion = new ProposedQuestion(student, course);

        Question question = createQuestion(courseId, proposedQuestionDto);
        proposedQuestion.addQuestion(question);

        this.entityManager.persist(proposedQuestion);
        return new ProposedQuestionDto(proposedQuestion);
    }
}
