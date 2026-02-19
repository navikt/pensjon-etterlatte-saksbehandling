package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingDao
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerHendelseService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.FantIkkeForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import java.util.UUID

// TODO: burde plasseres en annen plass, but for now pga overlappende avhengigheter i applicationContext...
class EtteroppgjoerTempService(
    private val etteroppgjoerDao: EtteroppgjoerDao,
    private val etteroppgjoerForbehandlingDao: EtteroppgjoerForbehandlingDao,
    private val hendelserService: EtteroppgjoerHendelseService,
) {
    fun hentForbehandling(behandlingId: UUID): EtteroppgjoerForbehandling =
        etteroppgjoerForbehandlingDao.hentForbehandling(behandlingId) ?: throw FantIkkeForbehandling(behandlingId)

    /**
     * Setter status tilbake til MOTTATT_SKATTEOPPGJOER i etteroppgjøret hvis det skal være ny forbehandling, eller
     * VENTER_PAA_SVAR hvis det ikke skal være en ny forbehandling. Etteroppgjørbehandlingen avbrytes også.
     */
    fun tilbakestillEtteroppgjoerVedAvbruttRevurdering(
        forbehandling: EtteroppgjoerForbehandling,
        aarsak: AarsakTilAvbrytelse?,
        utlandstilknytning: Utlandstilknytning?,
    ) {
        val sakId = forbehandling.sak.id
        val etteroppgjoer =
            etteroppgjoerDao.hentEtteroppgjoerForInntektsaar(sakId, forbehandling.aar)
                ?: throw InternfeilException("Fant ikke etteroppgjoer for sakId=$sakId og inntektsaar=${forbehandling.aar}")

        if (etteroppgjoer.status !in listOf(EtteroppgjoerStatus.UNDER_REVURDERING, EtteroppgjoerStatus.OMGJOERING)) {
            throw InternfeilException(
                "Kan ikke tilbakestille etteroppgjoer for sakId=$sakId: " +
                    "forventet etteroppgjoerStatus ${EtteroppgjoerStatus.UNDER_REVURDERING} " +
                    "eller ${EtteroppgjoerStatus.OMGJOERING}, fant ${etteroppgjoer.status}",
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
