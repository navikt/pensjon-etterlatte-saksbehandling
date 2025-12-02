package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerHendelseService
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.logger
import no.nav.etterlatte.oppgave.OppgaveService
import java.util.UUID

// TODO: burde plasseres en annen plass, but for now pga overlappende avhengigheter i applicationContext...
class EtteroppgjoerTempService(
    private val oppgaveService: OppgaveService,
    private val etteroppgjoerDao: EtteroppgjoerDao,
    private val etteroppgjoerForbehandlingDao: EtteroppgjoerForbehandlingDao,
    private val hendelserService: EtteroppgjoerHendelseService,
) {
    fun opprettOppgaveForOpprettForbehandling(
        sakId: SakId,
        merknad: String? = null,
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
                logger.info("Det eksisterer allerede en oppgave for opprette forbehandling i sak=$sakId, hopper over opprettelse")
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

    /**
     * Setter status tilbake til MOTTATT_SKATTEOPPGJOER i etteroppgjøret hvis det skal være ny forbehandling, eller
     * VENTER_PAA_SVAR hvis det ikke skal være en ny forbehandling. Etteroppgjørbehandlingen avbrytes også.
     */
    fun tilbakestillEtteroppgjoerVedAvbruttRevurdering(
        behandling: Behandling,
        aarsak: AarsakTilAvbrytelse?,
        utlandstilknytning: Utlandstilknytning?,
    ) {
        val sakId = behandling.sak.id
        val forbehandling =
            etteroppgjoerForbehandlingDao.hentForbehandling(UUID.fromString(behandling.relatertBehandlingId))
                ?: throw InternfeilException("Fant ikke forbehandling med relatertBehandlingId=${behandling.relatertBehandlingId}")

        val etteroppgjoer =
            etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sakId, forbehandling.aar)
                ?: throw InternfeilException("Fant ikke etteroppgjoer for sakId=$sakId og inntektsaar=${forbehandling.aar}")

        if (etteroppgjoer.status != EtteroppgjoerStatus.UNDER_REVURDERING) {
            throw InternfeilException(
                "Kan ikke tilbakestille etteroppgjoer for sakId=$sakId: " +
                    "forventet etteroppgjoerStatus ${EtteroppgjoerStatus.UNDER_REVURDERING}, fant ${etteroppgjoer.status}",
            )
        }

        if (!forbehandling.erRedigerbar() || !forbehandling.erRevurdering()) {
            throw InternfeilException(
                "Kan ikke tilbakestille etteroppgjoer for sakId=$sakId: " +
                    "forventet at forbehandling kunne avbrytes, men kan ikke",
            )
        }

        val kommentar =
            if (aarsak == AarsakTilAvbrytelse.ETTEROPPGJOER_ENDRING_ER_TIL_UGUNST) {
                "Endringen er til ugunst for bruker"
            } else {
                "Revurderingen ble avbrutt"
            }

        forbehandling
            .tilAvbrutt(AarsakTilAvbryteForbehandling.ANNET, kommentar)
            .let { avbruttForbehandling ->
                etteroppgjoerForbehandlingDao.lagreForbehandling(avbruttForbehandling)

                etteroppgjoerDao.oppdaterEtteroppgjoerStatus(
                    sakId = sakId,
                    inntektsaar = etteroppgjoer.inntektsaar,
                    status =
                        if (aarsak == AarsakTilAvbrytelse.ETTEROPPGJOER_ENDRING_ER_TIL_UGUNST) {
                            EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
                        } else {
                            EtteroppgjoerStatus.VENTER_PAA_SVAR
                        },
                )
                hendelserService.registrerOgSendEtteroppgjoerHendelse(
                    etteroppgjoerForbehandling = avbruttForbehandling,
                    hendelseType = EtteroppgjoerHendelseType.AVBRUTT,
                    saksbehandler = (Kontekst.get().brukerTokenInfo)?.ident(),
                    utlandstilknytning = utlandstilknytning,
                )
            }
    }
}
