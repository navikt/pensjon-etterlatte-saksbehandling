package no.nav.etterlatte.beregning.grunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.beregning.BeregningRepository
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.Companion.automatiskSaksbehandler
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

val REFORM_TIDSPUNKT_BP = YearMonth.of(2024, 1)

class ManglerVirkningstidspunktBP : UgyldigForespoerselException(
    code = "MANGLER_VIRK_BP",
    detail = "Mangler virkningstidspunkt for barnepensjon.",
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
    ): Boolean =
        when {
            behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, false) -> {
                val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                if (behandling.sakType == SakType.BARNEPENSJON) {
                    validerSoeskenMedIBeregning(behandlingId, beregningsGrunnlag, brukerTokenInfo)
                }
                val kanLagreDetteGrunnlaget =
                    if (behandling.behandlingType == BehandlingType.REVURDERING) {
                        // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                        val sisteIverksatteBehandling =
                            vedtaksvurderingKlient.hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
                                .sortedByDescending { it.datoFattet }
                                .first { it.vedtakType != VedtakType.OPPHOER }

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

                kanLagreDetteGrunnlaget &&
                    beregningsGrunnlagRepository.lagreBeregningsGrunnlag(
                        BeregningsGrunnlag(
                            behandlingId = behandlingId,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                            soeskenMedIBeregning = soeskenMedIBeregning,
                            institusjonsoppholdBeregningsgrunnlag =
                                beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                            beregningsMetode = beregningsGrunnlag.beregningsMetode,
                        ),
                    )
            }

            else -> false
        }

    private suspend fun validerSoeskenMedIBeregning(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: LagreBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        // TODO Skal vekk..
        val grunnlag = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo)
        if (grunnlag.hentAvdoede().size > 1) {
            throw BPBeregningsgrunnlagMerEnnEnAvdoedException(behandlingId)
        }

        val soeskensFoedselsnummere =
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning.flatMap { soeskenGrunnlag -> soeskenGrunnlag.data }
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
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(
                forrigeIverksatteBehandlingId,
            )
        val revurderingVirk = revurdering.virkningstidspunkt().dato.atDay(1)

        val soeskenjusteringErLiktFoerVirk =
            if (revurdering.sakType == SakType.BARNEPENSJON) {
                erGrunnlagLiktFoerEnDato(
                    beregningsGrunnlag.soeskenMedIBeregning,
                    forrigeGrunnlag!!.soeskenMedIBeregning,
                    revurderingVirk,
                )
            } else {
                true
            }

        val institusjonsoppholdErLiktFoerVirk =
            erGrunnlagLiktFoerEnDato(
                beregningsGrunnlag.institusjonsopphold ?: emptyList(),
                forrigeGrunnlag!!.institusjonsoppholdBeregningsgrunnlag,
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

        // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return if (behandling.behandlingType == BehandlingType.REVURDERING) {
            val sisteIverksatteBehandling =
                behandlingKlient.hentSisteIverksatteBehandling(
                    behandling.sak,
                    brukerTokenInfo,
                )
            beregningsGrunnlagRepository.finnBeregningsGrunnlag(sisteIverksatteBehandling.id)
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

    fun hentOverstyrBeregningGrunnlag(behandlingId: UUID): OverstyrBeregningGrunnlag {
        logger.info("Henter overstyr beregning grunnlag $behandlingId")

        return beregningsGrunnlagRepository.finnOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
        ).let { overstyrBeregningGrunnlagDaoListe ->
            OverstyrBeregningGrunnlag(
                perioder =
                    overstyrBeregningGrunnlagDaoListe.map { periode ->
                        GrunnlagMedPeriode(
                            data =
                                OverstyrBeregningGrunnlagData(
                                    utbetaltBeloep = periode.utbetaltBeloep,
                                    trygdetid = periode.trygdetid,
                                    trygdetidForIdent = periode.trygdetidForIdent,
                                    prorataBroekTeller = periode.prorataBroekTeller,
                                    prorataBroekNevner = periode.prorataBroekNevner,
                                    beskrivelse = periode.beskrivelse,
                                ),
                            fom = periode.datoFOM,
                            tom = periode.datoTOM,
                        )
                    },
                kilde = overstyrBeregningGrunnlagDaoListe.firstOrNull()?.kilde ?: automatiskSaksbehandler,
            )
        }
    }

    suspend fun lagreOverstyrBeregningGrunnlag(
        behandlingId: UUID,
        data: OverstyrBeregningGrunnlagDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregningGrunnlag {
        logger.info("Lagre overstyr beregning grunnlag $behandlingId")

        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        beregningsGrunnlagRepository.lagreOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
            data.perioder.map {
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = it.fom,
                    datoTOM = it.tom,
                    utbetaltBeloep = it.data.utbetaltBeloep,
                    trygdetid = it.data.trygdetid,
                    trygdetidForIdent = it.data.trygdetidForIdent,
                    prorataBroekTeller = it.data.prorataBroekTeller,
                    prorataBroekNevner = it.data.prorataBroekNevner,
                    sakId = behandling.sak,
                    beskrivelse = it.data.beskrivelse,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                )
            },
        )

        return hentOverstyrBeregningGrunnlag(behandlingId)
    }
}

class BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(behandlingId: UUID) : UgyldigForespoerselException(
    code = "BP_BEREGNING_SOESKEN_IKKE_AVDOEDES_BARN",
    detail = "Barnepensjon beregningsgrunnlag har søsken fnr som ikke er avdødeds barn",
    meta = mapOf("behandlingId" to behandlingId),
)

class BPBeregningsgrunnlagSoeskenMarkertDoedException(behandlingId: UUID) : UgyldigForespoerselException(
    code = "BP_BEREGNING_SOESKEN_MARKERT_DOED",
    detail = "Barnpensjon beregningsgrunnlag bruker søsken som er døde i beregningen",
    meta = mapOf("behandlingId" to behandlingId),
)

class BPBeregningsgrunnlagMerEnnEnAvdoedException(behandlingId: UUID) : UgyldigForespoerselException(
    code = "BP_BEREGNING_MER_ENN_EN_AVDOED",
    detail = "Kan maks ha en avdød",
    meta = mapOf("behandlingId" to behandlingId),
)
