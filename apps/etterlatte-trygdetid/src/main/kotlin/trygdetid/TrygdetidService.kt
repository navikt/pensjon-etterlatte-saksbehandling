package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.ATTESTERT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.AVKORTET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.BEREGNET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.FATTET_VEDTAK
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.IVERKSATT
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.SAMORDNET
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.TIL_SAMORDNING
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.feilhaandtering.sjekkIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.avtale.AvtaleService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.PesysKlient
import no.nav.etterlatte.trygdetid.klienter.Trygdetidsgrunnlag
import no.nav.etterlatte.trygdetid.klienter.TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon
import no.nav.etterlatte.trygdetid.klienter.VedtaksvurderingKlient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

interface TrygdetidService {
    suspend fun hentTrygdetiderIBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>

    suspend fun hentTrygdetidIBehandlingMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid?

    suspend fun opprettTrygdetiderForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        overskriv: Boolean = false,
    ): List<Trygdetid>

    suspend fun harTrygdetidsgrunnlagIPesysForApOgUfoere(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun leggInnTrygdetidsgrunnlagFraPesys(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>

    suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun slettTrygdetidGrunnlagForTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun slettPesysTrygdetidGrunnlagForTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun overstyrNorskPoengaaarForTrygdetid(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun setYrkesskade(
        trygdetidId: UUID,
        behandlingId: UUID,
        yrkesskade: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun oppdaterTrygdetidMedBegrunnelse(
        trygdetidId: UUID,
        behandlingId: UUID,
        begrunnelse: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun kopierSisteTrygdetidberegninger(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>

    suspend fun kopierOgOverskrivTrygdetid(
        behandlingId: UUID,
        kildeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<TrygdetidDto>

    fun overstyrBeregnetTrygdetidForAvdoed(
        behandlingId: UUID,
        ident: String,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid

    suspend fun oppdaterOpplysningsgrunnlagForTrygdetider(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>

    suspend fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun opprettOverstyrtBeregnetTrygdetid(
        behandlingId: UUID,
        overskriv: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun finnBehandlingMedTrygdetidForSammeAvdoede(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): UUID?
}

data class TrygdetidPeriodePesys(
    val isoCode: String, // ISO 3166-1 alpha-3 code:
    val fra: LocalDate,
    val til: LocalDate,
    val poengInnAar: Boolean?,
    val poengUtAar: Boolean?,
    val prorata: Boolean?,
    val kilde: Grunnlagsopplysning.PesysYtelseKilde, // GrunnlagKildeCode i Pesys
)

private fun Trygdetidsgrunnlag.tilTrygdetidsPeriode(kilde: Grunnlagsopplysning.PesysYtelseKilde): TrygdetidPeriodePesys =
    TrygdetidPeriodePesys(
        isoCode = land!!,
        fra = fomDato,
        til = tomDato,
        poengInnAar = poengIInnAr,
        poengUtAar = poengIUtAr,
        prorata = ikkeProRata?.not(),
        kilde = kilde,
    )

private fun TrygdetidPeriodePesys.fraPesystilVanlig(): TrygdetidPeriode =
    TrygdetidPeriode(
        fra = fra,
        til = til,
    )

private const val AUTOMATISK_FREMTIDIG_BEGRUNNELSE = "Automatisk beregnet fremtidig trygdetid"

class TrygdetidServiceImpl(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService,
    private val pesysKlient: PesysKlient,
    private val avtaleService: AvtaleService,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val featureToggleService: FeatureToggleService,
) : TrygdetidService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val SIST_FREMTIDIG_TRYGDETID_ALDER = 66L
    }

    override suspend fun hentTrygdetiderIBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> =
        trygdetidRepository
            .hentTrygdetiderForBehandling(behandlingId)
            .mapNotNull { trygdetid -> sjekkTrygdetidMotGrunnlag(trygdetid, brukerTokenInfo) }
            .sortedBy { it.ident }

    override suspend fun hentTrygdetidIBehandlingMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? =
        trygdetidRepository
            .hentTrygdetidMedId(behandlingId, trygdetidId)
            ?.let { trygdetid -> sjekkTrygdetidMotGrunnlag(trygdetid, brukerTokenInfo) }

    override suspend fun opprettTrygdetiderForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        overskriv: Boolean,
    ) = kanOppdatereTrygdetid(
        behandlingId,
        brukerTokenInfo,
    ) {
        val avdoede = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo).hentAvdoede()
        val eksisterendeTrygdetider =
            trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).let { trygdetider ->
                if (overskriv) {
                    trygdetider
                        .forEach { trygdetid -> trygdetidRepository.slettTrygdetid(trygdetid.id) }
                    emptyList()
                } else {
                    trygdetider
                }
            }

        if (eksisterendeTrygdetider.isNotEmpty() && avdoede.size == eksisterendeTrygdetider.size) {
            throw TrygdetidAlleredeOpprettetException()
        }

        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        when (behandling.behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                val ukjentAvdoed = avdoede.isEmpty()
                val tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar ?: false

                if (ukjentAvdoed || tidligereFamiliepleier) {
                    logger.info(
                        "Oppretter overstyrt trygdetid for behandling $behandlingId " +
                            "(ukjentAvdoed=$ukjentAvdoed, tidligereFamiliepleier=$tidligereFamiliepleier)",
                    )
                    listOf(opprettOverstyrtBeregnetTrygdetid(behandlingId, true, brukerTokenInfo))
                } else {
                    logger.info("Oppretter trygdetid for behandling $behandlingId")
                    opprettTrygdetiderForBehandling(behandling, eksisterendeTrygdetider, avdoede, brukerTokenInfo)
                }
            }

            BehandlingType.REVURDERING -> {
                val sisteIverksatteBehandling =
                    vedtaksvurderingKlient
                        .hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
                        .sortedByDescending { it.datoFattet }
                        .first { it.vedtakType != VedtakType.OPPHOER } // Opphør har ikke trygdetid

                val forrigeTrygdetider =
                    hentTrygdetiderIBehandling(sisteIverksatteBehandling.behandlingId, brukerTokenInfo)
                if (forrigeTrygdetider.isEmpty()) {
                    opprettTrygdetiderForRevurdering(behandling, eksisterendeTrygdetider, avdoede, brukerTokenInfo)
                } else {
                    val kopierteTrygdetider =
                        kopierSisteTrygdetidberegninger(behandling, forrigeTrygdetider, eksisterendeTrygdetider)
                    kopierAvtale(behandling.id, sisteIverksatteBehandling.behandlingId)
                    opprettTrygdetiderForRevurdering(behandling, kopierteTrygdetider, avdoede, brukerTokenInfo)
                }
            }
        }
    }.also { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo) }

    private suspend fun opprettTrygdetiderForRevurdering(
        behandling: DetaljertBehandling,
        eksisterendeTrygdetider: List<Trygdetid>,
        avdoede: List<Grunnlagsdata<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) = if (behandling.revurderingsaarsak == Revurderingaarsak.REGULERING &&
        behandling.prosesstype == Prosesstype.AUTOMATISK
    ) {
        logger.info("Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt")
        throw ManglerForrigeTrygdetidMaaReguleresManuelt()
    } else {
        logger.info("Oppretter trygdetid for behandling ${behandling.id} revurdering")
        opprettTrygdetiderForBehandling(behandling, eksisterendeTrygdetider, avdoede, brukerTokenInfo)
    }

    private suspend fun opprettTrygdetiderForBehandling(
        behandling: DetaljertBehandling,
        eksisterendeTrygdetider: List<Trygdetid>,
        avdoede: List<Grunnlagsdata<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) = avdoede
        .map { avdoed ->
            val fnr =
                krevIkkeNull(avdoed.hentFoedselsnummer()?.verdi?.value) {
                    "Kunne ikke hente identifikator for avdød til trygdetid i " +
                        "behandlingen med id=${behandling.id}"
                }

            Pair(fnr, avdoed)
        }.filter { avdoedMedFnr ->
            eksisterendeTrygdetider.none { avdoedMedFnr.first == it.ident }
        }.map { avdoedMedFnr ->
            val trygdetid =
                Trygdetid(
                    sakId = behandling.sak,
                    behandlingId = behandling.id,
                    opplysninger = hentOpplysninger(avdoedMedFnr.second, behandling.id),
                    ident = avdoedMedFnr.first,
                    yrkesskade = false,
                )

            val opprettetTrygdetid = trygdetidRepository.opprettTrygdetid(trygdetid)

            val oppdatertTrygdetid =
                opprettFremtidigTrygdetidForAvdoed(opprettetTrygdetid, avdoedMedFnr.second, brukerTokenInfo)

            oppdatertTrygdetid ?: opprettetTrygdetid
        }.also {
            logger.info("Opprettet ${it.size} trygdetider for behandling=${behandling.id}")
        }

    override suspend fun harTrygdetidsgrunnlagIPesysForApOgUfoere(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        val avdoede = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo).hentAvdoede()
        val perioderIPesys =
            avdoede
                .map { avdoed ->
                    val fnr =
                        sjekkIkkeNull(avdoed.hentFoedselsnummer()?.verdi?.value) {
                            "Kunne ikke hente identifikator for avdød til trygdetid i " +
                                "behandlingen med id=$behandlingId"
                        }

                    Pair(fnr, avdoed)
                }.map { avdoedMedFnr ->
                    val doedsdato =
                        avdoedMedFnr.second.hentDoedsdato()?.verdi
                            ?: throw UgyldigForespoerselException("INGEN_AVDOEDE", "Avdød mangler dødsdato")
                    val trygdetidForUfoereOgAlderspensjon =
                        pesysKlient.hentTrygdetidsgrunnlag(
                            Pair(avdoedMedFnr.first, doedsdato),
                            brukerTokenInfo,
                        )
                    trygdetidForUfoereOgAlderspensjon
                }.map {
                    mapTrygdetidsgrunnlagFraPesys(it)
                }
        return perioderIPesys.flatten().isNotEmpty()
    }

    override suspend fun leggInnTrygdetidsgrunnlagFraPesys(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        if (!behandling.status.kanEndres()) {
            throw UgyldigForespoerselException(
                code = "UGYLDIG_TILSTAND_TRYGDETID",
                detail = "Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand",
            )
        }
        val avdoede = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo).hentAvdoede()
        when (behandling.behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                val ukjentAvdoed = avdoede.isEmpty()
                val tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar ?: false

                if (ukjentAvdoed || tidligereFamiliepleier) {
                    throw InternfeilException("Støtter ikke pesys henting for ukjent avdød eller tidligere Familiepleier")
                }
            }

            else -> {
                throw InternfeilException("Kan kun hente inn trygdetider for førstegangsbehandling")
            }
        }

        return avdoede
            .map { avdoed ->
                val fnr =
                    krevIkkeNull(avdoed.hentFoedselsnummer()?.verdi?.value) {
                        "Kunne ikke hente identifikator for avdød til trygdetid i " +
                            "behandlingen med id=$behandlingId"
                    }

                Pair(fnr, avdoed)
            }.map { avdoedMedFnr ->
                val hentTrygdetid =
                    trygdetidRepository.hentTrygdetid(behandlingId)
                        ?: throw InternfeilException("Trygdetid er ikke opprettet")
                val doedsdato =
                    avdoedMedFnr.second.hentDoedsdato()?.verdi ?: throw InternfeilException("Avdød mangler dødsdato")

                val trygdetidForUfoereOgAlderspensjon =
                    pesysKlient.hentTrygdetidsgrunnlag(
                        Pair(avdoedMedFnr.first, doedsdato),
                        brukerTokenInfo,
                    )

                val hentetTrygdetidMedPesysTrygdetid =
                    populerTrygdetidsGrunnlagFraPesys(hentTrygdetid, trygdetidForUfoereOgAlderspensjon)
                trygdetidRepository.oppdaterTrygdetid(hentetTrygdetidMedPesysTrygdetid)
                val oppdatertTrygdetid =
                    opprettFremtidigTrygdetidForAvdoed(
                        hentetTrygdetidMedPesysTrygdetid,
                        avdoedMedFnr.second,
                        brukerTokenInfo,
                    )

                oppdatertTrygdetid ?: hentetTrygdetidMedPesysTrygdetid
            }
    }

    private fun populerTrygdetidsGrunnlagFraPesys(
        trygdetid: Trygdetid,
        pesysTrygdetidsgrunnlag: TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon,
    ): Trygdetid = trygdetid.copy(trygdetidGrunnlag = mapTrygdetidsgrunnlagFraPesys(pesysTrygdetidsgrunnlag))

    private fun mapTrygdetidsgrunnlagFraPesys(
        pesysTrygdetidsgrunnlag: TrygdetidsgrunnlagUfoeretrygdOgAlderspensjon,
    ): List<TrygdetidGrunnlag> {
        val mappedAlderspensjonTrygdetidsgrunnlag =
            pesysTrygdetidsgrunnlag.trygdetidAlderspensjon?.trygdetidsgrunnlagListe?.trygdetidsgrunnlagListe?.map {
                mapPesysTrygdetidsgrunnlag(it, Grunnlagsopplysning.Alderspensjon.create())
            } ?: emptyList()

        val mappedUfoereTrygdetidsgrunnlag =
            pesysTrygdetidsgrunnlag.trygdetidUfoeretrygdpensjon?.trygdetidsgrunnlagListe?.trygdetidsgrunnlagListe?.map {
                mapPesysTrygdetidsgrunnlag(it, Grunnlagsopplysning.Ufoeretrygd.create())
            } ?: emptyList()

        if (mappedAlderspensjonTrygdetidsgrunnlag.isNotEmpty()) {
            return mappedAlderspensjonTrygdetidsgrunnlag
        } else if (mappedUfoereTrygdetidsgrunnlag.isNotEmpty()) {
            return mappedUfoereTrygdetidsgrunnlag
        }
        logger.info("Fant ingen trygdtidsgrunnlag som vi kunne mappe over")
        return emptyList()
    }

    private fun mapPesysTrygdetidsgrunnlag(
        tt: Trygdetidsgrunnlag,
        kilde: Grunnlagsopplysning.PesysYtelseKilde,
    ): TrygdetidGrunnlag {
        val trygdetidsperiode = tt.tilTrygdetidsPeriode(kilde)
        return TrygdetidGrunnlag(
            id = UUID.randomUUID(),
            type = TrygdetidType.FAKTISK,
            bosted = tt.land ?: "Ukjent",
            periode = trygdetidsperiode.fraPesystilVanlig(),
            kilde = trygdetidsperiode.kilde,
            beregnetTrygdetid = null,
            poengUtAar = trygdetidsperiode.poengUtAar == true,
            poengInnAar = trygdetidsperiode.poengInnAar == true,
            prorata = trygdetidsperiode.prorata == true,
            begrunnelse = null,
        )
    }

    private suspend fun opprettFremtidigTrygdetidForAvdoed(
        trygdetid: Trygdetid,
        avdoed: Grunnlagsdata<JsonNode>,
        brukerTokenInfo: BrukerTokenInfo,
    ) = beregnfremtidigTrygdetidsPeriode(avdoed)?.let { periode ->
        lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
            trygdetid.behandlingId,
            trygdetid.id,
            TrygdetidGrunnlag(
                id = UUID.randomUUID(),
                type = TrygdetidType.FREMTIDIG,
                bosted = LandNormalisert.NORGE.isoCode,
                periode = periode,
                kilde =
                    Grunnlagsopplysning.Saksbehandler(
                        Grunnlagsopplysning.automatiskSaksbehandler.ident,
                        Tidspunkt.now(),
                    ),
                begrunnelse = AUTOMATISK_FREMTIDIG_BEGRUNNELSE,
                poengInnAar = false,
                poengUtAar = false,
                prorata = false,
            ),
            brukerTokenInfo,
        )
    }

    private fun beregnfremtidigTrygdetidsPeriode(avdoed: Grunnlagsdata<JsonNode>): TrygdetidPeriode? {
        val doedsDato = avdoed.hentDoedsdato()?.verdi

        val sistFremtidigDato =
            avdoed
                .hentFoedselsdato()
                ?.verdi
                ?.plusYears(
                    SIST_FREMTIDIG_TRYGDETID_ALDER,
                )?.with(TemporalAdjusters.firstDayOfNextYear())
                ?.minusDays(1)

        return if (doedsDato != null && sistFremtidigDato != null && doedsDato.isBefore(sistFremtidigDato)) {
            TrygdetidPeriode(doedsDato, sistFremtidigDato)
        } else {
            null
        }
    }

    override suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandlingMedSjekk(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            // Hvis vi oppdaterer en trygdetid som har automatisk begrunnelse, fjern automatisk begrunnelse
            val trygdetidGrunnlagForOppdatering =
                when (trygdetidGrunnlag.begrunnelse) {
                    AUTOMATISK_FREMTIDIG_BEGRUNNELSE -> trygdetidGrunnlag.copy(begrunnelse = null)
                    else -> trygdetidGrunnlag
                }

            lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
                behandlingId,
                trygdetidId,
                trygdetidGrunnlagForOppdatering,
                brukerTokenInfo,
            )
        }

    private suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val gjeldendeTrygdetid: Trygdetid =
            trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: throw GenerellIkkeFunnetException()

        val trygdetidGrunnlagBeregnet: TrygdetidGrunnlag =
            trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
                beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag),
            )

        val trygdetidMedOppdatertTrygdetidGrunnlag: Trygdetid =
            gjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

        return oppdaterBeregnetTrygdetid(behandlingId, trygdetidMedOppdatertTrygdetidGrunnlag, brukerTokenInfo)
    }

    private data class DatoerForBehandling(
        val foedselsDato: LocalDate,
        val doedsDato: LocalDate,
    )

    private fun hentDatoerForBehandling(trygdetid: Trygdetid): DatoerForBehandling =
        DatoerForBehandling(
            toLocalDate(trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FOEDSELSDATO })
                ?: throw IngenFoedselsdatoForAvdoedFunnet(trygdetid.id),
            toLocalDate(trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.DOEDSDATO })
                ?: throw IngenDoedsdatoForAvdoedFunnet(trygdetid.id),
        )

    override suspend fun slettTrygdetidGrunnlagForTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository
                    .hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?.slettTrygdetidGrunnlag(trygdetidGrunnlagId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            oppdaterBeregnetTrygdetid(behandlingId, trygdetid, brukerTokenInfo)
        }

    override suspend fun slettPesysTrygdetidGrunnlagForTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository
                    .hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val altUnntattPesysKildeGrunnlag =
                trygdetid.copy(
                    trygdetidGrunnlag =
                        trygdetid.trygdetidGrunnlag.filter {
                            when (it.kilde) {
                                is Grunnlagsopplysning.PesysYtelseKilde -> false
                                else -> true
                            }
                        },
                )
            oppdaterBeregnetTrygdetid(behandlingId, altUnntattPesysKildeGrunnlag, brukerTokenInfo)
        }

    override suspend fun kopierSisteTrygdetidberegninger(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra behandling $forrigeBehandlingId")

        kopierAvtale(behandlingId, forrigeBehandlingId)

        val forrigeTrygdetid = hentTrygdetiderIBehandling(forrigeBehandlingId, brukerTokenInfo)
        val eksisterendeTrygdetider = hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)

        if (eksisterendeTrygdetider.isNotEmpty()) { // Interessert i om dette forekommer noen gang
            logger.info(
                "Det eksisterer trygdetider fra før ved kopiering av trygdetider " +
                    "fra behandling $forrigeBehandlingId til $behandlingId",
            )
        }

        val alleTrygdetider =
            kopierSisteTrygdetidberegninger(behandling, forrigeTrygdetid, eksisterendeTrygdetider)

        if (featureToggleService.isEnabled(TrygdetidToggles.OPPDATER_BEREGNET_TRYGDETID_VED_KOPIERING, false)) {
            alleTrygdetider.forEach { trygdetid ->
                val erOverstyrt = trygdetid.beregnetTrygdetid?.resultat?.overstyrt == true
                if (!erOverstyrt && behandling.prosesstype != Prosesstype.AUTOMATISK) {
                    oppdaterBeregnetTrygdetid(behandlingId, trygdetid, brukerTokenInfo)
                } else {
                    behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
                }
            }
        }
        return alleTrygdetider
    }

    private fun kopierAvtale(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ) {
        val avtale =
            avtaleService
                .hentAvtaleForBehandling(forrigeBehandlingId)
                ?.copy(id = UUID.randomUUID(), behandlingId = behandlingId)

        if (avtale == null) {
            logger.info("Fant ingen avtale på forrigeBehandling – hopper over kopiering av avtale")
        } else {
            logger.info("Kopierer avtale fra forrigeBehandling=$forrigeBehandlingId til nyBehandling=$behandlingId")

            avtaleService.opprettAvtale(avtale)
        }
    }

    private fun kopierSisteTrygdetidberegninger(
        behandling: DetaljertBehandling,
        forrigeTrygdetider: List<Trygdetid>,
        eksisterendeTrygdetider: List<Trygdetid>,
    ): List<Trygdetid> {
        logger.info(
            "Kopierer trygdetid for behandling ${behandling.id} fra " +
                "trygdetider med id ${forrigeTrygdetider.joinToString { it.id.toString() }}",
        )

        val opprettetTrygdetider =
            forrigeTrygdetider
                .filter { forrigeTrygdetid ->
                    eksisterendeTrygdetider.none { it.ident == forrigeTrygdetid.ident }
                }.map { forrigeTrygdetid ->
                    val kopiertTrygdetid =
                        Trygdetid(
                            sakId = behandling.sak,
                            behandlingId = behandling.id,
                            opplysninger = forrigeTrygdetid.opplysninger.map { it.copy(id = UUID.randomUUID()) },
                            trygdetidGrunnlag = forrigeTrygdetid.trygdetidGrunnlag.map { it.copy(id = UUID.randomUUID()) },
                            beregnetTrygdetid = forrigeTrygdetid.beregnetTrygdetid,
                            ident = forrigeTrygdetid.ident,
                            yrkesskade = forrigeTrygdetid.yrkesskade,
                            overstyrtNorskPoengaar = forrigeTrygdetid.overstyrtNorskPoengaar,
                            kopiertGrunnlagFraBehandling = forrigeTrygdetid.behandlingId,
                            begrunnelse = forrigeTrygdetid.begrunnelse,
                        )

                    trygdetidRepository.opprettTrygdetid(kopiertTrygdetid)
                }

        return eksisterendeTrygdetider + opprettetTrygdetider
    }

    private fun kildeFoedselsnummer(): Grunnlagsopplysning.RegelKilde =
        Grunnlagsopplysning.RegelKilde(
            "Beregnet basert på fødselsdato fra pdl",
            Tidspunkt.now(),
            "1",
        )

    private fun hentOpplysninger(
        avdoed: Grunnlagsdata<JsonNode>,
        behandlingId: UUID,
    ): List<Opplysningsgrunnlag> {
        val avodedDoedsdato = avdoed.hentDoedsdato()

        val avdoededsDatoOpplysning =
            avodedDoedsdato?.verdi?.let {
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, avodedDoedsdato.kilde, it)
            }

        val foedselsdato = avdoed.hentFoedselsdato()
        val foedselsdatoVerdi =
            foedselsdato?.verdi ?: throw TrygdetidMaaHaFoedselsdatoException(behandlingId)

        return listOfNotNull(
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FOEDSELSDATO,
                foedselsdato.kilde,
                foedselsdatoVerdi,
            ),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLT_16,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid fra tidspunkt en person er fylt 16 år
                foedselsdatoVerdi.plusYears(16),
            ),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLLER_66,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid frem til tidspunkt en person er fyller 66 pår
                foedselsdatoVerdi.plusYears(SIST_FREMTIDIG_TRYGDETID_ALDER),
            ),
            avdoededsDatoOpplysning,
        )
    }

    private suspend fun <T> kanOppdatereTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> T,
    ): T {
        val kanFastsetteTrygdetid = behandlingKlient.kanOppdatereTrygdetid(behandlingId, brukerTokenInfo)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            logger.info("Kan ikke opprette/endre trygdetid da behandlingen med id=$behandlingId er i feil tilstand")

            throw UgyldigForespoerselException(
                code = "UGYLDIG_TILSTAND_TRYGDETID",
                detail = "Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand",
            )
        }
    }

    override suspend fun opprettOverstyrtBeregnetTrygdetid(
        behandlingId: UUID,
        overskriv: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        // Merk: vi kan ikke bruke den vanlige sjekken på kanOppdatereTrygdetid, siden dette kallet skjer typisk
        // når behandlingen akkurat er opprettet. Men det er viktig at vi ikke tillater endringer hvis behandlingen
        // ikke kan endres.
        if (behandling.status !in BehandlingStatus.kanEndres()) {
            throw IkkeTillattException(
                "BEHANLDING_KAN_IKKE_ENDRES",
                "Kan ikke opprette overstyrt trygdetid i en behandling som ikke kan endres",
            )
        }

        val trygdetider = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)

        if (trygdetider.isNotEmpty()) {
            if (overskriv) {
                logger.warn("Sletter ${trygdetider.size} trygdetid(er) for behandling (id=$behandlingId)")
                trygdetider.forEach { trygdetidRepository.slettTrygdetid(it.id) }
            } else {
                throw TrygdetidAlleredeOpprettetException()
            }
        }
        logger.info("Oppretter manuell overstyrt trygdetid for behandling $behandlingId")

        val avdoede =
            grunnlagKlient
                .hentGrunnlag(behandling.id, brukerTokenInfo)
                .hentAvdoede()
        val avdoed =
            if (avdoede.isNotEmpty()) {
                val avdoedViKoblerTrygdetidPaa =
                    if (avdoede.size == 1) {
                        avdoede.first()
                    } else {
                        // TODO: Det er mest sannsynlig den med tidligst dødsdato som er aktuell,
                        //   men det er muligens behov for å overstyre trygdetiden til en eller flere avdøde,
                        //   og dette burde bli angitt i requesten.
                        //   slik det er nå får man ikke muliggheten til å godt overstyre trygdetiden hvis det er
                        //   flere avdøde i saken og man trenger flere ulike perioder for de to avdøde.
                        avdoede.minBy { it.hentDoedsdato()?.verdi ?: LocalDate.MAX }
                    }
                if (avdoedViKoblerTrygdetidPaa.hentDoedsdato()?.verdi == null) {
                    throw UgyldigForespoerselException(
                        "KJENT_AVDOED_MANGLER_DOEDSDATO",
                        "Persongalleriet inneholder en kjent avdød som ikke har en dødsdato registrert. For å " +
                            "overstyre trygdetid i saken må avdød angitt i persongalleriet få registrert en dødsdato " +
                            "eller persongalleriet må rettes på.",
                    )
                } else {
                    avdoedViKoblerTrygdetidPaa
                }
            } else {
                null // ukjent avdød
            }

        val tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar ?: false

        val ident =
            if (tidligereFamiliepleier) {
                krevIkkeNull(behandling.soeker) {
                    "Kunne ikke hente identifikator for soeker til trygdetid i " +
                        "behandlingen med id=$behandlingId"
                }
            } else {
                avdoed?.let {
                    krevIkkeNull(it.hentFoedselsnummer()?.verdi?.value) {
                        "Kunne ikke hente identifikator for avdød til trygdetid i " +
                            "behandlingen med id=$behandlingId"
                    }
                } ?: UKJENT_AVDOED
            }

        val trygdetid =
            Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandlingId,
                opplysninger = avdoed?.let { hentOpplysninger(it, behandlingId) } ?: emptyList(),
                ident = ident,
                trygdetidGrunnlag = emptyList(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetid(
                        resultat =
                            DetaljertBeregnetTrygdetidResultat(
                                faktiskTrygdetidNorge = null,
                                faktiskTrygdetidTeoretisk = null,
                                fremtidigTrygdetidNorge = null,
                                fremtidigTrygdetidTeoretisk = null,
                                samletTrygdetidNorge = null,
                                samletTrygdetidTeoretisk = null,
                                prorataBroek = null,
                                overstyrt = true,
                                yrkesskade = false,
                                beregnetSamletTrygdetidNorge = null,
                            ),
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                    ),
                yrkesskade = false,
            )
        return trygdetidRepository.opprettTrygdetid(trygdetid)
    }

    override suspend fun setYrkesskade(
        trygdetidId: UUID,
        behandlingId: UUID,
        yrkesskade: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?: throw TrygdetidIkkeFunnetForBehandling()

            logger.info("Oppdatere yrkesskade $yrkesskade for trygdetid $trygdetidId for behandling $behandlingId")

            oppdaterBeregnetTrygdetid(behandlingId, trygdetid.copy(yrkesskade = yrkesskade), brukerTokenInfo)
        }

    override suspend fun oppdaterTrygdetidMedBegrunnelse(
        trygdetidId: UUID,
        behandlingId: UUID,
        begrunnelse: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository
                    .hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?: throw TrygdetidIkkeFunnetForBehandling()

            trygdetidRepository
                .oppdaterTrygdetid(trygdetid.oppdaterBegrunnelse(begrunnelse))
                .let { sjekkTrygdetidMotGrunnlag(it, brukerTokenInfo)!! }
        }

    override fun overstyrBeregnetTrygdetidForAvdoed(
        behandlingId: UUID,
        ident: String,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid {
        val trygdetid =
            trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).find { it.ident == ident }
                ?: throw GenerellIkkeFunnetException()

        val oppdatertTrygdetid =
            trygdetid.copy(
                trygdetidGrunnlag = emptyList(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetid(
                        resultat = beregnetTrygdetid.copy(overstyrt = true),
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                    ),
            )

        return trygdetidRepository.oppdaterTrygdetid(oppdatertTrygdetid)
    }

    override suspend fun oppdaterOpplysningsgrunnlagForTrygdetider(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val trygdetidList = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)
        if (trygdetidList.isEmpty()) {
            throw IngenTrygdetidFunnetForAvdoede()
        }
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Oppdaterer opplysningsgrunnlag for trygdetider (behandlingId=$behandlingId)")

        val avdoede = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo).hentAvdoede()
        return trygdetidList
            .map { trygdetid -> medOppdaterteOpplysningerOmAvdoede(trygdetid, avdoede, behandlingId) }
            .map { trygdetid -> oppdaterBeregnetTrygdetid(behandlingId, trygdetid, brukerTokenInfo) }
    }

    private fun medOppdaterteOpplysningerOmAvdoede(
        trygdetid: Trygdetid,
        avdoede: List<Grunnlagsdata<JsonNode>>,
        behandlingId: UUID,
    ) = trygdetid.copy(
        opplysninger =
            hentOpplysninger(
                avdoede.first { it.hentFoedselsnummer()?.verdi?.value == trygdetid.ident },
                behandlingId,
            ),
    )

    override suspend fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetider = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

            if (trygdetider.isEmpty()) {
                throw IngenTrygdetidFunnetForAvdoede()
            }

            if (trygdetider.any { it.beregnetTrygdetid == null }) {
                throw TrygdetidManglerBeregning()
            }

            // Dersom forrige steg (vilkårsvurdering) har blitt endret vil statusen være VILKAARSVURDERT. Når man
            // trykker videre fra vilkårsvurdering skal denne validere tilstand og sette status TRYGDETID_OPPDATERT.
            if (behandling.status == BehandlingStatus.VILKAARSVURDERT) {
                behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
            } else {
                false
            }
        }

    private suspend fun sjekkTrygdetidMotGrunnlag(
        trygdetid: Trygdetid,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val soeker = grunnlagKlient.hentGrunnlag(trygdetid.behandlingId, brukerTokenInfo).soeker
        if (trygdetid.ident == UKJENT_AVDOED || trygdetid.ident == soeker.hentFoedselsnummer()?.verdi?.value) {
            return trygdetid
                .copy(opplysningerDifferanse = OpplysningerDifferanse(false, GrunnlagOpplysningerDto.tomt()))
        }
        val nyAvdoedGrunnlag = grunnlagKlient.hentGrunnlag(trygdetid.behandlingId, brukerTokenInfo).hentAvdoede()
        val avdoedeFnr = nyAvdoedGrunnlag.mapNotNull { it.hentFoedselsnummer()?.verdi?.value }
        if (!avdoedeFnr.contains(trygdetid.ident)) {
            trygdetidRepository.slettTrygdetid(trygdetid.id)
            return null
        }
        return trygdetid.copy(
            opplysningerDifferanse =
                finnOpplysningerDifferanse(
                    trygdetid,
                    nyAvdoedGrunnlag.firstOrNull { it.hentFoedselsnummer()?.verdi?.value == trygdetid.ident },
                ),
        )
    }

    private fun finnOpplysningerDifferanse(
        trygdetid: Trygdetid,
        nyAvdoedGrunnlag: Grunnlagsdata<JsonNode>?,
    ): OpplysningerDifferanse {
        if (
            trygdetid.beregnetTrygdetid
                ?.resultat
                ?.overstyrt
                .let { it != null && it }
        ) {
            return OpplysningerDifferanse(
                differanse = false,
                GrunnlagOpplysningerDto.tomt(),
            )
        }

        val nyeOpplysninger =
            if (nyAvdoedGrunnlag != null) hentOpplysninger(nyAvdoedGrunnlag, trygdetid.behandlingId) else emptyList()

        if (nyeOpplysninger.isEmpty() && trygdetid.opplysninger.isEmpty()) {
            return OpplysningerDifferanse(false, GrunnlagOpplysningerDto(null, null, null, null))
        }

        val nyFoedselsdato = nyeOpplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FOEDSELSDATO }
        val nyDoedsdato = nyeOpplysninger.firstOrNull { it.type == TrygdetidOpplysningType.DOEDSDATO }
        val nyFylt16 = nyeOpplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FYLT_16 }
        val nyFyller66 = nyeOpplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FYLLER_66 }

        val eksisterendeFoedselsdato =
            trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FOEDSELSDATO }
        val eksisterendeDoedsdato = trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.DOEDSDATO }
        val eksisterendeFylt16 = trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FYLT_16 }
        val eksisterendeFyller66 = trygdetid.opplysninger.firstOrNull { it.type == TrygdetidOpplysningType.FYLLER_66 }
        val diff =
            toLocalDate(nyFoedselsdato) != toLocalDate(eksisterendeFoedselsdato) ||
                toLocalDate(nyDoedsdato) != toLocalDate(eksisterendeDoedsdato) ||
                toLocalDate(nyFylt16) != toLocalDate(eksisterendeFylt16) ||
                toLocalDate(nyFyller66) != toLocalDate(eksisterendeFyller66)

        return OpplysningerDifferanse(
            diff,
            nyeOpplysninger.toDto(),
        )
    }

    private fun toLocalDate(opplysningsgrunnlag: Opplysningsgrunnlag?): LocalDate? =
        opplysningsgrunnlag?.let {
            objectMapper.readValue(it.opplysning.toString())
        }

    override suspend fun overstyrNorskPoengaaarForTrygdetid(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?: throw GenerellIkkeFunnetException()

            oppdaterBeregnetTrygdetid(
                behandlingId,
                trygdetid.copy(overstyrtNorskPoengaar = overstyrtNorskPoengaar),
                brukerTokenInfo,
            )
        }

    private suspend fun oppdaterBeregnetTrygdetid(
        behandlingId: UUID,
        trygdetid: Trygdetid,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val datoer = hentDatoerForBehandling(trygdetid)
        val nordiskKonvensjon = avtaleService.hentAvtaleForBehandling(behandlingId)?.nordiskTrygdeAvtale == JaNei.JA

        val nyBeregnetTrygdetid =
            beregnTrygdetidService.beregnTrygdetid(
                trygdetidGrunnlag = trygdetid.trygdetidGrunnlag,
                datoer.foedselsDato,
                datoer.doedsDato,
                trygdetid.overstyrtNorskPoengaar,
                trygdetid.yrkesskade,
                nordiskKonvensjon,
            )
        return when (nyBeregnetTrygdetid) {
            null -> trygdetid.nullstillBeregnetTrygdetid()
            else -> trygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
        }.also { nyTrygdetid ->
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
        }
    }

    override suspend fun kopierOgOverskrivTrygdetid(
        behandlingId: UUID,
        kildeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<TrygdetidDto> =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            logger.info("Kopierer trygdetidsgrunnlag for behandling $behandlingId fra behandling $kildeBehandlingId")
            val trygdetiderMaal = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)
            val trygdetiderKilde = trygdetidRepository.hentTrygdetiderForBehandling(kildeBehandlingId)
            sjekkAtTrygdetideneGjelderSammeAvdoede(trygdetiderMaal, trygdetiderKilde, behandlingId, kildeBehandlingId)

            trygdetiderMaal.forEach { trygdetidRepository.slettTrygdetid(it.id) }

            kopierSisteTrygdetidberegninger(behandlingId, kildeBehandlingId, brukerTokenInfo)

            hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo)
                .map { it.toDto() }
        }

    override suspend fun finnBehandlingMedTrygdetidForSammeAvdoede(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): UUID? {
        val avdoede: List<Folkeregisteridentifikator> =
            grunnlagKlient
                .hentGrunnlag(behandlingId, brukerTokenInfo)
                .hentAvdoede()
                .mapNotNull { it.hentFoedselsnummer()?.verdi }
        if (avdoede.isEmpty()) {
            return null
        }

        if (!lagredeTrygdetiderHarSammeAvdoede(behandlingId, avdoede)) {
            logger.warn(
                "Avdøde i grunnlag stemmer ikke med avdøde i trygdetider. " +
                    "Returnerer ingen behandling med trygdetid for samme avdøde.",
            )
            return null
        }

        return behandlingMedTrygdetiderForAvdoede(avdoede)
            .filter { it != behandlingId }
            .firstOrNull { behandlingStatusOkForKopieringAvTrygdetid(it, brukerTokenInfo) }
    }

    private fun behandlingMedTrygdetiderForAvdoede(avdoedeList: List<Folkeregisteridentifikator>): List<UUID> {
        val avdoede = avdoedeList.map { it.value }
        val trygdetiderByBehandlingId: Collection<List<TrygdetidPartial>> =
            trygdetidRepository
                .hentTrygdetiderForAvdoede(avdoede)
                .groupBy(TrygdetidPartial::behandlingId)
                .values

        return trygdetiderByBehandlingId
            .filter { it.trygdetiderGjelderEksaktSammeAvdoede(avdoede) }
            .sortedByDescending { trygdetider -> trygdetider.maxOfOrNull { it.opprettet } }
            .map { it.first().behandlingId }
    }

    private fun behandlingStatusOkForKopieringAvTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        runBlocking {
            behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).status in
                listOf(
                    IVERKSATT,
                    BEREGNET,
                    AVKORTET,
                    FATTET_VEDTAK,
                    ATTESTERT,
                    TIL_SAMORDNING,
                    SAMORDNET,
                )
        }

    private fun sjekkAtTrygdetideneGjelderSammeAvdoede(
        trygdetiderMaal: List<Trygdetid>,
        trygdetiderKilde: List<Trygdetid>,
        behandlingIdMaal: UUID,
        behandlingIdKilde: UUID,
    ) {
        krev(trygdetiderMaal.map { it.ident }.sorted() == trygdetiderKilde.map { it.ident }.sorted()) {
            val feilmelding = "Trygdetidene gjelder forskjellige avdøde"
            logger.error("$feilmelding. Se sikkerlogg for detaljer.")
            sikkerlogger().error(
                """
                $feilmelding ved kopiering av trygdetidsgrunnlag 
                fra $behandlingIdKilde til $behandlingIdMaal
                Mål: ${trygdetiderKilde.joinToString { it.ident }}
                Kilde: ${trygdetiderMaal.joinToString { it.ident }}
                """.trimIndent(),
            )
            feilmelding
        }
    }

    private fun lagredeTrygdetiderHarSammeAvdoede(
        behandlingId: UUID,
        avdoede: List<Folkeregisteridentifikator>,
    ): Boolean {
        val trygdetidIdenter = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).map { it.ident }
        return (
            trygdetidIdenter.size == avdoede.size &&
                trygdetidIdenter.containsAll(avdoede.map { it.value })
        )
    }
}

class ManglerForrigeTrygdetidMaaReguleresManuelt :
    UgyldigForespoerselException(
        "MANGLER_TRYGDETID_FOR_REGULERING",
        "Forrige behandling mangler trygdetid, og kan dermed ikke reguleres manuelt",
    )

class TrygdetidAlleredeOpprettetException :
    IkkeTillattException("TRYGDETID_FINNES_ALLEREDE", "Det er opprettet trygdetid for behandlingen allerede")

class TrygdetidMaaHaFoedselsdatoException(
    behandlingId: UUID,
) : IkkeTillattException(
        "TRYGDETID_MANGLER_FOEDSELSDATO",
        "Kan ikke lage trygdetid uten fødselsdato, behandling: $behandlingId",
    )

class IngenTrygdetidFunnetForAvdoede :
    UgyldigForespoerselException(
        code = "TRYGDETID_IKKE_FUNNET_AVDOEDE",
        detail = "Ingen trygdetider er funnet for den / de avdøde",
    )

class TrygdetidManglerBeregning :
    UgyldigForespoerselException(
        code = "TRYGDETID_MANGLER_BEREGNING",
        detail = "Oppgitt trygdetid er ikke gyldig fordi det mangler en beregning",
    )

class IngenFoedselsdatoForAvdoedFunnet(
    trygdetidId: UUID,
) : UgyldigForespoerselException(
        code = "FOEDSELSDATO_FOR_AVDOED_IKKE_FUNNET",
        detail = "Fant ikke fødselsdato for avdød",
        meta =
            mapOf(
                "trygdetidId" to trygdetidId,
            ),
    )

class IngenDoedsdatoForAvdoedFunnet(
    trygdetidId: UUID,
) : UgyldigForespoerselException(
        code = "FOEDSELSDATO_FOR_AVDOED_IKKE_FUNNET",
        detail = "Fant ikke dødsdato for avdød",
        meta =
            mapOf(
                "trygdetidId" to trygdetidId,
            ),
    )

class TrygdetidIkkeFunnetForBehandling :
    UgyldigForespoerselException(
        code = "TRYGDETID_IKKE_FUNNET_BEHANDLING",
        detail = "Etterspurt trygdetid er ikke funnet for behandlingen",
    )
