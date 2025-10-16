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
        val defaultMerknad = "Etteroppgjøret for $ETTEROPPGJOER_AAR er klart til behandling"

        val eksisterendeOppgaver =
            oppgaveService
                .hentOppgaverForSakAvType(sakId, listOf(OppgaveType.ETTEROPPGJOER))
                .filter { it.erIkkeAvsluttet() && it.referanse.isEmpty() }

        if (eksisterendeOppgaver.isEmpty()) {
            oppgaveService.opprettOppgave(
                referanse = "",
                sakId = sakId,
                kilde = OppgaveKilde.HENDELSE,
                type = OppgaveType.ETTEROPPGJOER,
                merknad = merknad ?: defaultMerknad,
            )
        } else {
            throw InternfeilException(
                "Forsøker å opprette ny oppgave om opprette forbehandling, " +
                    "men det eksisterer allerede ${eksisterendeOppgaver.size} oppgave(r) for sakId=$sakId",
            )
        }
    }

    // TODO: ikke optimalt plassering men gjøres pga overlappende avhengigheter i applicationContext

    /**
     * Setter status tilbake til MOTTATT_SKATTEOPPGJOER i etteroppgjøret og avbryter forbehandlingen.
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

        if (!forbehandling.kanAvbrytes() || forbehandling.kopiertFra == null) {
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

        etteroppgjoerForbehandlingDao.lagreForbehandling(
            forbehandling.tilAvbrutt(AarsakTilAvbryteForbehandling.ANNET, kommentar),
        )
        etteroppgjoerDao.oppdaterEtteroppgjoerStatus(
            sakId,
            etteroppgjoer.inntektsaar,
            EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
        )
        hendelserService.registrerOgSendEtteroppgjoerHendelse(
            etteroppgjoerForbehandling = forbehandling,
            hendelseType = EtteroppgjoerHendelseType.AVBRUTT,
            saksbehandler = (Kontekst.get().brukerTokenInfo)?.ident(),
            utlandstilknytning = utlandstilknytning,
        )
    }
}
