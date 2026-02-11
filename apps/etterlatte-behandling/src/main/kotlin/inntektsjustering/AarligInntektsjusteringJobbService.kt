package no.nav.etterlatte.inntektsjustering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.etteroppgjoer.ETTEROPPGJOER_AAR
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.omregning.OmregningService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.Personopplysning
import no.nav.etterlatte.grunnlag.aldersovergang.AldersovergangService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.AAPEN_BEHANDLING
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.ALDERSOVERGANG_67
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.HAR_OPPHOER_FOM
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.HAR_OVERSTYRT_BEREGNING
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.HAR_SANKSJON
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.TIL_SAMORDNING
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.UTDATERTE_PERSONO_INFO
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.UTDATERT_IDENT
import no.nav.etterlatte.inntektsjustering.AarligInntektsjusteringAarsakManuell.VERGEMAAL
import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.tilVirkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningDataPacket
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

class AarligInntektsjusteringJobbService(
    private val omregningService: OmregningService,
    private val sakService: SakService,
    private val behandlingService: BehandlingService,
    private val revurderingService: RevurderingService,
    private val grunnlagService: GrunnlagService,
    private val aldersovergangService: AldersovergangService,
    private val vedtakKlient: VedtakKlient,
    private val beregningKlient: BeregningKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val oppgaveService: OppgaveService,
    private val rapid: KafkaProdusent<String, String>,
    private val featureToggleService: FeatureToggleService,
    private val etteroppgjoerForbehandlingService: EtteroppgjoerForbehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startAarligInntektsjusteringJobb(request: AarligInntektsjusteringRequest) {
        logger.info("Starter årlig inntektsjusteringjobb ${request.kjoering}")
        request.saker.forEach { sakId ->
            startEnkeltSak(request.kjoering, request.loependeFom, sakId)
        }
    }

    // i det tilfelle hvor aarligInntektsjusteringJobb ikka kan behandle sak atuomatisk,
    fun opprettRevurderingForAarligInntektsjustering(
        sakId: SakId,
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Revurdering {
        val sak = sakService.finnSak(sakId) ?: throw InternfeilException("Fant ikke sak med id $sakId")
        val aapneBehandlinger = behandlingService.hentAapneBehandlingerForSak(sak.id)
        if (aapneBehandlinger.isNotEmpty()) {
            logger.info("Sak har åpne behandlinger, kan ikke opprette revurdering")
            throw UgyldigForespoerselException(
                "KAN_IKKE_OPPRETTE_REVURDERING_PGA_AAPNE_BEHANDLINGER",
                "Kan ikke opprette revurdering på grunn av sak har åpne behandlinger",
            )
        }

        val begrunnelse = oppgaveService.hentOppgave(oppgaveId).merknad
        val loependeFom = AarligInntektsjusteringRequest.utledLoependeFom()
        val revurdering = nyManuellRevurdering(sakId, hentForrigeBehandling(sakId), loependeFom, begrunnelse!!)

        oppgaveService.ferdigstillOppgave(oppgaveId, saksbehandler)

        return revurdering
    }

    private fun startEnkeltSak(
        kjoering: String,
        loependeFom: YearMonth,
        sakId: SakId,
    ) = inTransaction {
        logger.info("Årlig inntektsjusteringsjobb $kjoering for sak $sakId")
        try {
            val vedtak =
                runBlocking {
                    vedtakKlient.sakHarLopendeVedtakPaaDato(sakId, loependeFom.atDay(1), HardkodaSystembruker.omregning)
                }

            if (!vedtak.erLoepende) {
                oppdaterKjoering(kjoering, KjoeringStatus.FERDIGSTILT, sakId, "Sak er ikke løpende")
                return@inTransaction
            }

            val forrigeBehandling = hentForrigeBehandling(sakId)

            val avkortingSjekk = hentAvkortingSjekk(sakId, loependeFom, forrigeBehandling.id)
            if (avkortingSjekk.harInntektForAar) {
                oppdaterKjoering(
                    kjoering,
                    KjoeringStatus.FERDIGSTILT,
                    sakId,
                    "Sak har allerede oppgitt inntekt for ${loependeFom.year}",
                )
                return@inTransaction
            }

            if (maaGjoeresManuelt(kjoering, sakId, loependeFom, vedtak, avkortingSjekk, forrigeBehandling)) {
                return@inTransaction
            }

            oppdaterKjoering(kjoering, KjoeringStatus.KLAR_FOR_OMREGNING, sakId)
            publiserKlarForOmregning(sakId, loependeFom, kjoering)
        } catch (e: Exception) {
            logger.warn("Automatisk jobb feilet! kjøring:$kjoering sak:$sakId", e)
            oppdaterKjoering(
                kjoering,
                KjoeringStatus.FEILA,
                sakId,
                begrunnelse = e.message ?: e.toString(),
            )
        }
    }

    private fun maaGjoeresManuelt(
        kjoering: String,
        sakId: SakId,
        loependeFom: YearMonth,
        vedtak: LoependeYtelseDTO,
        avkortingSjekk: InntektsjusteringAvkortingInfoResponse,
        forrigeBehandling: Behandling,
    ): Boolean {
        val sak = sakService.finnSak(sakId) ?: throw InternfeilException("Fant ikke sak med id $sakId")

        val aapneOppgaver =
            oppgaveService
                .hentOppgaverForSak(sakId)
                .filter { it.kilde == OppgaveKilde.BEHANDLING }
                .any { it.erIkkeAvsluttet() }

        if (aapneOppgaver) {
            nyOppgaveOgOppdaterKjoering(sakId, forrigeBehandling.id, kjoering, AAPEN_BEHANDLING)
            return true
        }

        if (vedtak.underSamordning) {
            nyOppgaveOgOppdaterKjoering(sakId, forrigeBehandling.id, kjoering, TIL_SAMORDNING)
            return true
        }

        if (forrigeBehandling.opphoerFraOgMed?.year == loependeFom.year) {
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, HAR_OPPHOER_FOM)
            return true
        }

        val aldersovergangMaaned =
            runBlocking {
                aldersovergangService.aldersovergangMaaned(
                    sakId,
                    SakType.OMSTILLINGSSTOENAD,
                )!!
            }
        if (aldersovergangMaaned.year == loependeFom.year) {
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, ALDERSOVERGANG_67)
            return true
        }

        if (avkortingSjekk.harSanksjon) {
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, HAR_SANKSJON)
            return true
        }

        val harOverstyrtBeregning =
            runBlocking {
                beregningKlient.harOverstyrt(forrigeBehandling.id, HardkodaSystembruker.omregning)
            }
        if (harOverstyrtBeregning) {
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, HAR_OVERSTYRT_BEREGNING)
            return true
        }

        hentPdlPersonident(sak).let { sisteIdentifikatorPdl ->
            val sisteIdent =
                when (sisteIdentifikatorPdl) {
                    is PdlIdentifikator.FolkeregisterIdent -> sisteIdentifikatorPdl.folkeregisterident.value
                    is PdlIdentifikator.Npid -> sisteIdentifikatorPdl.npid.ident
                }
            if (sak.ident != sisteIdent) {
                nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, UTDATERT_IDENT)
                return true
            }
        }

        val opplysningerGjenny = hentOpplysningerGjenny(sak, forrigeBehandling.id)
        val opplysningerPdl = hentPdlPersonopplysning(sak)

        if (!opplysningerPdl.vergemaalEllerFremtidsfullmakt.isNullOrEmpty()) {
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, VERGEMAAL)
            return true
        }
        val opplysningerErUendretIPdl =
            with(opplysningerGjenny.opplysning) {
                fornavn == opplysningerPdl.fornavn &&
                    mellomnavn == opplysningerPdl.mellomnavn &&
                    etternavn == opplysningerPdl.etternavn &&
                    foedselsdato == opplysningerPdl.foedselsdato &&
                    doedsdato == opplysningerPdl.doedsdato &&
                    erLikeVergemaal(vergemaalEllerFremtidsfullmakt, opplysningerPdl.vergemaalEllerFremtidsfullmakt)
            }

        if (!opplysningerErUendretIPdl) {
            logger.info(
                "PDL info forskjellig for årlig inntektsjustering i sak $sakId. " +
                    "Denne går til manuell håndtering. Se sikkerlogg for detaljer om hva som er forskjellig",
            )
            sikkerlogger().info(
                "PDL info forskjellig for årlig inntektsjustering i sak $sakId. " +
                    "Data i grunnlag: ${opplysningerGjenny.opplysning}, data i PDL: $opplysningerPdl",
            )
            nyBehandlingOgOppdaterKjoering(sakId, loependeFom, forrigeBehandling, kjoering, UTDATERTE_PERSONO_INFO)
            return true
        }

        return false
    }

    private fun hentAvkortingSjekk(
        sakId: SakId,
        loependeFom: YearMonth,
        forrigeBehandlingId: UUID,
    ): InntektsjusteringAvkortingInfoResponse =
        runBlocking {
            beregningKlient.inntektsjusteringAvkortingInfoSjekk(
                sakId,
                loependeFom.year,
                forrigeBehandlingId,
                HardkodaSystembruker.omregning,
            )
        }

    /*
      Inntektsjustering: vi opprette oppgave i det tilfelle behandling ikke kan opprettes automatisk f.eks har åpen behandling,
      samordning etc. Ny behandling opprettes via oppgaven.
     */
    private fun nyOppgaveOgOppdaterKjoering(
        sakId: SakId,
        forrigeBehandlingId: UUID,
        kjoering: String,
        aarsakTilManuell: AarligInntektsjusteringAarsakManuell,
    ) {
        if (!manuellBehandlingSkruddPaa()) {
            oppdaterKjoering(
                kjoering,
                KjoeringStatus.TIL_MANUELL_UTEN_OPPGAVE,
                sakId,
                aarsakTilManuell.name,
            )
            return
        }
        oppgaveService.opprettOppgave(
            referanse = forrigeBehandlingId.toString(),
            sakId = sakId,
            kilde = OppgaveKilde.BEHANDLING,
            type = OppgaveType.AARLIG_INNTEKTSJUSTERING,
            merknad = genererManuellBegrunnelseTekst(aarsakTilManuell),
        )
        oppdaterKjoering(
            kjoering,
            KjoeringStatus.TIL_MANUELL,
            sakId,
            aarsakTilManuell.name,
        )
    }

    private fun nyBehandlingOgOppdaterKjoering(
        sakId: SakId,
        loependeFom: YearMonth,
        forrigeBehandling: Behandling,
        kjoering: String,
        aarsakTilManuell: AarligInntektsjusteringAarsakManuell,
    ) {
        if (!manuellBehandlingSkruddPaa()) {
            oppdaterKjoering(
                kjoering,
                KjoeringStatus.TIL_MANUELL_UTEN_OPPGAVE,
                sakId,
                aarsakTilManuell.name,
            )
            return
        }
        nyManuellRevurdering(sakId, forrigeBehandling, loependeFom, genererManuellBegrunnelseTekst(aarsakTilManuell))
        oppdaterKjoering(
            kjoering,
            KjoeringStatus.TIL_MANUELL,
            sakId,
            aarsakTilManuell.name,
        )
    }

    private fun nyManuellRevurdering(
        sakId: SakId,
        forrigeBehandling: Behandling,
        loependeFom: YearMonth,
        begrunnelse: String,
    ): Revurdering {
        val persongalleri = grunnlagService.hentPersongalleri(sakId)

        val revurdering =
            revurderingService
                .opprettRevurdering(
                    sakId = sakId,
                    persongalleri = krevIkkeNull(persongalleri) { "Persongalleri mangler for sak=$sakId" },
                    forrigeBehandling = forrigeBehandling,
                    mottattDato = null,
                    prosessType = Prosesstype.MANUELL,
                    kilde = Vedtaksloesning.GJENNY,
                    revurderingAarsak = Revurderingaarsak.AARLIG_INNTEKTSJUSTERING,
                    virkningstidspunkt = loependeFom.atDay(1).tilVirkningstidspunkt(begrunnelse),
                    begrunnelse = begrunnelse,
                    saksbehandlerIdent = Fagsaksystem.EY.navn,
                    frist = Tidspunkt.ofNorskTidssone(loependeFom.minusMonths(1).atDay(1), LocalTime.NOON),
                    opprinnelse = BehandlingOpprinnelse.SAKSBEHANDLER,
                ).oppdater()
                .also {
                    revurderingService.fjernSaksbehandlerFraRevurderingsOppgave(it)
                }

        return revurdering
    }

    private fun publiserKlarForOmregning(
        sakId: SakId,
        loependeFom: YearMonth,
        kjoering: String,
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
                                    kjoering = kjoering,
                                    sakId = sakId,
                                    revurderingaarsak = Revurderingaarsak.AARLIG_INNTEKTSJUSTERING,
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

    private fun oppdaterKjoering(
        kjoering: String,
        status: KjoeringStatus,
        sakId: SakId,
        begrunnelse: String? = null,
    ) {
        omregningService.oppdaterKjoering(
            KjoeringRequest(
                kjoering,
                status,
                sakId,
                begrunnelse,
            ),
        )
    }

    private fun hentForrigeBehandling(sakId: SakId) =
        behandlingService.hentSisteIverksatteBehandling(sakId)
            ?: throw InternfeilException("Fant ikke iverksatt behandling sak=$sakId")

    private fun hentPdlPersonopplysning(sak: Sak) =
        pdlTjenesterKlient
            .hentPdlModellForSaktype(sak.ident, PersonRolle.GJENLEVENDE, SakType.OMSTILLINGSSTOENAD)
            .toPerson()

    private fun hentPdlPersonident(sak: Sak) =
        runBlocking {
            pdlTjenesterKlient.hentPdlIdentifikator(sak.ident)
                ?: throw InternfeilException("Fant ikke ident fra PDL for sak ${sak.id}")
        }

    private fun hentOpplysningerGjenny(
        sak: Sak,
        sisteBehandlingId: UUID,
    ): Personopplysning =
        grunnlagService
            .hentPersonopplysninger(
                sisteBehandlingId,
                sak.sakType,
            ).soeker ?: throw InternfeilException("Fant ikke opplysninger for behandling=$sisteBehandlingId")

    private fun manuellBehandlingSkruddPaa(): Boolean =
        featureToggleService.isEnabled(ManuellBehandlingToggle.MANUELL_BEHANDLING, defaultValue = false)

    private fun erLikeVergemaal(
        vergerEn: List<VergemaalEllerFremtidsfullmakt>?,
        vergerTo: List<VergemaalEllerFremtidsfullmakt>?,
    ): Boolean {
        if (vergerEn.isNullOrEmpty() && vergerTo.isNullOrEmpty()) {
            return true
        }
        return vergerEn == vergerTo
    }

    private fun genererManuellBegrunnelseTekst(aarsakTilManuell: AarligInntektsjusteringAarsakManuell): String =
        "Inntektsjustering for neste år må behandles manuelt. Årsak: ${aarsakTilManuell.name}"
}

enum class AarligInntektsjusteringAarsakManuell {
    UTDATERT_IDENT,
    UTDATERTE_PERSONO_INFO,
    VERGEMAAL,
    TIL_SAMORDNING,
    AAPEN_BEHANDLING,
    HAR_SANKSJON,
    HAR_OPPHOER_FOM,
    ALDERSOVERGANG_67,
    HAR_OVERSTYRT_BEREGNING,
}

enum class ManuellBehandlingToggle(
    val value: String,
) : FeatureToggle {
    MANUELL_BEHANDLING("aarlig-inntektsjustering-la-manuell-behandling"),
    ;

    override fun key(): String = this.value
}
