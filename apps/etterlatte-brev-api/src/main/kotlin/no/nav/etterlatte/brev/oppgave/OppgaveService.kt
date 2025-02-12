package no.nav.etterlatte.brev.oppgave

import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.behandlingklient.OppgaveKlient
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class OppgaveService(
    private val oppgaveKlient: OppgaveKlient,
) {
    private val logger = LoggerFactory.getLogger(OppgaveService::class.java)

    suspend fun opprettOppgaveForFeiletBrev(
        sakId: SakId,
        brevID: BrevID,
        brukerTokenInfo: BrukerTokenInfo,
        brevKode: Brevkoder,
    ): OppgaveIntern {
        logger.info("Oppretter oppgave for brev som feilet (sakId=$sakId, brevID=$brevID)")

        val oppgaveKilde =
            when (brevKode) {
                Brevkoder.BP_INFORMASJON_DOEDSFALL -> OppgaveKilde.DOEDSHENDELSE
                Brevkoder.BP_INFORMASJON_DOEDSFALL_MELLOM_ATTEN_OG_TJUE_VED_REFORMTIDSPUNKT -> OppgaveKilde.DOEDSHENDELSE
                Brevkoder.OMS_INFORMASJON_DOEDSFALL -> OppgaveKilde.DOEDSHENDELSE
                else -> throw InternfeilException("Brevkode støttes ikke $brevKode for sak $sakId brevid: $brevID")
            }

        val nyOppgave =
            NyOppgaveDto(
                oppgaveKilde = oppgaveKilde,
                oppgaveType = OppgaveType.MANUELL_UTSENDING_BREV,
                merknad = "Kunne ikke sende informasjonsbrev automatisk for dødshendelse.",
                referanse = brevID.toString(),
            )

        return oppgaveKlient.opprettOppgave(sakId, nyOppgave, brukerTokenInfo)
    }
}
