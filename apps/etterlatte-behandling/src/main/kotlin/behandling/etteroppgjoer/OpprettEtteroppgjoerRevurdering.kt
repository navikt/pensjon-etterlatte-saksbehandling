package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.time.YearMonth

class OpprettEtteroppgjoerRevurdering(
    private val behandlingService: BehandlingService,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
) {
    fun opprett(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val inntektsaar = 2024 // TODO utledes fra hvor? forbehandling?

        val (revurdering, sisteIverksatte) =
            inTransaction {
                revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)

                val sisteIverksatte =
                    behandlingService.hentSisteIverksatte(sakId)
                        ?: throw InternfeilException("Fant ikke iverksatt behandling sak=$sakId")

                val persongalleri =
                    grunnlagService.hentPersongalleri(sakId)
                        ?: throw InternfeilException("Fant ikke iverksatt persongaller")

                val virkningstidspunkt =
                    Virkningstidspunkt(
                        dato = YearMonth.of(inntektsaar, 1), // TODO må utledes
                        kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                        begrunnelse = "Satt automatisk ved opprettelse av revurdering med årsak etteroppgjør.",
                    )

                val revurdering =
                    revurderingService
                        .opprettRevurdering(
                            sakId = sakId,
                            forrigeBehandling = sisteIverksatte,
                            persongalleri = persongalleri,
                            prosessType = Prosesstype.MANUELL, // TODO parameter når automatisk implementeres
                            kilde = Vedtaksloesning.GJENNY,
                            revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                            virkningstidspunkt = virkningstidspunkt,
                            begrunnelse = "TODO", // TODO
                            saksbehandlerIdent = brukerTokenInfo.ident(),
                            mottattDato = null,
                            relatertBehandlingId = null,
                            frist = null,
                            paaGrunnAvOppgave = null,
                        ).oppdater()

                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = revurdering.id,
                    kopierFraBehandling = sisteIverksatte.id,
                    brukerTokenInfo = brukerTokenInfo,
                )

                etteroppgjoerService.oppdaterStatus(sakId, inntektsaar, EtteroppgjoerStatus.UNDER_REVURDERING)

                Pair(revurdering, sisteIverksatte)
            }
        runBlocking {
            trygdetidKlient.kopierTrygdetidFraForrigeBehandling(
                behandlingId = revurdering.id,
                forrigeBehandlingId = sisteIverksatte.id,
                brukerTokenInfo = brukerTokenInfo,
            )

            beregningKlient.opprettBeregningsgrunnlagFraForrigeBehandling(
                behandlingId = revurdering.id,
                forrigeBehandlingId = sisteIverksatte.id,
                brukerTokenInfo = brukerTokenInfo,
            )
            beregningKlient.beregnBehandling(
                behandlingId = revurdering.id,
                brukerTokenInfo = brukerTokenInfo,
            )

            // TODO Avkorting basert på faktisk inntekt fra forbehandling
        }
        return revurdering
    }
}
