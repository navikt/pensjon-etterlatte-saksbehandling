package no.nav.etterlatte

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val app = AppBuilder()

    RapidApplication.create(app.env)
        .apply {
            JournalfoerBrev(this, app.journalpostService)
            DistribuerBrev(this, app.distribusjonService)
        }
        .start()
}