package no.nav.etterlatte.inntektsjustering

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlag.Personopplysning
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringKjoering
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.rapidsandrivers.OmregningData
import no.nav.etterlatte.rapidsandrivers.OmregningDataPacket
import no.nav.etterlatte.rapidsandrivers.OmregningHendelseType
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.YearMonth

class AarligInntektsjusteringJobbService(
    private val omregningService: OmregningService,
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val grunnlagService: GrunnlagService,
    private val vedtakKlient: VedtakKlient,
    private val beregningKlient: BeregningKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val rapid: KafkaProdusent<String, String>,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun startAarligInntektsjustering(request: AarligInntektsjusteringRequest) {
        request.saker.forEach { sakId ->
            startEnkeltSak(request.kjoering, request.loependeFom, sakId)
        }
    }

    private suspend fun startEnkeltSak(
        kjoering: String,
        loependeFom: YearMonth,
        sakId: SakId,
    ) {
        try {
            val vedtak =
                vedtakKlient.sakHarLopendeVedtakPaaDato(sakId, loependeFom.atDay(1), HardkodaSystembruker.omregning)

            logger.info("Årlig inntektsjusteringsjobb $kjoering for $sakId")
            val skalIkkeGjennomfoereJobb = skalIkkeGjennomfoereJobb(sakId, vedtak, loependeFom)
            if (skalIkkeGjennomfoereJobb != null) {
                omregningService.oppdaterKjoering(
                    KjoeringRequest(
                        kjoering,
                        KjoeringStatus.FERDIGSTILT,
                        sakId,
                        begrunnelse = skalIkkeGjennomfoereJobb,
                    ),
                )
                return
            }
            val aarsakTilManuell = kanIkkeKjoereAutomatisk(sakId, vedtak)
            if (aarsakTilManuell != null) {
                // TODO bør opprette behandling for dette ikke bare oppgave..
                val oppgave =
                    oppgaveService.opprettOppgave(
                        sakId.sakId.toString(),
                        sakId,
                        kilde = null,
                        type = OppgaveType.REVURDERING,
                        merknad = "", // TODO
                        // frist =  TODO
                    )
                omregningService.oppdaterKjoering(
                    KjoeringRequest(
                        kjoering,
                        KjoeringStatus.TIL_MANUELL,
                        sakId,
                        begrunnelse = aarsakTilManuell.name,
                    ),
                )
                return
            }
            omregningService.oppdaterKjoering(
                KjoeringRequest(
                    kjoering,
                    KjoeringStatus.KLAR_FOR_OMREGNING,
                    sakId,
                ),
            )
            publiserKlarForOmregning(sakId, loependeFom)
        } catch (e: Exception) {
            omregningService.oppdaterKjoering(
                KjoeringRequest(
                    kjoering,
                    KjoeringStatus.FEILA,
                    sakId,
                    begrunnelse = e.message ?: e.toString(),
                ),
            )
        }
    }

    private suspend fun kanIkkeKjoereAutomatisk(
        sakId: SakId,
        vedtak: LoependeYtelseDTO,
    ): AarligInntektsjusteringAarsakManuell? {
        if (vedtak.underSamordning) {
            return AarligInntektsjusteringAarsakManuell.TIL_SAMORDNING
        }

        val sak = sakService.finnSak(sakId) ?: throw InternfeilException("Fant ikke sak med id $sakId")

        val identErUendretPdl =
            hentPdlPersonident(sak).let { sisteIdentifikatorPdl ->
                val sisteIdent =
                    when (sisteIdentifikatorPdl) {
                        is PdlIdentifikator.FolkeregisterIdent -> sisteIdentifikatorPdl.folkeregisterident.value
                        is PdlIdentifikator.Npid -> sisteIdentifikatorPdl.npid.ident
                    }
                sak.ident == sisteIdent
            }
        if (!identErUendretPdl) {
            return AarligInntektsjusteringAarsakManuell.UTDATERT_IDENT
        }

        val opplysningerGjenny = hentOpplysningerGjenny(sak)

        val opplysningerErUendretIPdl =
            hentPdlPersonopplysning(sak).let { opplysningerPdl ->
                with(opplysningerGjenny.opplysning) {
                    fornavn == opplysningerPdl.fornavn.verdi &&
                        mellomnavn == opplysningerPdl.mellomnavn?.verdi &&
                        etternavn == opplysningerPdl.etternavn.verdi &&
                        foedselsdato == opplysningerPdl.foedselsdato?.verdi &&
                        doedsdato == opplysningerPdl.doedsdato?.verdi &&
                        vergemaalEllerFremtidsfullmakt == opplysningerPdl.vergemaalEllerFremtidsfullmakt?.map { it.verdi }
                }
            }
        if (!opplysningerErUendretIPdl) {
            return AarligInntektsjusteringAarsakManuell.UTDATERTE_PERSONOPPLYSNINGER
        }

        if (opplysningerGjenny.opplysning.vergemaalEllerFremtidsfullmakt?.isNotEmpty() == true) {
            return AarligInntektsjusteringAarsakManuell.VERGEMAAL
        }

        return null
    }

    private fun publiserKlarForOmregning(
        sakId: SakId,
        loependeFom: YearMonth,
    ) {
        val correlationId = getCorrelationId()
        rapid
            .publiser(
                "aarlig-inntektsjustering-$sakId",
                JsonMessage
                    .newMessage(
                        OmregningHendelseType.KLAR_FOR_OMREGNING.lagEventnameForType(),
                        mapOf(
                            CORRELATION_ID_KEY to correlationId,
                            TEKNISK_TID_KEY to Tidspunkt.now(),
                            OmregningDataPacket.KEY to
                                OmregningData(
                                    kjoering = AarligInntektsjusteringKjoering.getKjoering(),
                                    sakId = sakId,
                                    revurderingaarsak = Revurderingaarsak.INNTEKTSENDRING, // TODO egen årsak?
                                    fradato = loependeFom.atDay(1),
                                ).toPacket(),
                        ),
                    ).toJson(),
            ).also { (partition, offset) ->
                logger.info(
                    "Publiserte klar for omregningshendelse for $sakId på partition " +
                        "$partition, offset $offset, correlationid: $correlationId",
                )
            }
    }

    // Skal inntektjusteres hvis: 1) er løpende fom dato, 2) ikke har oppgitt inntekt fra 1.1 neste inntektsår
    private suspend fun skalIkkeGjennomfoereJobb(
        sakId: SakId,
        vedtak: LoependeYtelseDTO,
        loependeFom: YearMonth,
    ): String? =
        if (!vedtak.erLoepende) {
            "Sak er ikke løpende"
        } else if (beregningKlient.sakHarInntektForAar(sakId, loependeFom.year, HardkodaSystembruker.omregning)) {
            "Sak har allerede oppgitt inntekt for ${loependeFom.year}"
        } else {
            null
        }

    private fun hentPdlPersonopplysning(sak: Sak) =
        pdlTjenesterKlient.hentPdlModellFlereSaktyper(sak.ident, PersonRolle.INNSENDER, SakType.OMSTILLINGSSTOENAD)

    private suspend fun hentPdlPersonident(sak: Sak) =
        pdlTjenesterKlient.hentPdlIdentifikator(sak.ident)
            ?: throw InternfeilException("Fant ikke ident fra PDL for sak ${sak.id}")

    private suspend fun hentOpplysningerGjenny(sak: Sak): Personopplysning {
        val sisteIverksatteBehandling =
            behandlingService.hentSisteIverksatte(sak.id)
                ?: throw InternfeilException("Fant ikke iverksatt behandling sak=${sak.id}")
        return grunnlagService
            .hentPersonopplysninger(
                sisteIverksatteBehandling.id,
                sak.sakType,
                HardkodaSystembruker.omregning,
            ).innsender
            ?: throw InternfeilException("Fant ikke opplysninger for behandling=${sisteIverksatteBehandling.id}")
    }
}

enum class AarligInntektsjusteringAarsakManuell {
    UTDATERT_IDENT,
    UTDATERTE_PERSONOPPLYSNINGER,
    VERGEMAAL,
    TIL_SAMORDNING,
}
