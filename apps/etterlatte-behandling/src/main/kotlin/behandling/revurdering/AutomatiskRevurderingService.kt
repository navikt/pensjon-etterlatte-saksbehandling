package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagServiceImpl
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import java.time.LocalDate
import java.time.LocalTime

class AutomatiskRevurderingService(
    private val revurderingService: RevurderingService,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagServiceImpl,
) {
    /*
     * Denne tjenesten er tiltenkt automatiske jobber der det kan utføres mange samtidig.
     * Det er derfor behov for retries rundt oppfølgingsmetoder.
     */
    suspend fun oppprettRevurderingOgOppfoelging(request: AutomatiskRevurderingRequest): AutomatiskRevurderingResponse {
        validerSakensTilstand(request.sakId, request.revurderingAarsak)

        val forrigeBehandling =
            inTransaction {
                behandlingService.hentSisteIverksatte(request.sakId)
                    ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak ${request.sakId}")
            }
        val persongalleri = grunnlagService.hentPersongalleri(forrigeBehandling.id)

        val revurderingOgOppfoelging =
            inTransaction {
                opprettAutomatiskRevurdering(
                    sakId = request.sakId,
                    forrigeBehandling = forrigeBehandling,
                    revurderingAarsak = request.revurderingAarsak,
                    virkningstidspunkt = request.fraDato,
                    kilde = Vedtaksloesning.GJENNY,
                    persongalleri = persongalleri,
                    frist = request.oppgavefrist?.let { Tidspunkt.ofNorskTidssone(it, LocalTime.NOON) },
                )
            }

        retryOgPakkUt { revurderingOgOppfoelging.leggInnGrunnlag() }
        retryOgPakkUt {
            inTransaction {
                revurderingOgOppfoelging.opprettOgTildelOppgave()
            }
        }
        retryOgPakkUt { revurderingOgOppfoelging.sendMeldingForHendelse() }

        return AutomatiskRevurderingResponse(
            behandlingId = revurderingOgOppfoelging.behandlingId(),
            forrigeBehandlingId = forrigeBehandling.id,
            sakType = revurderingOgOppfoelging.sakType(),
        )
    }

    fun opprettAutomatiskRevurdering(
        sakId: SakId,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: LocalDate? = null,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        mottattDato: String? = null,
        begrunnelse: String? = null,
        frist: Tidspunkt? = null,
    ) = forrigeBehandling.let {
        revurderingService.opprettRevurdering(
            sakId = sakId,
            persongalleri = persongalleri,
            forrigeBehandling = forrigeBehandling.id,
            mottattDato = mottattDato,
            prosessType = Prosesstype.AUTOMATISK,
            kilde = kilde,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt?.tilVirkningstidspunkt("Opprettet automatisk"),
            utlandstilknytning = forrigeBehandling.utlandstilknytning,
            boddEllerArbeidetUtlandet = forrigeBehandling.boddEllerArbeidetUtlandet,
            begrunnelse = begrunnelse ?: "Automatisk revurdering - ${revurderingAarsak.name.lowercase()}",
            saksbehandlerIdent = Fagsaksystem.EY.navn,
            frist = frist,
            opphoerFraOgMed = forrigeBehandling.opphoerFraOgMed,
        )
    }

    fun validerSakensTilstand(
        sakId: SakId,
        revurderingAarsak: Revurderingaarsak,
    ) {
        if (revurderingAarsak == Revurderingaarsak.ALDERSOVERGANG) {
            revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
        }
    }
}
