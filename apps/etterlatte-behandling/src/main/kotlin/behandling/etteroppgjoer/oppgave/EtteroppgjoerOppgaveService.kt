package no.nav.etterlatte.behandling.etteroppgjoer.oppgave

import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.logger
import no.nav.etterlatte.oppgave.OppgaveService
import java.util.UUID

class EtteroppgjoerOppgaveService(
    private val oppgaveService: OppgaveService,
) {
    fun opprettOppgaveForOpprettForbehandling(
        sakId: SakId,
        merknad: String? = null,
        opprettetManuelt: Boolean? = false,
    ) {
        // Samme oppgave brukes for oppretting og behandling av forbehandling.
        // En tom referanse betyr at oppgaven gjelder oppretting.
        // Når forbehandling opprettes, settes referansen til forbehandlingId.
        val eksisterendeOppgaver =
            oppgaveService
                .hentOppgaverForSakAvType(sakId, listOf(OppgaveType.ETTEROPPGJOER))
                .filter { it.erIkkeAvsluttet() && it.referanse.isEmpty() }

        when {
            eksisterendeOppgaver.size > 2 -> {
                throw InternfeilException(
                    "For mange oppgaver for opprette forbehandling i sak=$sakId: " +
                        "fant ${eksisterendeOppgaver.size}, forventet maks 1. Må undersøke hvorfor vi fortsetter å opprette.",
                )
            }

            eksisterendeOppgaver.isNotEmpty() -> {
                if (opprettetManuelt == true) {
                    // Hvis oppgaven prøves å opprettes manuelt skal vi rapportere tilbake feil i saksbehandlingsløsningen
                    throw InternfeilException(
                        "Det eksisterer allerede en oppgave for opprette forbehandling i " +
                            "sak=$sakId, hopper over opprettelse",
                    )
                } else {
                    logger.info(
                        "Det eksisterer allerede en oppgave for opprette forbehandling i sak=$sakId, " +
                            "hopper over opprettelse",
                    )
                }
                return
            }

            else -> {
                oppgaveService.opprettOppgave(
                    referanse = "",
                    sakId = sakId,
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.ETTEROPPGJOER,
                    merknad = merknad ?: "Etteroppgjøret for $ETTEROPPGJOER_AAR er klart til behandling",
                )
            }
        }
    }

    fun verifiserOppgaveForOppretteForbehandling(
        oppgave: OppgaveIntern,
        sakId: SakId,
    ) {
        if (oppgave.sakId != sakId) {
            throw UgyldigForespoerselException(
                "OPPGAVE_IKKE_I_SAK",
                "OppgaveId=${oppgave.id} matcher ikke sakId=$sakId",
            )
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException(
                "OPPGAVE_AVSLUTTET",
                "Oppgaven tilknyttet forbehandling er avsluttet og kan ikke behandles",
            )
        }

        if (oppgave.type != OppgaveType.ETTEROPPGJOER) {
            throw UgyldigForespoerselException(
                "OPPGAVE_FEIL_TYPE",
                "Oppgaven har feil oppgaveType=${oppgave.type} til å opprette forbehandling",
            )
        }
    }

    fun sjekkAtOppgavenErTildeltSaksbehandler(
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val oppgave =
            oppgaveService
                .hentOppgaverForReferanse(forbehandlingId.toString())
                .firstOrNull { it.erIkkeAvsluttet() }
                ?: throw InternfeilException("Fant ingen oppgaver under behandling for forbehandlingId=$forbehandlingId")

        if (oppgave.saksbehandler?.ident != brukerTokenInfo.ident()) {
            throw IkkeTillattException(
                "IKKE_TILGANG_TIL_BEHANDLING",
                "Saksbehandler ${brukerTokenInfo.ident()} er ikke tildelt oppgaveId=${oppgave.id}",
            )
        }

        if (oppgave.erAvsluttet()) {
            throw UgyldigForespoerselException(
                "OPPGAVE_AVSLUTTET",
                "Oppgaven tilknyttet forbehandlingId=$forbehandlingId er avsluttet og kan ikke behandles",
            )
        }
    }
}
