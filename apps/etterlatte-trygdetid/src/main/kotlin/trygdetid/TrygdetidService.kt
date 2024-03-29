package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
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

    suspend fun setYrkesskade(
        trygdetidId: UUID,
        behandlingId: UUID,
        yrkesskade: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
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

    suspend fun sjekkTrygdetidMotGrunnlag(
        trygdetid: Trygdetid,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid?
}

interface GammelTrygdetidServiceMedNy : NyTrygdetidService, TrygdetidService {
    override suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        return hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo).minByOrNull { it.ident }
    }

    override suspend fun opprettTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid {
        val trygdetid = opprettTrygdetiderForBehandling(behandlingId, brukerTokenInfo).minByOrNull { it.ident }

        return checkNotNull(trygdetid) {
            "Kunne ikke opprette trygdetid for behandling=$behandlingId"
        }
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
        return kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, brukerTokenInfo).minBy { it.ident }
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

    suspend fun setYrkesskade(
        trygdetidId: UUID,
        behandlingId: UUID,
        yrkesskade: Boolean,
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

    suspend fun oppdaterOpplysningsgrunnlagForTrygdetider(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid>
}

class TrygdetidServiceImpl(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService,
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
            .mapNotNull { trygdetid -> sjekkTrygdetidMotGrunnlag(trygdetid, brukerTokenInfo) }
    }

    override suspend fun hentTrygdetidIBehandlingMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        return trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
            ?.let { trygdetid -> sjekkTrygdetidMotGrunnlag(trygdetid, brukerTokenInfo) }
    }

    override suspend fun opprettTrygdetiderForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        return kanOppdatereTrygdetid(
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
        val avdoede = grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo).hentAvdoede()

        if (avdoede.isNullOrEmpty()) {
            logger.warn("Kan ikke opprette trygdetid når det mangler avdøde (behandling=${behandling.id})")
            throw GrunnlagManglerAvdoede()
        }

        val trygdetider =
            avdoede.map { avdoed ->
                val trygdetid =
                    Trygdetid(
                        sakId = behandling.sak,
                        behandlingId = behandling.id,
                        opplysninger = hentOpplysninger(avdoed, behandling.id),
                        ident =
                            requireNotNull(avdoed.hentFoedselsnummer()?.verdi?.value) {
                                "Kunne ikke hente identifikator for avdød til trygdetid i " +
                                    "behandlingen med id=${behandling.id}"
                            },
                        yrkesskade = false,
                    )
                val opprettetTrygdetid = trygdetidRepository.opprettTrygdetid(trygdetid)

                val oppdatertTrygdetid =
                    opprettFremtidigTrygdetidForAvdoed(opprettetTrygdetid, avdoed, brukerTokenInfo)

                oppdatertTrygdetid ?: opprettetTrygdetid
            }

        logger.info("Opprettet ${trygdetider.size} trygdetider for behandling=${behandling.id}")

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

    override suspend fun lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
        behandlingId: UUID,
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: throw GenerellIkkeFunnetException()

            val trygdetidGrunnlagBeregnet: TrygdetidGrunnlag =
                trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
                    beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag),
                )

            val trygdetidMedOppdatertTrygdetidGrunnlag: Trygdetid =
                gjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

            oppdaterBeregnetTrygdetid(behandlingId, trygdetidMedOppdatertTrygdetidGrunnlag, brukerTokenInfo)
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
                    yrkesskade = forrigeTrygdetid.yrkesskade,
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
            logger.error("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")

            throw UgyldigForespoerselException(
                code = "UGYLDIG_TILSTAND_TRYGDETID",
                detail = "Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand",
            )
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

        val avdoed =
            grunnlagKlient.hentGrunnlag(behandling.id, brukerTokenInfo)
                .hentAvdoede().minByOrNull { it.hentDoedsdato()?.verdi ?: LocalDate.MAX }
        // TODO: Det er mest sannsynlig den med tidligst dødsdato som er aktuell, men dette er midlertidig løsning
        val trygdetid =
            Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandlingId,
                opplysninger = avdoed?.let { hentOpplysninger(it, behandlingId) } ?: emptyList(),
                ident =
                    avdoed?.let {
                        requireNotNull(it.hentFoedselsnummer()?.verdi?.value) {
                            "Kunne ikke hente identifikator for avdød til trygdetid i " +
                                "behandlingen med id=$behandlingId"
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
                                yrkesskade = false,
                                beregnetSamletTrygdetidNorge = null,
                            ),
                        tidspunkt = Tidspunkt.now(),
                        regelResultat = "".toJsonNode(),
                    ),
                yrkesskade = false,
            )
        val opprettet = trygdetidRepository.opprettTrygdetid(trygdetid)
        trygdetidRepository.oppdaterTrygdetid(
            opprettet,
            overstyrt = true,
        ) // Holder ikke å sette overstyrt på detaljertBeregnetTrygdetid
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

    override suspend fun oppdaterOpplysningsgrunnlagForTrygdetider(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val trygdetidList = trygdetidRepository.hentTrygdetiderForBehandling(behandlingId)
        if (trygdetidList.isEmpty()) {
            throw IngenTrygdetidFunnetForAvdoede()
        }
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Oppretter manuell overstyrt trygdetid for behandling $behandlingId")

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

    override suspend fun reberegnUtenFremtidigTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid =
        kanOppdatereTrygdetid(behandlingId, brukerTokenInfo) {
            val gjeldendeTrygdetid: Trygdetid =
                trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId) ?: throw GenerellIkkeFunnetException()

            val trygdetidUtenFremtidigGrunnlag =
                gjeldendeTrygdetid.copy(
                    trygdetidGrunnlag = gjeldendeTrygdetid.trygdetidGrunnlag.filter { it.type != TrygdetidType.FREMTIDIG },
                )

            val datoer = hentDatoerForBehandling(gjeldendeTrygdetid)

            val nyBeregnetTrygdetid =
                beregnTrygdetidService.beregnTrygdetid(
                    trygdetidGrunnlag = trygdetidUtenFremtidigGrunnlag.trygdetidGrunnlag,
                    foedselsDato = datoer.foedselsDato,
                    doedsDato = datoer.doedsDato,
                    norskPoengaar = trygdetidUtenFremtidigGrunnlag.overstyrtNorskPoengaar,
                    yrkesskade = trygdetidUtenFremtidigGrunnlag.yrkesskade,
                )

            when (nyBeregnetTrygdetid) {
                null -> trygdetidUtenFremtidigGrunnlag.nullstillBeregnetTrygdetid()
                else -> trygdetidUtenFremtidigGrunnlag.oppdaterBeregnetTrygdetid(nyBeregnetTrygdetid)
            }.also { nyTrygdetid ->
                trygdetidRepository.oppdaterTrygdetid(nyTrygdetid)
            }
        }

    override suspend fun sjekkTrygdetidMotGrunnlag(
        trygdetid: Trygdetid,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        if (trygdetid.ident == UKJENT_AVDOED) {
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
            trygdetid.beregnetTrygdetid?.resultat?.overstyrt.let { it != null && it }
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

        val nyBeregnetTrygdetid =
            beregnTrygdetidService.beregnTrygdetid(
                trygdetidGrunnlag = trygdetid.trygdetidGrunnlag,
                datoer.foedselsDato,
                datoer.doedsDato,
                trygdetid.overstyrtNorskPoengaar,
                trygdetid.yrkesskade,
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

class ManglerForrigeTrygdetidMaaReguleresManuelt : UgyldigForespoerselException(
    "MANGLER_TRYGDETID_FOR_REGULERING",
    "Forrige behandling mangler trygdetid, og kan dermed ikke reguleres manuelt",
)

class GrunnlagManglerAvdoede : UgyldigForespoerselException(
    code = "GRUNNLAG_MANGLER_AVDOEDE",
    detail = "Ingen avdød(e) funnet i grunnlag. Kan ikke opprette trygdetid.",
)

class TrygdetidAlleredeOpprettetException :
    IkkeTillattException("TRYGDETID_FINNES_ALLEREDE", "Det er opprettet trygdetid for behandlingen allerede")

class TrygdetidMaaHaFoedselsdatoException(behandlingId: UUID) :
    IkkeTillattException(
        "TRYGDETID_MANGLER_FOEDSELSDATO",
        "Kan ikke lage trygdetid uten fødselsdato, behandling: $behandlingId",
    )

class IngenTrygdetidFunnetForAvdoede : UgyldigForespoerselException(
    code = "TRYGDETID_IKKE_FUNNET_AVDOEDE",
    detail = "Ingen trygdetider er funnet for den / de avdøde",
)

class TrygdetidManglerBeregning : UgyldigForespoerselException(
    code = "TRYGDETID_MANGLER_BEREGNING",
    detail = "Oppgitt trygdetid er ikke gyldig fordi det mangler en beregning",
)

class IngenFoedselsdatoForAvdoedFunnet(trygdetidId: UUID) : UgyldigForespoerselException(
    code = "FOEDSELSDATO_FOR_AVDOED_IKKE_FUNNET",
    detail = "Fant ikke fødselsdato for avdød",
    meta =
        mapOf(
            "trygdetidId" to trygdetidId,
        ),
)

class IngenDoedsdatoForAvdoedFunnet(trygdetidId: UUID) : UgyldigForespoerselException(
    code = "FOEDSELSDATO_FOR_AVDOED_IKKE_FUNNET",
    detail = "Fant ikke dødsdato for avdød",
    meta =
        mapOf(
            "trygdetidId" to trygdetidId,
        ),
)

class TrygdetidIkkeFunnetForBehandling : UgyldigForespoerselException(
    code = "TRYGDETID_IKKE_FUNNET_BEHANDLING",
    detail = "Etterspurt trygdetid er ikke funnet for behandlingen",
)
