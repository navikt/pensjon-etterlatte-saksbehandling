package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.configFromEnvironment
import no.nav.etterlatte.rivers.DistribuerBrevRiver
import no.nav.etterlatte.rivers.FerdigstillJournalfoerOgDistribuerBrev
import no.nav.etterlatte.rivers.JournalfoerVedtaksbrevRiver
import no.nav.etterlatte.rivers.OpprettJournalfoerOgDistribuerRiver
import no.nav.etterlatte.rivers.SamordningsnotatRiver
import no.nav.etterlatte.rivers.StartInformasjonsbrevgenereringRiver
import no.nav.etterlatte.rivers.VedtaksbrevUnderkjentRiver
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.initRogR

fun main() {
}

fun buildRapisApp() {
    initRogR(
        applikasjonsnavn = "brev-kafka",
        configFromEnvironment = { configFromEnvironment(it) },
    ) { rapidsConnection, _ ->
        val brevgenerering =
            StartInformasjonsbrevgenereringRiver(
                brevgenereringRepository,
                rapidsConnection,
            )
        rapidsConnection.register(
            object : RapidsConnection.StatusListener {
                override fun onStartup(rapidsConnection: RapidsConnection) {
                    brevgenerering.init()
                }
            },
        )
        val ferdigstillJournalfoerOgDistribuerBrev =
            FerdigstillJournalfoerOgDistribuerBrev(
                pdfGenerator,
                journalfoerBrevService,
                brevdistribuerer,
            )
        OpprettJournalfoerOgDistribuerRiver(
            rapidsConnection,
            grunnlagService,
            brevoppretter,
            ferdigstillJournalfoerOgDistribuerBrev,
        )

        JournalfoerVedtaksbrevRiver(rapidsConnection, journalfoerBrevService)
        VedtaksbrevUnderkjentRiver(rapidsConnection, vedtaksbrevService)
        DistribuerBrevRiver(rapidsConnection, brevdistribuerer)
        SamordningsnotatRiver(rapidsConnection, nyNotatService)
    }
}
