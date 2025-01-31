package no.nav.etterlatte.beregning.grunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.Companion.automatiskSaksbehandler
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val REFORM_TIDSPUNKT_BP = YearMonth.of(2024, 1)

class ManglerVirkningstidspunktBP :
    UgyldigForespoerselException(
        code = "MANGLER_VIRK_BP",
        detail = "Mangler virkningstidspunkt for barnepensjon.",
    )

class ManglerForrigeGrunnlag :
    UgyldigForespoerselException(
        code = "MANGLER_FORRIGE_GRUNNLAG",
        detail = "Mangler forrige grunnlag for revurdering",
    )

class UgyldigBeregningsgrunnlag :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGSGRUNNLAG",
        detail = "Beregningsgrunnlaget er ikke gyldig",
    )

class BeregningsGrunnlagService(
    private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(BeregningsGrunnlagService::class.java)

    suspend fun lagreBeregningsGrunnlag(
        behandlingId: UUID,
        beregningsGrunnlag: LagreBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? =
        when {
            behandlingKlient.kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo) -> {
                logger.info("Lagrer beregningsgrunnlag for behandling $behandlingId")

                val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                val grunnlag = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo)

                if (behandling.sakType == SakType.BARNEPENSJON) {
                    validerSoeskenMedIBeregning(
                        behandlingId,
                        beregningsGrunnlag,
                        grunnlag,
                    )
                }
                val kanLagreDetteGrunnlaget =
                    if (behandling.behandlingType == BehandlingType.REVURDERING) {
                        // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                        val sisteIverksatteBehandling =
                            vedtaksvurderingKlient
                                .hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
                                .sortedByDescending { it.datoFattet }
                                .first { it.vedtakType != VedtakType.OPPHOER }

                        logger.info("Siste iverksatte behandling er ${sisteIverksatteBehandling.behandlingId}")

                        grunnlagErIkkeEndretFoerVirk(
                            behandling,
                            sisteIverksatteBehandling.behandlingId,
                            beregningsGrunnlag,
                        )
                    } else {
                        true
                    }

                val soeskenMedIBeregning =
                    if (behandling.sakType == SakType.BARNEPENSJON) {
                        beregningsGrunnlag.soeskenMedIBeregning.ifEmpty {
                            when (val virk = behandling.virkningstidspunkt) {
                                null -> throw ManglerVirkningstidspunktBP()
                                else -> {
                                    if (virk.dato.isBefore(REFORM_TIDSPUNKT_BP)) {
                                        // Her burde man egentlig ha en sjekk på om avdøde har noen barn.
                                        // Hvis avdøde har barn og vi er før reformtidspunkt må disse barnene søskenjusteres
                                        listOf(
                                            GrunnlagMedPeriode(
                                                data = emptyList(),
                                                fom = virk.dato.atDay(1),
                                                tom = null,
                                            ),
                                        )
                                    } else {
                                        emptyList()
                                    }
                                }
                            }
                        }
                    } else {
                        emptyList()
                    }

                if (kanLagreDetteGrunnlaget) {
                    logger.info("Beregningsgrunnlaget er gyldig for behandling $behandlingId - lagrer")
                    beregningsGrunnlagRepository.lagreBeregningsGrunnlag(
                        BeregningsGrunnlag(
                            behandlingId = behandlingId,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                            soeskenMedIBeregning = soeskenMedIBeregning,
                            institusjonsopphold =
                                beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                            beregningsMetode = beregningsGrunnlag.beregningsMetode,
                            beregningsMetodeFlereAvdoede =
                                beregningsGrunnlag.beregningsMetodeFlereAvdoede
                                    ?: emptyList(),
                            kunEnJuridiskForelder = beregningsGrunnlag.kunEnJuridiskForelder,
                        ),
                    )

                    behandlingKlient.statusTrygdetidOppdatert(behandlingId, brukerTokenInfo, commit = true)
                    beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId)
                } else {
                    logger.info("Beregningsgrunnlaget er ikke gyldig for behandling $behandlingId")
                    sikkerlogger().info(
                        "Beregningsgrunnlaget er ikke gyldig for behandling $behandlingId. Beregningsgrunnlag: ${beregningsGrunnlag.toJson()}",
                    )
                    throw UgyldigBeregningsgrunnlag()
                }
            }

            else -> null
        }

    private fun validerSoeskenMedIBeregning(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: LagreBeregningsGrunnlag,
        grunnlag: Grunnlag,
    ) {
        val soeskensFoedselsnummere =
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning
                .flatMap { soeskenGrunnlag -> soeskenGrunnlag.data }
                .map { it.foedselsnummer.value }

        if (soeskensFoedselsnummere.isNotEmpty()) {
            val avdoed = grunnlag.hentAvdoede().first().hentAvdoedesbarn()!!
            val avdoedesBarn = avdoed.verdi.avdoedesBarn!!.associateBy({ it.foedselsnummer.value }, { it })

            val alleSoeskenFinnes = soeskensFoedselsnummere.all { fnr -> avdoedesBarn.contains(fnr) }
            if (!alleSoeskenFinnes) {
                throw BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(behandlingId)
            }

            val alleSoeskenIberegningenErlevende =
                soeskensFoedselsnummere.all { fnr -> avdoedesBarn[fnr]?.doedsdato === null }
            if (!alleSoeskenIberegningenErlevende) {
                throw BPBeregningsgrunnlagSoeskenMarkertDoedException(behandlingId)
            }
        }
    }

    private fun grunnlagErIkkeEndretFoerVirk(
        revurdering: DetaljertBehandling,
        forrigeIverksatteBehandlingId: UUID,
        beregningsGrunnlag: LagreBeregningsGrunnlag,
    ): Boolean {
        logger.info("Kontrollerer at beregningsgrunnlag ikke er endret før virkningstidspunkt")
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(
                forrigeIverksatteBehandlingId,
            )

        // hvis forrigeGrunnlag er null, kan aarsaken være at tidligere behandling er manuelt overstyrt
        if (forrigeGrunnlag == null) {
            val overstyrtBeregningsGrunnlag =
                beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(
                    forrigeIverksatteBehandlingId,
                )
            if (overstyrtBeregningsGrunnlag.isNotEmpty()) {
                // i tilfelle hvor tidligere behandling er manuelt overstyrt  (mangler beregningsgrunnlag)
                // kan vi returnere True ettersom vi ikke har noe å sammenligne med
                return true
            } else {
                // i tilfelle hvor tidligere behandling ikke er overstyrt beregnet
                throw ManglerForrigeGrunnlag()
            }
        }

        val revurderingVirk = revurdering.virkningstidspunkt().dato.atDay(1)
        val soeskenjusteringErLiktFoerVirk =
            if (revurdering.sakType == SakType.BARNEPENSJON) {
                erGrunnlagLiktFoerEnDato(
                    beregningsGrunnlag.soeskenMedIBeregning,
                    forrigeGrunnlag.soeskenMedIBeregning,
                    revurderingVirk,
                )
            } else {
                true
            }

        val institusjonsoppholdErLiktFoerVirk =
            erGrunnlagLiktFoerEnDato(
                beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                forrigeGrunnlag.institusjonsopphold,
                revurderingVirk,
            )

        return soeskenjusteringErLiktFoerVirk && institusjonsoppholdErLiktFoerVirk
    }

    suspend fun hentBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId)
        if (grunnlag != null) {
            return grunnlag
        }

        val forrigeIverksatte = forrigeIverksatteBehandling(behandlingId, brukerTokenInfo)
        // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
        return if (forrigeIverksatte != null) {
            beregningsGrunnlagRepository
                .finnBeregningsGrunnlag(forrigeIverksatte.behandlingId)
                ?.also {
                    logger.info(
                        "Ga ut forrige beregningsgrunnlag for $behandlingId, funnet i " +
                            "${forrigeIverksatte.id}. Dette grunnlaget er kopiert inn til $behandlingId.",
                    )
                    beregningsGrunnlagRepository.lagreBeregningsGrunnlag(it.copy(behandlingId = behandlingId))
                }
        } else {
            null
        }
    }

    fun dupliserBeregningsGrunnlag(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Dupliser grunnlag for $behandlingId fra $forrigeBehandlingId")

        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(forrigeBehandlingId)
        if (forrigeGrunnlag == null) {
            val behandling =
                runBlocking {
                    behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                }
            if (beregningRepository.hentOverstyrBeregning(behandling.sak) != null) {
                dupliserOverstyrBeregningGrunnlag(behandlingId, forrigeBehandlingId)
                return
            } else {
                throw RuntimeException("Ingen grunnlag funnet for $forrigeBehandlingId")
            }
        }

        if (beregningsGrunnlagRepository.finnBeregningsGrunnlag(behandlingId) != null) {
            throw RuntimeException("Eksisterende grunnlag funnet for $behandlingId")
        }

        beregningsGrunnlagRepository.lagreBeregningsGrunnlag(forrigeGrunnlag.copy(behandlingId = behandlingId))

        dupliserOverstyrBeregningGrunnlag(behandlingId, forrigeBehandlingId)
    }

    private fun dupliserOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ) {
        beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(forrigeBehandlingId).let { grunnlag ->
            if (grunnlag.isNotEmpty()) {
                beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                    behandlingId,
                    grunnlag.map {
                        it.copy(
                            id = UUID.randomUUID(),
                            behandlingId = behandlingId,
                        )
                    },
                )
            }
        }
    }

    suspend fun hentOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningGrunnlag {
        logger.info("Henter overstyr beregning grunnlag $behandlingId")

        val overstyrtePerioder =
            beregningsGrunnlagRepository
                .finnOverstyrBeregningGrunnlagForBehandling(
                    behandlingId,
                ).let { overstyrBeregningGrunnlagDaoListe ->
                    OverstyrBeregningGrunnlag(
                        perioder =
                            overstyrBeregningGrunnlagDaoListe.map(OverstyrBeregningGrunnlagDao::tilGrunnlagMedPeriode),
                        kilde = overstyrBeregningGrunnlagDaoListe.firstOrNull()?.kilde ?: automatiskSaksbehandler,
                    )
                }
        return if (overstyrtePerioder.perioder.isEmpty()) {
            // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
            val forrigeIverksatte = forrigeIverksatteBehandling(behandlingId, brukerTokenInfo)
            if (forrigeIverksatte != null) {
                logger.info(
                    "Gir ut det forrige overstyrte beregningsgrunnlaget i behandling ${forrigeIverksatte.behandlingId} for " +
                        "nåværende behandling under arbeid $behandlingId",
                )
                val overstyrtePerioderForrigeBehandling =
                    beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(forrigeIverksatte.behandlingId)
                OverstyrBeregningGrunnlag(
                    perioder = overstyrtePerioderForrigeBehandling.map(OverstyrBeregningGrunnlagDao::tilGrunnlagMedPeriode),
                    kilde = overstyrtePerioderForrigeBehandling.firstOrNull()?.kilde ?: automatiskSaksbehandler,
                ).also {
                    // Lagre ned det grunnlaget vi gir ut fra forrige iverksatte også på behandlingen vi er i
                    logger.info(
                        "Kopierte overstyrt beregningsgrunnlag fra ${forrigeIverksatte.behandlingId} til " +
                            "$behandlingId, med ${overstyrtePerioderForrigeBehandling.size} perioder.",
                    )
                    if (overstyrtePerioderForrigeBehandling.isNotEmpty()) {
                        beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                            behandlingId,
                            overstyrtePerioderForrigeBehandling.map { it.copy(id = UUID.randomUUID()) },
                        )
                    }
                }
            } else {
                overstyrtePerioder
            }
        } else {
            overstyrtePerioder
        }
    }

    private suspend fun forrigeIverksatteBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakSammendragDto? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return if (behandling.behandlingType == BehandlingType.REVURDERING) {
            vedtaksvurderingKlient
                .hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
                .sortedByDescending { it.datoFattet }
                .first { it.vedtakType != VedtakType.OPPHOER } // Opphør har ikke beregningsgrunnlag
        } else {
            null
        }
    }

    suspend fun lagreOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        data: OverstyrBeregningGrunnlagDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningGrunnlag {
        logger.info("Lagre overstyr beregning grunnlag $behandlingId")

        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        if (behandlingKlient.kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                behandlingId,
                data.perioder.map {
                    val gyldigTrygdetid = it.data.trygdetid in 0..40
                    if (!gyldigTrygdetid) {
                        throw OverstyrtBeregningUgyldigTrygdetid(behandlingId)
                    }

                    // TODO her burde det sikkert være mer validering av hva saksbehandler kan sende inn

                    OverstyrBeregningGrunnlagDao(
                        id = UUID.randomUUID(),
                        behandlingId = behandlingId,
                        datoFOM = it.fom,
                        datoTOM = it.tom,
                        utbetaltBeloep = it.data.utbetaltBeloep,
                        foreldreloessats = it.data.foreldreloessats,
                        trygdetid = it.data.trygdetid,
                        trygdetidForIdent = it.data.trygdetidForIdent,
                        prorataBroekTeller = it.data.prorataBroekTeller,
                        prorataBroekNevner = it.data.prorataBroekNevner,
                        sakId = behandling.sak,
                        beskrivelse = it.data.beskrivelse,
                        aarsak = it.data.aarsak,
                        kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                    )
                },
            )
            behandlingKlient.statusTrygdetidOppdatert(behandlingId, brukerTokenInfo, commit = true)
            return hentOverstyrBeregningGrunnlag(behandlingId, brukerTokenInfo)
        } else {
            throw OverstyrtBeregningFeilBehandlingStatusException(behandlingId, behandling.status)
        }
    }

    fun tilpassOverstyrtBeregningsgrunnlagForRegulering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId).let { grunnlag ->
            if (grunnlag.isNotEmpty()) {
                val behandling =
                    runBlocking {
                        behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                    }

                val reguleringsmaaned = LocalDate.of(behandling.virkningstidspunkt().dato.year, 5, 1)

                val nyePerioder = mutableListOf<OverstyrBeregningGrunnlagDao>()
                grunnlag.forEach {
                    val erFoerRegulering = it.datoTOM != null && it.datoTOM!! < reguleringsmaaned
                    val erOverRegulering =
                        it.datoFOM < reguleringsmaaned && (it.datoTOM == null || it.datoTOM!! > reguleringsmaaned)

                    if (erFoerRegulering) {
                        nyePerioder.add(it)
                    } else if (erOverRegulering) {
                        val forrigeMaaned = reguleringsmaaned.minusMonths(1)
                        val eksisterende =
                            it.copy(
                                datoTOM =
                                    LocalDate.of(
                                        reguleringsmaaned.year,
                                        forrigeMaaned.month,
                                        forrigeMaaned.lengthOfMonth(),
                                    ),
                            )
                        val nyPeriode =
                            tilpassOverstyrtBeregningsgrunnlagForRegulering(
                                YearMonth.from(reguleringsmaaned),
                                fom = reguleringsmaaned,
                                it,
                                behandlingId,
                            )
                        nyePerioder.add(eksisterende)
                        nyePerioder.add(nyPeriode)
                    } else {
                        nyePerioder.add(
                            tilpassOverstyrtBeregningsgrunnlagForRegulering(
                                YearMonth.from(reguleringsmaaned),
                                fom = it.datoFOM,
                                it,
                                behandlingId,
                            ),
                        )
                    }
                }

                beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
                    behandlingId,
                    nyePerioder,
                )
            }
        }
    }

    fun sjekkOmOverstyrtGrunnlagErLiktFoerVirk(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val forrigeIverksatteBehandling = runBlocking { forrigeIverksatteBehandling(behandlingId, brukerTokenInfo) }
        if (forrigeIverksatteBehandling != null) {
            // har vi overstyrt beregning for denne behandlingen?
            val forrigeOverstyrBeregningGrunnlagPerioder =
                runBlocking { hentOverstyrBeregningGrunnlag(forrigeIverksatteBehandling.behandlingId, brukerTokenInfo) }
                    .perioder
                    .mapVerdier(OverstyrBeregningGrunnlagData::tilSammenligningsperiode)
            if (forrigeOverstyrBeregningGrunnlagPerioder.isEmpty()) {
                return
            }
            val naavaerendeGrunnlagPerioder =
                runBlocking { hentOverstyrBeregningGrunnlag(behandlingId, brukerTokenInfo) }
                    .perioder
                    .mapVerdier(OverstyrBeregningGrunnlagData::tilSammenligningsperiode)
            if (!erGrunnlagLiktFoerEnDato(
                    naavaerendeGrunnlagPerioder.sortedBy { it.fom },
                    forrigeOverstyrBeregningGrunnlagPerioder.sortedBy { it.fom },
                    virkningstidspunkt.atDay(1),
                )
            ) {
                throw OverstyrtBeregningsgrunnlagEndresFoerVirkException(
                    behandlingId = behandlingId,
                    forrigeBehandlingId = forrigeIverksatteBehandling.behandlingId,
                )
            }
        }
    }
}

class OverstyrtBeregningUgyldigTrygdetid(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_BEREGNING_UGYLDIG_TRYGDETID",
        detail = "Anvendt trygdetid må være mellom 0 og 40 år",
        meta = mapOf("behandlingId" to behandlingId),
    )

class OverstyrtBeregningFeilBehandlingStatusException(
    behandlingId: UUID,
    behandlingStatus: BehandlingStatus,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_BEREGNING_FEIL_BEHANDLINGSSTATUS",
        detail = "Kunne ikke lagre overstyrt beregningsgrunnlag fordi behandlingen er i feil status",
        meta = mapOf("behandlingId" to behandlingId, "behandlingStatus" to behandlingStatus),
    )

class BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_SOESKEN_IKKE_AVDOEDES_BARN",
        detail = "Barnepensjon beregningsgrunnlag har søsken fnr som ikke er avdødeds barn",
        meta = mapOf("behandlingId" to behandlingId),
    )

class BPBeregningsgrunnlagSoeskenMarkertDoedException(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BP_BEREGNING_SOESKEN_MARKERT_DOED",
        detail = "Barnpensjon beregningsgrunnlag bruker søsken som er døde i beregningen",
        meta = mapOf("behandlingId" to behandlingId),
    )

class OverstyrtBeregningsgrunnlagEndresFoerVirkException(
    behandlingId: UUID,
    forrigeBehandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OVERSTYRT_GRUNNLAG_ENDRET_FOER_VIRK",
        detail =
            "De overstyrte beregningsperiodene er forskjellige fra forrige " +
                "vedtak (id=$forrigeBehandlingId) før virkningstidpunktet i denne behandlingen (id=$behandlingId). " +
                "Endringer skal kun skje etter virkningstidspunktet.",
    )
