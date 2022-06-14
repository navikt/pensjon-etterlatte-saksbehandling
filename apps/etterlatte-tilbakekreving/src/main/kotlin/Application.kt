package no.nav.etterlatte

import no.nav.etterlatte.tilbakekreving.config.ApplicationContext

fun main() {
    startApplication(ApplicationContext())
}

fun startApplication(applicationContext: ApplicationContext) {
    // TODO her må vi gjøre noe lurt - ønsker å lukke connections etc før applikasjonen lukkes
    applicationContext.tilbakekrevingConsumer
}
