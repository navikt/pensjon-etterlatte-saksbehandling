package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagServiceImpl
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
    private val grunnlagService: GrunnlagServiceImpl,
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

        gyldigForAutomatiskRevurdering(request, loepende, forrigeBehandling, systembruker)

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

    private suspend fun gyldigForAutomatiskRevurdering(
        request: AutomatiskRevurderingRequest,
        vedtak: LoependeYtelseDTO,
        forrigeBehandling: Behandling,
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
            }

            Revurderingaarsak.AARLIG_INNTEKTSJUSTERING -> {}

            /*
             * Skal ikke automatisk revurdere by default hvis:
             * Det ikke er en løpende sak
             * Sak er under samordning
             * Sak har aktivt overstyrt beregning
             */
            else -> {
                if (!vedtak.erLoepende) {
                    throw OmregningKreverLoependeVedtak()
                }
                if (vedtak.underSamordning) {
                    throw OmregningAvSakUnderSamordning()
                }

                val overstyrtBeregning = beregningKlient.harOverstyrt(forrigeBehandling.id, brukerTokenInfo)
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
    } ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak $sakId")

    private fun opprettAutomatiskRevurdering(
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
