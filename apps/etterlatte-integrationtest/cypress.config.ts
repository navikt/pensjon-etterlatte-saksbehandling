const { defineConfig } = require('cypress')

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {},
    experimentalModifyObstructiveThirdPartyCode: true,
    baseUrl: 'https://etterlatte-saksbehandling.dev.intern.nav.no',
    testIsolation: false,
  },
  env: {
    doffen_email: 'Må overskrives av OS env variabel med navn cypress_doffen_username',
    doffen_password: 'Må overskrives av OS env variabel med navn cypress_doffen_password',
  },
  video: false,
  screenshotOnRunFailure: false,
  chromeWebSecurity: false,
  viewportWidth: 1500,
})
