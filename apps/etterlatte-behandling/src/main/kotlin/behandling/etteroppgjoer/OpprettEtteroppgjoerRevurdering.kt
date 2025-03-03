package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.revurdering.OpprettRevurderingRequest
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth

class OpprettEtteroppgjoerRevurdering(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val revurderingService: RevurderingService,
) {
    fun opprett(
        sakId: SakId,
        request: OpprettRevurderingRequest,
    ) {
        val sisteIverksatte =
            behandlingService.hentSisteIverksatte(sakId)
                ?: throw InternfeilException("Fant ikke iverksatt behandling sak=$sakId")

        val persongalleri =
            grunnlagService.hentPersongalleri(sakId)
                ?: throw InternfeilException("Fant ikke iverksatt persongaller")

        val virkningstidspunkt =
            Virkningstidspunkt(
                dato = YearMonth.of(2024, 1), // TODO må utledes
                kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                begrunnelse = "Satt automatisk ved opprettelse av revurdering med årsak etteroppgjør.",
            )

        val revurdering =
            revurderingService.opprettRevurdering(
                sakId = sakId,
                forrigeBehandling = sisteIverksatte,
                persongalleri = persongalleri,
                prosessType = Prosesstype.MANUELL, // TODO parameter når automatisk implementeres
                kilde = Vedtaksloesning.GJENNY,
                revurderingAarsak = Revurderingaarsak.ETTEROPPGJOER,
                virkningstidspunkt = virkningstidspunkt,
                begrunnelse = "TODO",
                saksbehandlerIdent = null, // TODO
                mottattDato = null,
                relatertBehandlingId = null,
                frist = null,
                paaGrunnAvOppgave = null,
            )

        // TODO vilkår

        // TODO trygdetid

        // TODO Beregningsgrunnlag
    }
}
