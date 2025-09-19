package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService

class EtteroppgjoerRevurderingService(
    private val behandlingService: BehandlingService,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
    private val vedtakKlient: VedtakKlient,
) {
    fun opprettEtteroppgjoerRevurdering(
        sakId: SakId,
        opprinnelse: BehandlingOpprinnelse,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)

        val sisteFerdigstilteForbehandling =
            etteroppgjoerForbehandlingService.hentSisteFerdigstillteForbehandlingPaaSak(
                sakId = sakId,
            )

        // TODO: er dette nok for å unngå mismatch ... ?
        etteroppgjoerService
            .hentAlleAktiveEtteroppgjoerForSak(sakId)
            .firstOrNull { it.sisteFerdigstilteForbehandling == sisteFerdigstilteForbehandling.id }
            ?: throw InternfeilException(
                "Fant ingen aktive etteroppgjoer for sak $sakId og forbehandling ${sisteFerdigstilteForbehandling.id}",
            )

        val (revurdering, sisteIverksatteBehandling) =
            inTransaction {
                val sisteIverksatteIkkeOpphoer = hentSisteIverksatteVedtakIkkeOpphoer(sakId, brukerTokenInfo)

                val sisteIverksatteBehandling =
                    behandlingService.hentBehandling(sisteIverksatteIkkeOpphoer.behandlingId)
                        ?: throw InternfeilException("Fant ikke iverksatt behandling ${sisteIverksatteIkkeOpphoer.behandlingId}")

                val revurdering =
                    opprettRevurdering(sakId, sisteIverksatteBehandling, opprinnelse, sisteFerdigstilteForbehandling, brukerTokenInfo)

                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = revurdering.id,
                    kopierFraBehandling = sisteIverksatteBehandling.id,
                    brukerTokenInfo = brukerTokenInfo,
                )

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                    sakId,
                    sisteFerdigstilteForbehandling.aar,
                    EtteroppgjoerStatus.UNDER_REVURDERING,
                )

                revurdering to sisteIverksatteBehandling
            }

        // TODO her må noe gjøres da feil her medfører en "halvveis behandling"
        runBlocking {
            trygdetidKlient.kopierTrygdetidFraForrigeBehandling(
                behandlingId = revurdering.id,
                forrigeBehandlingId = sisteIverksatteBehandling.id,
                brukerTokenInfo = brukerTokenInfo,
            )
            beregningKlient.opprettBeregningsgrunnlagFraForrigeBehandling(
                behandlingId = revurdering.id,
                forrigeBehandlingId = sisteIverksatteBehandling.id,
                brukerTokenInfo = brukerTokenInfo,
            )
            beregningKlient.beregnBehandling(
                behandlingId = revurdering.id,
                brukerTokenInfo = brukerTokenInfo,
            )
        }

        return revurdering
    }

    private fun opprettRevurdering(
        sakId: SakId,
        sisteIverksatteBehandling: Behandling,
        opprinnelse: BehandlingOpprinnelse,
        sisteFerdigstilteForbehandling: EtteroppgjoerForbehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val persongalleri =
            grunnlagService.hentPersongalleri(sakId)
                ?: throw InternfeilException("Fant ikke persongalleri for sak $sakId")

        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = sisteFerdigstilteForbehandling.innvilgetPeriode.fom,
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                begrunnelse = "Satt automatisk ved opprettelse av revurdering med årsak etteroppgjør.",
            )

        return revurderingService
            .opprettRevurdering(
                sakId = sakId,
                forrigeBehandling = sisteIverksatteBehandling,
                relatertBehandlingId = sisteFerdigstilteForbehandling.id.toString(),
                persongalleri = persongalleri,
                prosessType = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                virkningstidspunkt = virkningstidspunkt,
                saksbehandlerIdent = brukerTokenInfo.ident(),
                begrunnelse = "Etteroppgjør ${sisteFerdigstilteForbehandling.aar}",
                mottattDato = null,
                frist = null,
                paaGrunnAvOppgave = null,
                opprinnelse = opprinnelse,
            ).oppdater()
    }

    private fun hentSisteIverksatteVedtakIkkeOpphoer(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakSammendragDto =
        runBlocking {
            val iverksatteVedtak =
                vedtakKlient
                    .hentIverksatteVedtak(sakId, brukerTokenInfo)
                    .sortedByDescending { it.datoFattet }

            val sisteIverksatteVedtak =
                iverksatteVedtak.firstOrNull()
                    ?: throw InternfeilException("Fant ingen iverksatte vedtak for sak $sakId")

            if (sisteIverksatteVedtak.vedtakType == VedtakType.OPPHOER) {
                throw InternfeilException("Siste iverksatte vedtak er et opphør, dette er ikke støttet enda")
            }
            if (sisteIverksatteVedtak.opphoerFraOgMed != null) {
                throw InternfeilException("Siste iverksatte vedtak har opphør fra og med, dette er ikke støttet enda")
            }

            sisteIverksatteVedtak
        }
}
