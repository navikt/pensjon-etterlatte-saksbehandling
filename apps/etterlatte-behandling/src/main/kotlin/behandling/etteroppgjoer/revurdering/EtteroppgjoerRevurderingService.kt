package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.util.UUID

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
    private val featureToggleService: FeatureToggleService,
) {
    fun opprettEtteroppgjoerRevurdering(
        sakId: SakId,
        opprinnelse: BehandlingOpprinnelse,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val sisteFerdigstilteForbehandling =
            inTransaction {
                etteroppgjoerForbehandlingService
                    .hentSisteFerdigstillteForbehandling(sakId)
            }

        val (revurdering, sisteIverksatteBehandling) =
            inTransaction {
                revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
                val etteroppgjoer = etteroppgjoerService.hentAktivtEtteroppgjoerForSak(sakId)

                kanOppretteEtteroppgjoerRevurdering(etteroppgjoer, sisteFerdigstilteForbehandling.id)

                val sisteIverksatteBehandling = hentSisteIverksatteBehandling(sakId, brukerTokenInfo)
                val revurdering =
                    opprettRevurdering(sakId, sisteIverksatteBehandling, sisteFerdigstilteForbehandling.id, opprinnelse, brukerTokenInfo)

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

        return inTransaction {
            kopierFaktiskInntekt(
                fraForbehandlingId = sisteFerdigstilteForbehandling.id,
                tilForbehandlingId = UUID.fromString(revurdering.relatertBehandlingId),
                brukerTokenInfo = brukerTokenInfo,
            )
            krevIkkeNull(revurderingService.hentBehandling(revurdering.id)) { "Revurdering finnes ikke etter oppretting" }
        }
    }

    private fun opprettRevurdering(
        sakId: SakId,
        sisteIverksatteBehandling: Behandling,
        sisteFerdigstilteForbehandlingId: UUID,
        opprinnelse: BehandlingOpprinnelse,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val forbehandling = etteroppgjoerForbehandlingService.kopierOgLagreNyForbehandling(sisteFerdigstilteForbehandlingId, sakId)

        val persongalleri =
            grunnlagService.hentPersongalleri(sakId)
                ?: throw InternfeilException("Fant ikke persongalleri for sak $sakId")

        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = forbehandling.innvilgetPeriode.fom,
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                begrunnelse = "Satt automatisk ved opprettelse av revurdering med årsak etteroppgjør.",
            )

        return revurderingService
            .opprettRevurdering(
                sakId = sakId,
                forrigeBehandling = sisteIverksatteBehandling,
                relatertBehandlingId = forbehandling.id.toString(),
                persongalleri = persongalleri,
                prosessType = Prosesstype.MANUELL,
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                virkningstidspunkt = virkningstidspunkt,
                saksbehandlerIdent = brukerTokenInfo.ident(),
                begrunnelse = "Etteroppgjør ${forbehandling.aar}",
                mottattDato = null,
                frist = null,
                paaGrunnAvOppgave = null,
                opprinnelse = opprinnelse,
            ).oppdater()
    }

    private fun hentSisteIverksatteVedtakIkkeOpphoer(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): UUID =
        runBlocking {
            if (featureToggleService.isEnabled(EtteroppgjoerToggles.ETTEROPPGJOER_OPPHOER_SKYLDES_DOEDSFALL, false)) {
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

                sisteIverksatteVedtak.behandlingId
            } else {
                etteroppgjoerForbehandlingService.hentSisteIverksatteBehandlingMedAvkorting(sakId, brukerTokenInfo).id
            }
        }

    private fun kopierFaktiskInntekt(
        fraForbehandlingId: UUID,
        tilForbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): FaktiskInntektDto? {
        val sisteFerdigstilteForbehandling =
            etteroppgjoerForbehandlingService
                .hentDetaljertForbehandling(fraForbehandlingId, brukerTokenInfo)

        return sisteFerdigstilteForbehandling.faktiskInntekt?.apply {
            etteroppgjoerForbehandlingService.lagreOgBeregnFaktiskInntekt(
                forbehandlingId = tilForbehandlingId,
                request =
                    BeregnFaktiskInntektRequest(
                        loennsinntekt,
                        afp,
                        naeringsinntekt,
                        utlandsinntekt,
                        spesifikasjon,
                    ),
                brukerTokenInfo = brukerTokenInfo,
            )
        }
    }

    private fun kanOppretteEtteroppgjoerRevurdering(
        etteroppgjoer: Etteroppgjoer,
        forventetForbehandlingId: UUID,
    ) {
        if (!etteroppgjoer.kanOppretteRevurdering()) {
            throw InternfeilException(
                "Kan ikke opprette etteroppgjoer revurdering for sak ${etteroppgjoer.sakId} " +
                    "på grunn av feil status ${etteroppgjoer.status}",
            )
        }

        if (etteroppgjoer.sisteFerdigstilteForbehandling != forventetForbehandlingId) {
            throw InternfeilException(
                "Fant ingen aktive etteroppgjoer for sak ${etteroppgjoer.sakId} og forbehandling $forventetForbehandlingId",
            )
        }
    }

    private fun hentSisteIverksatteBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling {
        val sisteIverksatteBehandlingId = hentSisteIverksatteVedtakIkkeOpphoer(sakId, brukerTokenInfo)

        return behandlingService.hentBehandling(sisteIverksatteBehandlingId)
            ?: throw InternfeilException("Fant ikke iverksatt behandling $sisteIverksatteBehandlingId")
    }
}
