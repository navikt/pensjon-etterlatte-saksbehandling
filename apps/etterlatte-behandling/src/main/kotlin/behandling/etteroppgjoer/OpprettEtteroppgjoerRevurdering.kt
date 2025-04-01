package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.util.UUID

class OpprettEtteroppgjoerRevurdering(
    private val behandlingService: BehandlingService,
    private val etteroppgjoerService: EtteroppgjoerService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
) {
    fun opprett(
        sakId: SakId,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehadling(forbehandlingId)

        // revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId) TODO ønskelig?

        val sisteIverksatte =
            behandlingService.hentSisteIverksatte(sakId)
                ?: throw InternfeilException("Fant ikke iverksatt behandling sak=$sakId")

        val persongalleri =
            grunnlagService.hentPersongalleri(sakId)
                ?: throw InternfeilException("Fant ikke iverksatt persongaller")

        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = forbehandling.innvilgetPeriode.fom,
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
                    relatertBehandlingId = forbehandling.id.toString(),
                    frist = null,
                    paaGrunnAvOppgave = null,
                ).oppdater()

        vilkaarsvurderingService.kopierVilkaarsvurdering(
            behandlingId = revurdering.id,
            kopierFraBehandling = sisteIverksatte.id,
            brukerTokenInfo = brukerTokenInfo,
        )

        etteroppgjoerService.oppdaterStatus(sakId, forbehandling.aar, EtteroppgjoerStatus.UNDER_REVURDERING)

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
        }

        return revurdering
    }
}
