describe('Skal kunne behandle en førstegangssøknad for barnepensjon', function () {
  before('Login', () => {
    cy.origin('https://login.microsoftonline.com/', () => {
      cy.visit('/')
      cy.get('input[type="email"]').type(Cypress.env('doffen_email'))
      cy.get('input[type="submit"]').click()
      cy.get('input[type="password"]').type(Cypress.env('doffen_password'), { force: true })
      cy.get('input[type="submit"]').click()
      cy.get('input[type="submit"]').click()
    })
  })

  it('Skal vise oppgavebenken og hente oppgaveliste', () => {
    cy.visit('/')
    cy.contains('h1', 'Oppgavebenken')
    cy.contains('h2', 'Oppgaveliste')
  })

  it('Skal starte behandling av en oppgave', () => {
    cy.visit('/')
    cy.get('tbody > :nth-child(1) > :nth-child(8) > .navds-button').click()
    cy.get('.sc-cabOPr > .navds-heading')
  })
})
