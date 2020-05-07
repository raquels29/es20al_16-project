describe('Proposed Question walkthrough', () => {
  beforeEach(() => {
    cy.openProposeQuestionStudentMenu();
    cy.createProposedQuestion('TEST', 'Question', 'Options', 1);
    cy.showProposedQuestion('TEST');
    cy.contains('Logout').click();
    cy.openProposedQuestionsMenu();
  });

  afterEach(() => {
    cy.contains('Logout').click();
    cy.openProposeQuestionStudentMenu();
    cy.deleteProposedQuestion('TEST');
    cy.contains('Logout').click();
  });

  it('evaluate awaiting proposed question', () => {
    cy.evaluate('TEST', 'APPROVED', 'Justification');
    cy.get('[data-cy="search"').clear();

    cy.evaluate('TEST', 'AWAITING', ' ');
  });

  it('evaluate approved proposed question', () => {
    cy.evaluate('TEST', 'APPROVED', 'Justification');
    cy.get('[data-cy="search"').clear();
    cy.evaluate('TEST', 'REJECTED', 'Justification');
    cy.get('[data-cy="search"').clear();
  });

  it('reject awaiting proposed question without justification', () => {
    cy.evaluate('TEST', 'REJECTED', ' ');
    cy.closeErrorMessage();
    cy.get('[data-cy="cancelButton"]').click();
  });

  it('student resubmit rejected proposed question', () => {
    cy.evaluate('TEST', 'REJECTED', 'Justification');
    cy.openProposeQuestionStudentMenu();
    cy.updateProposedQuestion('TEST', 'New_Content');
  });
});