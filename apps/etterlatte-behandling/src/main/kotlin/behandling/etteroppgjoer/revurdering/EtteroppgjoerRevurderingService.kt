package no.nav.etterlatte.behandling.etteroppgjoer.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpphoerFraTidligereBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.Etteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerDataService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerService
import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerStatus
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.BeregnFaktiskInntektRequest
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelser
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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
        inntektsaar: Int,
        opprinnelse: BehandlingOpprinnelse,
        klageId: UUID? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val etteroppgjoer =
            inTransaction { etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) }
        val sisteFerdigstilteForbehandlingId = etteroppgjoer.sisteFerdigstilteForbehandling

        krevIkkeNull(sisteFerdigstilteForbehandlingId) {
            "Fant ikke sisteFerdigstilteForbehandling for sakId=$sakId"
        }

        val (revurdering, sisteIverksatteBehandling) =
            inTransaction {
                revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(sakId)
                val etteroppgjoer = etteroppgjoerService.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)

                sjekkKanOppretteEtteroppgjoerRevurdering(etteroppgjoer, sisteFerdigstilteForbehandlingId)

                val vedtakListe = etteroppgjoerDataService.hentIverksatteVedtak(sakId, brukerTokenInfo)
                val sisteIverksatteBehandlingMedAvkorting =
                    hentBehandling(etteroppgjoerDataService.sisteVedtakMedAvkorting(vedtakListe).behandlingId)
                val vedtakMedGjeldendeOpphoer = etteroppgjoerDataService.vedtakMedGjeldendeOpphoer(vedtakListe)

                val revurdering =
                    opprettRevurdering(
                        sakId = sakId,
                        sisteIverksatteBehandling = sisteIverksatteBehandlingMedAvkorting,
                        sisteFerdigstilteForbehandlingId = sisteFerdigstilteForbehandlingId,
                        opprinnelse = opprinnelse,
                        opphoerFraTidligereBehandling =
                            if (vedtakMedGjeldendeOpphoer != null) {
                                OpphoerFraTidligereBehandling(
                                    vedtakMedGjeldendeOpphoer.opphoersdato()!!,
                                    vedtakMedGjeldendeOpphoer.behandlingId,
                                )
                            } else {
                                null
                            },
                        klageId = klageId,
                        brukerTokenInfo = brukerTokenInfo,
                    )

                vilkaarsvurderingService.kopierVilkaarsvurdering(
                    behandlingId = revurdering.id,
                    kopierFraBehandling = sisteIverksatteBehandlingMedAvkorting.id,
                    brukerTokenInfo = brukerTokenInfo,
                )

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                    sakId = sakId,
                    inntektsaar = etteroppgjoer.inntektsaar,
                    status = EtteroppgjoerStatus.UNDER_REVURDERING,
                )

                revurdering to sisteIverksatteBehandlingMedAvkorting
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
                tilForbehandlingId = revurdering.relatertBehandlingId!!,
                brukerTokenInfo = brukerTokenInfo,
            )
            krevIkkeNull(revurderingService.hentBehandling(revurdering.id)) { "Klarte ikke å finne revurdering etter opprettelse" }
        }
    }

    fun gjennopprettAvbruttEtteroppgjoerRevurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val (behandling, forbehandling) =
            inTransaction {
                val behandling = hentBehandling(behandlingId)

                if (behandling.status != BehandlingStatus.AVBRUTT) {
                    throw IkkeTillattException(
                        "BEHANDLING_IKKE_AVBRUTT",
                        "Revurdering med id=${behandling.id} er ikke avbrutt og kan ikke gjennopprettes",
                    )
                }

                val forbehandling = hentForbehandlingForRevurdering(behandling)
                if (forbehandling.status != EtteroppgjoerForbehandlingStatus.AVBRUTT) {
                    throw IkkeTillattException(
                        "FORBEHANDLING_IKKE_AVBRUTT",
                        "Etteroppgjør forbehandling med id=${forbehandling.id} er ikke avbrutt og kan ikke gjennopprettes",
                    )
                }

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                    forbehandling.sak.id,
                    forbehandling.aar,
                    EtteroppgjoerStatus.OMGJOERING,
                    EtteroppgjoerHendelser.OMGJOERING,
                )

                behandling to forbehandling
            }

        return opprettEtteroppgjoerRevurdering(
            sakId = behandling.sak.id,
            inntektsaar = forbehandling.aar,
            opprinnelse = behandling.opprinnelse,
            klageId = forbehandling.klageOmgjoering,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    fun omgjoerEtteroppgjoerRevurderingEtterKlage(
        behandlingId: UUID,
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val (behandling, forbehandling) =
            inTransaction {
                val behandling = hentBehandling(behandlingId)

                if (behandling.status != BehandlingStatus.IVERKSATT) {
                    throw IkkeTillattException(
                        "BEHANDLING_IKKE_FERDIGSTILT",
                        "Revurdering med id=${behandling.id} er ikke iverksatt og kan ikke omgjoeres med klageId=$klageId",
                    )
                }

                val forbehandling = hentForbehandlingForRevurdering(behandling)

                etteroppgjoerService.oppdaterEtteroppgjoerStatus(
                    forbehandling.sak.id,
                    forbehandling.aar,
                    EtteroppgjoerStatus.OMGJOERING,
                    EtteroppgjoerHendelser.OMGJOERING,
                )

                behandling to forbehandling
            }

        return opprettEtteroppgjoerRevurdering(
            sakId = behandling.sak.id,
            inntektsaar = forbehandling.aar,
            opprinnelse = behandling.opprinnelse,
            brukerTokenInfo = brukerTokenInfo,
            klageId = klageId,
        )
    }

    private fun hentBehandling(behandlingId: UUID): Behandling =
        (
            behandlingService.hentBehandling(behandlingId)
                ?: throw IkkeFunnetException("INGEN_BEHANDLING", "Behandling med id=$behandlingId finnes ikke")
        )

    private fun hentForbehandlingForRevurdering(behandling: Behandling): EtteroppgjoerForbehandling {
        val forbehandlingId =
            behandling.relatertBehandlingId ?: throw UgyldigForespoerselException(
                "MANGLER_FORBEHANDLING_ID",
                "Behandling med id=${behandling.id} peker ikke på en gyldig etteroppgjør forbehandling",
            )
        return etteroppgjoerForbehandlingService.hentForbehandling(forbehandlingId)
    }

    fun hentBeregnetResultatForRevurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw IkkeFunnetException("INGEN_BEHANDLING", "Behandling med id=$behandlingId finnes ikke")
        val forbehandling = hentForbehandlingForRevurdering(behandling)
        return etteroppgjoerForbehandlingService.hentBeregnetEtteroppgjoerResultat(forbehandling, brukerTokenInfo)
            ?: throw IkkeFunnetException(
                "MANGLER_BEREGNET_RESULTAT",
                "Forbehandling med id=${forbehandling.id} til revurdering med id=$behandlingId har " +
                    "ikke et beregnet resultat for etteroppgjøret.",
            )
    }

    private fun opprettRevurdering(
        sakId: SakId,
        sisteIverksatteBehandling: Behandling,
        sisteFerdigstilteForbehandlingId: UUID,
        opprinnelse: BehandlingOpprinnelse,
        opphoerFraTidligereBehandling: OpphoerFraTidligereBehandling?,
        klageId: UUID? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): Revurdering {
        val forbehandling =
            etteroppgjoerForbehandlingService.kopierOgLagreNyForbehandling(
                sisteFerdigstilteForbehandlingId,
                sakId,
                klageId,
                brukerTokenInfo,
            )

        val persongalleri =
            grunnlagService.hentPersongalleri(sisteIverksatteBehandling.id)
                ?: throw InternfeilException("Fant ikke persongalleri for sak $sakId")

        if (forbehandling.sak.ident != persongalleri.soeker) {
            throw InternfeilException(
                "Siste grunnlag i sak $sakId er ikke oppdatert med nåværende ident i sak. Saken må revurderes med ny ident før etteroppgjøret kan gjennomføres.",
            )
        }

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
                relatertBehandlingId = forbehandling.id,
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
                opphoerFraTidligereBehandling = opphoerFraTidligereBehandling,
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

private fun VedtakSammendragDto.opphoersdato(): YearMonth? {
    val opphoerFom =
        if (vedtakType == VedtakType.OPPHOER) {
            virkningstidspunkt
        } else {
            opphoerFraOgMed
        }
    return opphoerFom
}
