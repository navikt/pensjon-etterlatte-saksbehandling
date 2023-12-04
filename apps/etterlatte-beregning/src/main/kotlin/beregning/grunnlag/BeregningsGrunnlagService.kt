package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.Companion.automatiskSaksbehandler
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(msg: String) : Exception(msg)

class BPBeregningsgrunnlagSoeskenMarkertDoedException(msg: String) : Exception(msg)

class BPBeregningsgrunnlagMerEnnEnAvdoedException(msg: String) : Exception(msg)

class BeregningsGrunnlagService(
    private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    private val behandlingKlient: BehandlingKlient,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(BeregningsGrunnlagService::class.java)

    private suspend fun validerBeregningsgrunnlagBarnepensjon(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val grunnlag = grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo)
        if (grunnlag.hentAvdoede().size > 1) {
            throw BPBeregningsgrunnlagMerEnnEnAvdoedException("Kan maks ha en avdød, behandlingid: $behandlingId")
        }

        val soeskensFoedselsnummere =
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning.flatMap { grunnlag -> grunnlag.data }
                .map { it.foedselsnummer.value }

        if (soeskensFoedselsnummere.isNotEmpty()) {
            val avdoed = grunnlag.hentAvdoede().first().hentAvdoedesbarn()!!
            val avdoedesBarn = avdoed.verdi.avdoedesBarn!!.associateBy({ it.foedselsnummer.value }, { it })

            val alleSoeskenFinnes = soeskensFoedselsnummere.all { fnr -> avdoedesBarn.contains(fnr) }
            if (!alleSoeskenFinnes) {
                throw BPBeregningsgrunnlagSoeskenIkkeAvdoedesBarnException(
                    "Barnepensjon beregningsgrunnlag har søsken fnr som ikke er avdødeds barn. behandlingId: $behandlingId",
                )
            }

            val alleSoeskenIberegningenErlevende =
                soeskensFoedselsnummere.all { fnr -> avdoedesBarn[fnr]?.doedsdato === null }
            if (!alleSoeskenIberegningenErlevende) {
                throw BPBeregningsgrunnlagSoeskenMarkertDoedException(
                    "Barnpensjon beregningsgrunnlag bruker søsken som er døde i beregningen. behandlingId: $behandlingId",
                )
            }
        }
    }

    suspend fun lagreBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        when {
            behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, false) -> {
                validerBeregningsgrunnlagBarnepensjon(behandlingId, barnepensjonBeregningsGrunnlag, brukerTokenInfo)
                val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                val kanLagreDetteGrunnlaget =
                    if (behandling.behandlingType == BehandlingType.REVURDERING) {
                        // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                        val sisteIverksatteBehandling =
                            behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
                        grunnlagErIkkeEndretFoerVirk(
                            behandling,
                            sisteIverksatteBehandling.id,
                            barnepensjonBeregningsGrunnlag,
                        )
                    } else {
                        true
                    }

                val soeskenMedIBeregning =
                    barnepensjonBeregningsGrunnlag.soeskenMedIBeregning.ifEmpty {
                        when (val virkningstidspunkt = behandling.virkningstidspunkt) {
                            null -> throw RuntimeException("Kan ikke lagre default soeskenjustering uten virkningstidspunkt")
                            else -> listOf(GrunnlagMedPeriode(emptyList(), virkningstidspunkt.dato.atDay(1), null))
                        }
                    }

                kanLagreDetteGrunnlaget &&
                    beregningsGrunnlagRepository.lagre(
                        BeregningsGrunnlag(
                            behandlingId = behandlingId,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                            soeskenMedIBeregning = soeskenMedIBeregning,
                            institusjonsoppholdBeregningsgrunnlag =
                                barnepensjonBeregningsGrunnlag.institusjonsopphold ?: emptyList(),
                            beregningsMetode = barnepensjonBeregningsGrunnlag.beregningsMetode,
                        ),
                    )
            }

            else -> false
        }

    suspend fun lagreOMSBeregningsGrunnlag(
        behandlingId: UUID,
        omstillingstoenadBeregningsGrunnlag: OmstillingstoenadBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean =
        when {
            behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, false) -> {
                val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                val kanLagreDetteGrunnlaget =
                    if (behandling.behandlingType == BehandlingType.REVURDERING) {
                        // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                        val sisteIverksatteBehandling =
                            behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
                        grunnlagErIkkeEndretFoerVirkOMS(
                            behandling,
                            sisteIverksatteBehandling.id,
                            omstillingstoenadBeregningsGrunnlag,
                        )
                    } else {
                        true
                    }

                kanLagreDetteGrunnlaget &&
                    beregningsGrunnlagRepository.lagreOMS(
                        BeregningsGrunnlagOMS(
                            behandlingId = behandlingId,
                            kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                            institusjonsoppholdBeregningsgrunnlag =
                                omstillingstoenadBeregningsGrunnlag.institusjonsopphold ?: emptyList(),
                            beregningsMetode = omstillingstoenadBeregningsGrunnlag.beregningsMetode,
                        ),
                    )
            }

            else -> false
        }

    private fun grunnlagErIkkeEndretFoerVirk(
        revurdering: DetaljertBehandling,
        forrigeIverksatteBehandlingId: UUID,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag,
    ): Boolean {
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(
                forrigeIverksatteBehandlingId,
            )
        val revurderingVirk = revurdering.virkningstidspunkt!!.dato.atDay(1)

        val soeskenjusteringErLiktFoerVirk =
            erGrunnlagLiktFoerEnDato(
                barnepensjonBeregningsGrunnlag.soeskenMedIBeregning,
                forrigeGrunnlag!!.soeskenMedIBeregning,
                revurderingVirk,
            )
        val institusjonsoppholdErLiktFoerVirk =
            erGrunnlagLiktFoerEnDato(
                barnepensjonBeregningsGrunnlag.institusjonsopphold ?: emptyList(),
                forrigeGrunnlag.institusjonsoppholdBeregningsgrunnlag,
                revurderingVirk,
            )

        return soeskenjusteringErLiktFoerVirk && institusjonsoppholdErLiktFoerVirk
    }

    private fun grunnlagErIkkeEndretFoerVirkOMS(
        revurdering: DetaljertBehandling,
        forrigeIverksatteBehandlingId: UUID,
        omstillingstoenadBeregningsGrunnlag: OmstillingstoenadBeregningsGrunnlag,
    ): Boolean {
        val forrigeGrunnlag =
            beregningsGrunnlagRepository.finnOmstillingstoenadGrunnlagForBehandling(forrigeIverksatteBehandlingId)
        val revurderingVirk = revurdering.virkningstidspunkt!!.dato.atDay(1)

        return erGrunnlagLiktFoerEnDato(
            omstillingstoenadBeregningsGrunnlag.institusjonsopphold ?: emptyList(),
            forrigeGrunnlag?.institusjonsoppholdBeregningsgrunnlag ?: emptyList(),
            revurderingVirk,
        )
    }

    suspend fun hentBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(behandlingId)
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
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(sisteIverksatteBehandling.id)
        } else {
            null
        }
    }

    suspend fun hentOmstillingstoenadBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningsGrunnlagOMS? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnOmstillingstoenadGrunnlagForBehandling(behandlingId)
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
            beregningsGrunnlagRepository.finnOmstillingstoenadGrunnlagForBehandling(sisteIverksatteBehandling.id)
        } else {
            null
        }
    }

    fun dupliserBeregningsGrunnlagBP(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ) {
        logger.info("Dupliser grunnlag for $behandlingId fra $forrigeBehandlingId")

        val forrigeGrunnlagBP =
            beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(forrigeBehandlingId)
                ?: throw RuntimeException("Ingen grunnlag funnet for $forrigeBehandlingId")

        if (beregningsGrunnlagRepository.finnBarnepensjonGrunnlagForBehandling(behandlingId) != null) {
            throw RuntimeException("Eksisterende grunnlag funnet for $behandlingId")
        }

        beregningsGrunnlagRepository.lagre(forrigeGrunnlagBP.copy(behandlingId = behandlingId))
    }

    fun dupliserBeregningsGrunnlagOMS(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
    ) {
        logger.info("Dupliser grunnlag for $behandlingId fra $forrigeBehandlingId")

        val forrigeGrunnlagOMS =
            beregningsGrunnlagRepository.finnOmstillingstoenadGrunnlagForBehandling(forrigeBehandlingId)
                ?: throw RuntimeException("Ingen grunnlag funnet for $forrigeBehandlingId")

        if (beregningsGrunnlagRepository.finnOmstillingstoenadGrunnlagForBehandling(behandlingId) != null) {
            throw RuntimeException("Eksisterende grunnlag funnet for $behandlingId")
        }

        beregningsGrunnlagRepository.lagreOMS(forrigeGrunnlagOMS.copy(behandlingId = behandlingId))
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
