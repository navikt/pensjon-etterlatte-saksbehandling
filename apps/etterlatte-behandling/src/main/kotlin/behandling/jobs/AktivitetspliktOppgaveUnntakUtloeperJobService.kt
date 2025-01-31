package no.nav.etterlatte.behandling.jobs

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.aktivitetsplikt.AktivitetspliktDao
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory

class AktivitetspliktOppgaveUnntakUtloeperJobService(
    private val aktivitetspliktDao: AktivitetspliktDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    fun run() {
        newSingleThreadContext("aktivitetspliktOppgaveUnntakUtloeperJob").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                // TODO: opprett unntak utloeper oppgaver

                // TODO: hent sak_id for saker med aktivitetsplikt_unntakt, med tom dato satt

                // TODO: sjekk at vi ikke har opprettet oppgave for det
                // TODO: - aktivitetsplikt_unntak.id IKKE eksisterer i oppgave.referanse && oppgave.type = OPPFOELGING
            }
        }
    }

    private fun hentSakerMedAktivitetspliktUnntakMedTom(): List<SakId> =
        aktivitetspliktDao.finnSakerKlarForOppfoelgingsoppgaveVarigUnntakUtloeper()
}
