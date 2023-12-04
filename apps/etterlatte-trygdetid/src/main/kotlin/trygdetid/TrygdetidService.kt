package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.VilkaarsvuderingKlient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

interface TrygdetidService {
    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid?

    suspend fun opprettTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetidGrunnlag: TrygdetidGrunnlag,
    ): Trygdetid

    suspend fun lagreYrkesskadeTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun slettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    fun overstyrBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid

    suspend fun overstyrNorskPoengaar(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun reberegnUtenFremtidigTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun opprettOverstyrtBeregnetTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

interface GammelTrygdetidServiceMedNy : NyTrygdetidService, TrygdetidService {
    override suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        return hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo).firstOrNull()
    }

    override suspend fun opprettTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        return opprettTrygdetiderForBehandling(behandlingId, brukerTokenInfo).first()
    }

    override suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetidGrunnlag: TrygdetidGrunnlag,
    ): Trygdetid {
        val trygdetid = hentTrygdetidOld(behandlingId) ?: throw GenerellIkkeFunnetException()
        return lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
            behandlingId,
            trygdetid.id,
            trygdetidGrunnlag,
            brukerTokenInfo,
        )
    }

    override suspend fun lagreYrkesskadeTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        return lagreYrkesskadeTrygdetidGrunnlagForBehandling(behandlingId, brukerTokenInfo)
    }

    override suspend fun overstyrNorskPoengaar(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        return overstyrNorskPoengaaarForTrygdetid(trygdetidId, behandlingId, overstyrtNorskPoengaar, brukerTokenInfo)
    }

    override suspend fun slettTrygdetidGrunnlag(
        behandlingId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val trygdetid = hentTrygdetidOld(behandlingId) ?: throw GenerellIkkeFunnetException()
        return slettTrygdetidGrunnlagForTrygdetid(behandlingId, trygdetid.id, trygdetidGrunnlagId, brukerTokenInfo)
    }

    override suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        return kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, brukerTokenInfo).first()
    }

    override fun overstyrBeregnetTrygdetid(
        behandlingId: UUID,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid {
        val trygdetid =
            runBlocking { hentTrygdetidOld(behandlingId) }
                ?: throw GenerellIkkeFunnetException()
        return overstyrBeregnetTrygdetidForAvdoed(behandlingId, trygdetid.ident, beregnetTrygdetid)
    }
}

interface NyTrygdetidService {
    @Deprecated(
        replaceWith = ReplaceWith("hentTrygdetiderIBehandling"),
        message = "Håndterer ikke flere trygdetider i behandling riktig, kun for bruk i overgangsfase",
    )
    suspend fun hentTrygdetidOld(behandlingId: UUID): Trygdetid?

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
    ): List<Trygdetid>

    suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
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

    suspend fun overstyrNorskPoengaaarForTrygdetid(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun lagreYrkesskadeTrygdetidGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun lagreYrkesskadeTrygdetidGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid

    suspend fun kopierSisteTrygdetidberegninger(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>

    fun overstyrBeregnetTrygdetidForAvdoed(
        behandlingId: UUID,
        ident: String,
        beregnetTrygdetid: DetaljertBeregnetTrygdetidResultat,
    ): Trygdetid
}

class TrygdetidServiceImpl(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val vilkaarsvurderingKlient: VilkaarsvuderingKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService,
    private val featureToggleService: FeatureToggleService,
) : GammelTrygdetidServiceMedNy {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val SIST_FREMTIDIG_TRYGDETID_ALDER = 66L
    }

    @Deprecated(
        replaceWith = ReplaceWith("hentTrygdetiderIBehandling"),
        message = "Håndterer ikke flere trygdetider i behandling riktig, kun for bruk i overgangsfase",
    )
    override suspend fun hentTrygdetidOld(behandlingId: UUID): Trygdetid? {
        return trygdetidRepository.hentTrygdetid(behandlingId)
    }

    override suspend fun hentTrygdetiderIBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        return trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)
            .map { trygdetid -> sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, trygdetid) }
    }

    override suspend fun hentTrygdetidIBehandlingMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        return trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
            ?.let { trygdetid -> sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, trygdetid) }
    }

    override suspend fun opprettTrygdetiderForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        return tilstandssjekk(
            behandlingId,
            brukerTokenInfo,
        ) {
            if (trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).isNotEmpty()) {
                throw TrygdetidAlleredeOpprettetException()
            }

            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> {
                    logger.info("Oppretter trygdetid for behandling $behandlingId")
                    opprettTrygdetiderForBehandling(behandling, brukerTokenInfo)
                }

                BehandlingType.REVURDERING -> {
                    logger.info("Oppretter trygdetid for behandling $behandlingId for revurdering")
                    val forrigeBehandling =
                        behandlingKlient.hentSisteIverksatteBehandling(
                            behandling.sak,
                            brukerTokenInfo,
                        )
                    val forrigeTrygdetider = hentTrygdetiderIBehandling(forrigeBehandling.id, brukerTokenInfo)
                    if (forrigeTrygdetider.isEmpty()) {
                        opprettTrygdetiderForRevurdering(behandling, brukerTokenInfo)
                    } else {
                        kopierSisteTrygdetidberegninger(behandling, forrigeTrygdetider)
                    }
                }

                BehandlingType.MANUELT_OPPHOER -> throw StoetterIkkeTrygdetidForBehandlingstypen(behandling.behandlingType)
            }
        }.also { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo) }
    }

    private suspend fun opprettTrygdetiderForRevurdering(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> =
        if (behandling.revurderingsaarsak == Revurderingaarsak.REGULERING &&
            behandling.prosesstype == Prosesstype.AUTOMATISK
        ) {
            logger.info("Forrige trygdetid for ${behandling.id} finnes ikke - må reguleres manuelt")
            throw ManglerForrigeTrygdetidMaaReguleresManuelt()
        } else {
            logger.info("Oppretter trygdetid for behandling ${behandling.id} revurdering")
            opprettTrygdetiderForBehandling(behandling, brukerTokenInfo)
        }

    private suspend fun opprettTrygdetiderForBehandling(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val avdoede = grunnlagKlient.hentGrunnlag(behandling.sak, behandling.id, brukerTokenInfo).hentAvdoede()
        val trygdetider =
            avdoede.map { avdoed ->
                val trygdetid =
                    Trygdetid(
                        sakId = behandling.sak,
                        behandlingId = behandling.id,
                        opplysninger = hentOpplysninger(avdoed),
                        ident =
                            requireNotNull(avdoed.hentFoedselsnummer()?.verdi?.value) {
                                "Kunne ikke hente identifikator for avdød til trygdetid i " +
                                    "behandlingen med id=${behandling.id}"
                            },
                    )
                val opprettetTrygdetid = trygdetidRepository.opprettTrygdetid(trygdetid)

                val oppdatertTrygdetid =
                    opprettFremtidigTrygdetidForAvdoed(opprettetTrygdetid, avdoed, brukerTokenInfo)

                oppdatertTrygdetid ?: opprettetTrygdetid
            }

        return trygdetider
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
                begrunnelse = "Automatisk beregnet fremtidig trygdetid",
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
            avdoed.hentFoedselsdato()?.verdi?.plusYears(
                SIST_FREMTIDIG_TRYGDETID_ALDER,
            )?.with(TemporalAdjusters.firstDayOfNextYear())?.minusDays(1)

        return if (doedsDato != null && sistFremtidigDato != null && doedsDato.isBefore(sistFremtidigDato)) {
            TrygdetidPeriode(doedsDato, sistFremtidigDato)
        } else {
            null
        }
    }

    @Deprecated("Fungerer ikke bra med flere avdøde")
    private suspend fun sjekkYrkesskadeForEndring(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        trygdetid: Trygdetid,
    ): Trygdetid {
        val vurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, brukerTokenInfo)

        if (trygdetid.isYrkesskade() != vurdering.isYrkesskade()) {
            return trygdetid.copy(trygdetidGrunnlag = emptyList()).nullstillBeregnetTrygdetid()
        }

        return trygdetid
    }

    @Deprecated("Fungerer ikke bra med flere avdøde")
    override suspend fun lagreYrkesskadeTrygdetidGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        return tilstandssjekk(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).firstOrNull()
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val sjekketGjeldendeTrygdetid =
                sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, gjeldendeTrygdetid)

            val nyBeregnetTrygdetid =
                beregnTrygdetidService.beregnTrygdetidForYrkesskade(
                    Grunnlagsopplysning.Saksbehandler(
                        brukerTokenInfo.ident(),
                        Tidspunkt.now(),
                    ),
                )

            val nyTrygdetid = sjekketGjeldendeTrygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid).also {
                behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
            }
        }
    }

    override suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: throw GenerellIkkeFunnetException()

            val sjekketGjeldendeTrygdetid =
                sjekkYrkesskadeForEndring(behandlingId, brukerTokenInfo, gjeldendeTrygdetid)

            val trygdetidGrunnlagBeregnet: TrygdetidGrunnlag =
                trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
                    beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag),
                )

            val trygdetidMedOppdatertTrygdetidGrunnlag: Trygdetid =
                sjekketGjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

            oppdaterBeregnetTrygdetid(behandlingId, trygdetidMedOppdatertTrygdetidGrunnlag, brukerTokenInfo)
        }

    private data class DatoerForBehandling(
        val foedselsDato: LocalDate,
        val doedsDato: LocalDate,
    )

    private suspend fun hentDatoerForBehandlingOgAvdoed(
        behandlingId: UUID,
        avdoedIdent: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): DatoerForBehandling {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        val avdoed =
            grunnlagKlient.hentGrunnlag(behandling.sak, behandlingId, brukerTokenInfo).hentAvdoede()
                .find { it.hentFoedselsnummer()?.verdi?.value == avdoedIdent }

        return DatoerForBehandling(
            foedselsDato =
                avdoed?.hentFoedselsdato()?.verdi
                    ?: throw DatoForAvdoedManglerException(behandlingId),
            doedsDato =
                avdoed.hentDoedsdato()?.verdi
                    ?: throw Exception("Fant ikke doedsdato for avdoed for behandlingId=$behandlingId"),
        )
    }

    override suspend fun slettTrygdetidGrunnlagForTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlagId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
                    ?.slettTrygdetidGrunnlag(trygdetidGrunnlagId)
                    ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            oppdaterBeregnetTrygdetid(behandlingId, trygdetid, brukerTokenInfo)
        }

    override suspend fun kopierSisteTrygdetidberegninger(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return kopierSisteTrygdetidberegninger(behandling, forrigeBehandlingId, brukerTokenInfo)
    }

    private suspend fun kopierSisteTrygdetidberegninger(
        behandling: DetaljertBehandling,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        logger.info("Kopierer trygdetid for behandling ${behandling.id} fra behandling $forrigeBehandlingId")

        val forrigeTrygdetid = hentTrygdetiderIBehandling(forrigeBehandlingId, brukerTokenInfo)

        return kopierSisteTrygdetidberegninger(behandling, forrigeTrygdetid)
    }

    private fun kopierSisteTrygdetidberegninger(
        behandling: DetaljertBehandling,
        forrigeTrygdetider: List<Trygdetid>,
    ): List<Trygdetid> {
        // TODO: Ta høyde for nye avdøde her
        logger.info(
            "Kopierer trygdetid for behandling ${behandling.id} fra " +
                "trygdetider med id ${forrigeTrygdetider.joinToString { it.id.toString() }}",
        )

        return forrigeTrygdetider.map { forrigeTrygdetid ->
            val kopiertTrygdetid =
                Trygdetid(
                    sakId = behandling.sak,
                    behandlingId = behandling.id,
                    opplysninger = forrigeTrygdetid.opplysninger.map { it.copy(id = UUID.randomUUID()) },
                    trygdetidGrunnlag = forrigeTrygdetid.trygdetidGrunnlag.map { it.copy(id = UUID.randomUUID()) },
                    beregnetTrygdetid = forrigeTrygdetid.beregnetTrygdetid,
                    ident = forrigeTrygdetid.ident,
                )

            return@map trygdetidRepository.opprettTrygdetid(kopiertTrygdetid)
        }
    }

    private fun kildeFoedselsnummer(): Grunnlagsopplysning.RegelKilde =
        Grunnlagsopplysning.RegelKilde(
            "Beregnet basert på fødselsdato fra pdl",
            Tidspunkt.now(),
            "1",
        )

    private fun hentOpplysninger(avdoed: Grunnlagsdata<JsonNode>): List<Opplysningsgrunnlag> {
        val foedselsdato = avdoed.hentFoedselsdato()
        val opplysninger =
            listOf(
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FOEDSELSDATO,
                    foedselsdato?.kilde,
                    foedselsdato?.verdi,
                ),
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FYLT_16,
                    kildeFoedselsnummer(),
                    // Ifølge paragraf § 3-5 regnes trygdetid fra tidspunkt en person er fylt 16 år
                    foedselsdato?.verdi?.plusYears(16),
                ),
                Opplysningsgrunnlag.ny(
                    TrygdetidOpplysningType.FYLLER_66,
                    kildeFoedselsnummer(),
                    // Ifølge paragraf § 3-5 regnes trygdetid frem til tidspunkt en person er fyller 66 pår
                    foedselsdato?.verdi?.plusYears(SIST_FREMTIDIG_TRYGDETID_ALDER),
                ),
                avdoed.hentDoedsdato().let {
                    Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, it?.kilde, it?.verdi)
                },
            )
        return opplysninger
    }

    private suspend fun <T> tilstandssjekk(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        block: suspend () -> T,
    ): T {
        val kanFastsetteTrygdetid = behandlingKlient.kanOppdatereTrygdetid(behandlingId, brukerTokenInfo)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }

    override suspend fun opprettOverstyrtBeregnetTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (trygdetidRepository.hentTrygdetiderForBehandling(behandlingId).isNotEmpty()) {
            throw TrygdetidAlleredeOpprettetException()
        }
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Oppretter manuell overstyrt trygdetid for behandling $behandlingId")

        val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, behandling.id, brukerTokenInfo).hentAvdoede().firstOrNull()
        val trygdetid =
            Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandling.id,
                opplysninger = avdoed?.let { hentOpplysninger(it) } ?: emptyList(),
                ident =
                    avdoed?.let {
                        requireNotNull(it.hentFoedselsnummer()?.verdi?.value) {
                            "Kunne ikke hente identifikator for avdød til trygdetid i " +
                                "behandlingen med id=${behandling.id}"
                        }
                    } ?: UKJENT_AVDOED,
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
                            ),
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                    ),
            )
        val opprettet = trygdetidRepository.opprettTrygdetid(trygdetid)
        trygdetidRepository.oppdaterTrygdetid(
            opprettet,
            overstyrt = true,
        ) // Holder ikke å sette overstyrt på detaljertBeregnetTrygdetid
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
                        resultat = beregnetTrygdetid,
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                    ),
            )

        return trygdetidRepository.oppdaterTrygdetid(oppdatertTrygdetid, true)
    }

    override suspend fun sjekkGyldighetOgOppdaterBehandlingStatus(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
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

    override suspend fun reberegnUtenFremtidigTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: throw GenerellIkkeFunnetException()

            val trygdetidUtenFremtidigGrunnlag =
                gjeldendeTrygdetid.copy(
                    trygdetidGrunnlag = gjeldendeTrygdetid.trygdetidGrunnlag.filter { it.type != TrygdetidType.FREMTIDIG },
                )

            val datoer = hentDatoerForBehandlingOgAvdoed(behandlingId, gjeldendeTrygdetid.ident, brukerTokenInfo)

            val nyBeregnetTrygdetid =
                beregnTrygdetidService.beregnTrygdetid(
                    trygdetidGrunnlag = trygdetidUtenFremtidigGrunnlag.trygdetidGrunnlag,
                    foedselsDato = datoer.foedselsDato,
                    doedsDato = datoer.doedsDato,
                    norskPoengaar = trygdetidUtenFremtidigGrunnlag.overstyrtNorskPoengaar,
                )

            when (nyBeregnetTrygdetid) {
                null -> trygdetidUtenFremtidigGrunnlag.nullstillBeregnetTrygdetid()
                else -> trygdetidUtenFremtidigGrunnlag.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid)
            }
        }

    override suspend fun overstyrNorskPoengaaarForTrygdetid(
        trygdetidId: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int?,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        tilstandssjekk(behandlingId, brukerTokenInfo) {
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
        val datoer = hentDatoerForBehandlingOgAvdoed(behandlingId, trygdetid.ident, brukerTokenInfo)

        val nyBeregnetTrygdetid =
            beregnTrygdetidService.beregnTrygdetid(
                trygdetidGrunnlag = trygdetid.trygdetidGrunnlag,
                datoer.foedselsDato,
                datoer.doedsDato,
                trygdetid.overstyrtNorskPoengaar,
            )

        return when (nyBeregnetTrygdetid) {
            null -> trygdetid.nullstillBeregnetTrygdetid()
            else -> trygdetid.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
        }.also { nyTrygdetid ->
            trygdetidRepository.oppdaterTrygdetid(nyTrygdetid)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)
        }
    }
}

class DatoForAvdoedManglerException(behandlingId: UUID) :
    InternfeilException("Fant ikke datoer for avdød i behandling med id=$behandlingId")

class ManglerForrigeTrygdetidMaaReguleresManuelt : UgyldigForespoerselException(
    "MANGLER_TRYGDETID_FOR_REGULERING",
    "Forrige behandling mangler trygdetid, og kan dermed ikke reguleres manuelt",
)

class TrygdetidAlleredeOpprettetException :
    IkkeTillattException("TRYGDETID_FINNES_ALLEREDE", "Det er opprettet trygdetid for behandlingen allerede")

class StoetterIkkeTrygdetidForBehandlingstypen(behandlingType: BehandlingType) :
    UgyldigForespoerselException(
        code = "STOETTER_IKKE_BEHANDLINGTYPEN",
        detail = "Støtter ikke trygdetid for behandlingstypen $behandlingType",
    )

class IngenTrygdetidFunnetForAvdoede : UgyldigForespoerselException(
    code = "TRYGDETID_IKKE_FUNNET_AVDOEDE",
    detail = "Ingen trygdetider er funnet for den / de avdøde",
)

class TrygdetidManglerBeregning : UgyldigForespoerselException(
    code = "TRYGDETID_MANGLER_BEREGNING",
    detail = "Oppgitt trygdetid er ikke gyldig fordi det mangler en beregning",
)
