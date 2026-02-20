package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.OpphoerFraTidligereBehandling
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.time.LocalDate
import java.time.LocalTime

class AutomatiskRevurderingService(
    private val revurderingService: RevurderingService,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val vedtakKlient: VedtakKlient,
    private val beregningKlient: BeregningKlient,
) {
    /*
     * Denne tjenesten er tiltenkt automatiske jobber der det kan utføres mange samtidig.
     * Det er derfor behov for retries rundt oppfølgingsmetoder.
     */
    suspend fun oppprettRevurderingOgOppfoelging(
        request: AutomatiskRevurderingRequest,
        systembruker: Systembruker,
    ): AutomatiskRevurderingResponse {
        if (request.revurderingAarsak == Revurderingaarsak.ALDERSOVERGANG) {
            inTransaction { revurderingService.maksEnOppgaveUnderbehandlingForKildeBehandling(request.sakId) }
        }

        val loepende =
            vedtakKlient.sakHarLopendeVedtakPaaDato(
                request.sakId,
                request.fraDato,
                systembruker,
            )
        val forrigeBehandling = hentForrigeBehandling(loepende, request.sakId)

        val forrigeIverksatteBehandling =
            inTransaction { behandlingService.hentSisteIverksatteBehandling(request.sakId) }
                ?: throw RevurderingManglerIverksattBehandling(request.sakId)

        gyldigForAutomatiskRevurdering(request, loepende, systembruker)

        val persongalleri =
            krevIkkeNull(grunnlagService.hentPersongalleri(request.sakId)) {
                "Persongalleri mangler for sak=${request.sakId}"
            }

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
                    mottattDato = request.mottattDato?.toString(),
                    opphoerFraTidligereBehandling =
                        if (forrigeIverksatteBehandling.opphoerFraOgMed != null) {
                            OpphoerFraTidligereBehandling(
                                forrigeIverksatteBehandling.opphoerFraOgMed!!,
                                forrigeIverksatteBehandling.id,
                            )
                        } else {
                            null
                        },
                )
            }

        retryOgPakkUt {
            inTransaction {
                revurderingOgOppfoelging.leggInnGrunnlag()
            }
        }
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

    private suspend fun gyldigForAutomatiskRevurdering(
        request: AutomatiskRevurderingRequest,
        vedtak: LoependeYtelseDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        when (request.revurderingAarsak) {
            // Har egen sjekk tidligere for å slippe kall mot vedtak
            Revurderingaarsak.ALDERSOVERGANG -> {}

            /*
             * Skal ikke kjøre regulering hvis:
             * Det ikke er en løpende sak
             * Sak er under samordning
             * Sak har aktivt overstyrt beregning OG en åpen behandling samtidig
             */
            Revurderingaarsak.REGULERING -> {
                // TODO utfører per nå sjekkene i egne Rivers før dette gjør derfor ingenting her
                if (vedtak.underSamordning) {
                    throw OmregningAvSakUnderSamordning()
                }
            }

            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> {}

            /*
             * Skal ikke automatisk revurdere by default hvis:
             * Det ikke er en løpende sak
             * Sak er under samordning
             * Sak har aktivt overstyrt beregning
             */
            else -> {
                val sisteLoependeBehandlingId =
                    krevIkkeNull(vedtak.sisteLoependeBehandlingId) {
                        "Saken må ha en behandling som var sist løpende på ytelsesperioden vi omregner fra, " +
                            "sakId=${request.sakId}, fom=${request.fraDato}"
                    }

                if (!vedtak.erLoepende) {
                    throw OmregningKreverLoependeVedtak()
                }
                if (vedtak.underSamordning) {
                    throw OmregningAvSakUnderSamordning()
                }

                val overstyrtBeregning = beregningKlient.harOverstyrt(sisteLoependeBehandlingId, brukerTokenInfo)
                if (overstyrtBeregning) {
                    throw OmregningOverstyrtBeregning()
                }
            }
        }
    }

    private fun hentForrigeBehandling(
        vedtak: LoependeYtelseDTO,
        sakId: SakId,
    ) = vedtak.sisteLoependeBehandlingId?.let {
        inTransaction {
            behandlingService.hentBehandling(it)
        }
    } ?: throw UgyldigForespoerselException(
        code = "MANGLER_LOEPENDE_BEHANDLING",
        detail = "Fant ikke løpende behandling på omregningstidspunkt i sak $sakId",
    )

    private fun opprettAutomatiskRevurdering(
        sakId: SakId,
        forrigeBehandling: Behandling,
        revurderingAarsak: Revurderingaarsak,
        virkningstidspunkt: LocalDate,
        kilde: Vedtaksloesning,
        persongalleri: Persongalleri,
        frist: Tidspunkt? = null,
        mottattDato: String? = null,
        opphoerFraTidligereBehandling: OpphoerFraTidligereBehandling? = null,
    ) = forrigeBehandling.let {
        revurderingService.opprettRevurdering(
            sakId = sakId,
            persongalleri = persongalleri,
            forrigeBehandling = forrigeBehandling,
            mottattDato = mottattDato,
            prosessType = Prosesstype.AUTOMATISK,
            kilde = kilde,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt.tilVirkningstidspunkt("Opprettet automatisk"),
            begrunnelse = "Automatisk revurdering - ${revurderingAarsak.name.lowercase()}",
            saksbehandlerIdent = Fagsaksystem.EY.navn,
            frist = frist,
            opphoerFraTidligereBehandling = opphoerFraTidligereBehandling,
            opprinnelse = BehandlingOpprinnelse.AUTOMATISK_JOBB,
        )
    }
}

class OmregningKreverLoependeVedtak :
    UgyldigForespoerselException("OMREGNING_KREVER_LØPENDE_VEDTAK", "Omregning krever at sak har løpende vedtak")

class OmregningAvSakUnderSamordning :
    UgyldigForespoerselException(
        "OMREGNING_SAK_UNDER_SAMORDNING",
        "Omregning kan ikke utføres om sak er under samordning",
    )

class OmregningOverstyrtBeregning :
    UgyldigForespoerselException(
        "OMREGNING_OVERSTYRT_BEREGNING",
        "Omregning kan ikke utføres om sak har aktiv overstyrt beregning",
    )
