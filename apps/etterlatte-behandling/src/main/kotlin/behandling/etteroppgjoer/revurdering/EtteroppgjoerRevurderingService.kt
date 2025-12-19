package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.time.YearMonth
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
    private val etteroppgjoerDataService: EtteroppgjoerDataService,
) {
    fun opprettEtteroppgjoerRevurdering(
        sakId: SakId,
        opprinnelse: BehandlingOpprinnelse,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val etteroppgjoer = etteroppgjoerService.hentAktivtEtteroppgjoerForSak(sakId)
        val sisteFerdigstilteForbehandlingId = etteroppgjoer.sisteFerdigstilteForbehandling

        krevIkkeNull(sisteFerdigstilteForbehandlingId) {
            "Fant ikke sisteFerdigstilteForbehandling for sak $sakId"
        }

        val (revurdering, sisteIverksatteBehandling) =
            inTransaction {
                revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
                val etteroppgjoer = etteroppgjoerService.hentAktivtEtteroppgjoerForSak(sakId)

                sjekkKanOppretteEtteroppgjoerRevurdering(etteroppgjoer, sisteFerdigstilteForbehandlingId)

                val (sisteIverksatteBehandling, opphoerFom) =
                    etteroppgjoerDataService.hentSisteIverksatteBehandlingOgOpphoer(
                        sakId,
                        brukerTokenInfo,
                    )
                val revurdering =
                    opprettRevurdering(
                        sakId,
                        sisteIverksatteBehandling,
                        sisteFerdigstilteForbehandlingId,
                        opprinnelse,
                        opphoerFom,
                        brukerTokenInfo,
                    )

                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = revurdering.id,
                    kopierFraBehandling = sisteIverksatteBehandling.id,
                    brukerTokenInfo = brukerTokenInfo,
                )

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                    sakId,
                    etteroppgjoer.inntektsaar,
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
                fraForbehandlingId = sisteFerdigstilteForbehandlingId,
                tilForbehandlingId = UUID.fromString(revurdering.relatertBehandlingId),
                brukerTokenInfo = brukerTokenInfo,
            )
            krevIkkeNull(revurderingService.hentBehandling(revurdering.id)) { "Revurdering finnes ikke etter oppretting" }
        }
    }

    fun hentBeregnetResultatForRevurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val forbehandlingId =
            behandling?.relatertBehandlingId?.parseUuid() ?: throw UgyldigForespoerselException(
                "MANGLER_FORBEHANDLING_ID",
                "Behandling med id=$behandlingId peker ikke på en gyldig forbehandling",
            )
        val forbehandling = etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
        return etteroppgjoerForbehandlingService.hentBeregnetEtteroppgjoerResultat(forbehandling, brukerTokenInfo)
            ?: throw IkkeFunnetException(
                "MANGLER_BEREGNET_RESULTAT",
                "Forbehandling med id=$forbehandlingId til revurdering med id=$behandlingId har " +
                    "ikke et beregnet resultat for etteroppgjøret.",
            )
    }

    private fun opprettRevurdering(
        sakId: SakId,
        sisteIverksatteBehandling: Behandling,
        sisteFerdigstilteForbehandlingId: UUID,
        opprinnelse: BehandlingOpprinnelse,
        opphoerFom: YearMonth?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val forbehandling =
            etteroppgjoerForbehandlingService.kopierOgLagreNyForbehandling(
                sisteFerdigstilteForbehandlingId,
                sakId,
                brukerTokenInfo,
            )

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
                opphoerFraOgMed = opphoerFom,
            ).oppdater()
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

    private fun sjekkKanOppretteEtteroppgjoerRevurdering(
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

    private fun String.parseUuid(): UUID? =
        try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}

data class SisteAvkortingOgOpphoer(
    val sisteBehandlingMedAvkorting: UUID,
    val opphoerFom: YearMonth?,
)
