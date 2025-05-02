package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
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
) {
    fun opprett(
        sakId: SakId,
        forbehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val (revurdering, sisteIverksatte) =
            inTransaction {
                val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
                if (forbehandling.status !in listOf(EtteroppgjoerForbehandlingStatus.FERDIGSTILT)) {
                    throw InternfeilException("Forbehandlingen har ikke riktig status: ${forbehandling.status}")
                }

                // TODO hva blir riktig her? vi ønsker ikke mer enn en oppgave, men kan det være oppgaver åpne på forbehandling?
                // revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)

                val iverksatteVedtak =
                    runBlocking {
                        vedtakKlient
                            .hentIverksatteVedtak(sakId, brukerTokenInfo)
                            .sortedByDescending { it.datoFattet }
                    }

                if (iverksatteVedtak.isEmpty()) {
                    throw InternfeilException("Fant ingen iverksatte vedtak for sak $sakId")
                }

                // TODO vedtak med opphør støttes ikke enda da vi må tenke litt rundt hvordan dette skal håndteres mtp etteroppgjør
                if (iverksatteVedtak.first().vedtakType === VedtakType.OPPHOER) {
                    throw InternfeilException("Siste iverksatte vedtak er et opphør, dette er ikke støttet enda")
                }

                val sisteIverksatteIkkeOpphoer = iverksatteVedtak.first { it.vedtakType != VedtakType.OPPHOER }

                if (sisteIverksatteIkkeOpphoer.opphoerFraOgMed != null) {
                    throw InternfeilException("Siste iverksatte vedtak har opphør fra og med, dette er ikke støttet enda")
                }

                val sisteIverksatte =
                    behandlingService.hentBehandling(sisteIverksatteIkkeOpphoer.behandlingId)
                        ?: throw InternfeilException("Fant ikke iverksatt behandling ${sisteIverksatteIkkeOpphoer.behandlingId}")

                val persongalleri =
                    grunnlagService.hentPersongalleri(sakId)
                        ?: throw InternfeilException("Fant ikke persongalleri for sak $sakId")

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
                        ).oppdater()

                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = revurdering.id,
                    kopierFraBehandling = sisteIverksatte.id,
                    brukerTokenInfo = brukerTokenInfo,
                )

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(sakId, forbehandling.aar, EtteroppgjoerStatus.UNDER_REVURDERING)

                revurdering to sisteIverksatte
            }

        // TODO her må noe gjøres da feil her medfører en "halvveis behandling"
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
