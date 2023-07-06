package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class BeregningsGrunnlagService(
    private val beregningsGrunnlagRepository: BeregningsGrunnlagRepository,
    private val behandlingKlient: BehandlingKlient
) {

    private val logger = LoggerFactory.getLogger(BeregningsGrunnlagService::class.java)

    suspend fun lagreBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean = when {
        behandlingKlient.beregn(behandlingId, brukerTokenInfo, false) -> {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            val kanLagreDetteGrunnlaget = if (behandling.behandlingType == BehandlingType.REVURDERING) {
                // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                val sisteIverksatteBehandling =
                    behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
                grunnlagErIkkeEndretFoerVirk(behandling, sisteIverksatteBehandling.id, barnepensjonBeregningsGrunnlag)
            } else {
                true
            }

            val soeskenMedIBeregning = barnepensjonBeregningsGrunnlag.soeskenMedIBeregning.ifEmpty {
                when (val virkningstidspunkt = behandling.virkningstidspunkt) {
                    null -> throw RuntimeException("Kan ikke lagre default soeskenjustering uten virkningstidspunkt")
                    else -> listOf(GrunnlagMedPeriode(emptyList(), virkningstidspunkt.dato.atDay(1), null))
                }
            }

            kanLagreDetteGrunnlaget && beregningsGrunnlagRepository.lagre(
                BeregningsGrunnlag(
                    behandlingId = behandlingId,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                    soeskenMedIBeregning = soeskenMedIBeregning,
                    institusjonsoppholdBeregningsgrunnlag =
                    barnepensjonBeregningsGrunnlag.institusjonsopphold
                )
            )
        }

        else -> false
    }

    private fun grunnlagErIkkeEndretFoerVirk(
        revurdering: DetaljertBehandling,
        forrigeIverksatteBehandlingId: UUID,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag
    ): Boolean {
        val forrigeGrunnlag = beregningsGrunnlagRepository.finnGrunnlagForBehandling(forrigeIverksatteBehandlingId)
        val revurderingVirk = revurdering.virkningstidspunkt!!.dato.atDay(1)

        val soeskenjusteringErLiktFoerVirk = erGrunnlagLiktFoerEnDato(
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning,
            forrigeGrunnlag!!.soeskenMedIBeregning,
            revurderingVirk
        )
        val institusjonsoppholdErLiktFoerVirk = erGrunnlagLiktFoerEnDato(
            barnepensjonBeregningsGrunnlag.institusjonsopphold ?: emptyList(),
            forrigeGrunnlag.institusjonsoppholdBeregningsgrunnlag ?: emptyList(),
            revurderingVirk
        )

        return soeskenjusteringErLiktFoerVirk && institusjonsoppholdErLiktFoerVirk
    }

    suspend fun hentBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingId)
        if (grunnlag != null) {
            return grunnlag
        }

        // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return if (behandling.behandlingType == BehandlingType.REVURDERING) {
            val sisteIverksatteBehandling = behandlingKlient.hentSisteIverksatteBehandling(
                behandling.sak,
                brukerTokenInfo
            )
            beregningsGrunnlagRepository.finnGrunnlagForBehandling(sisteIverksatteBehandling.id)
        } else {
            null
        }
    }

    fun dupliserBeregningsGrunnlag(behandlingId: UUID, forrigeBehandlingId: UUID) {
        logger.info("Dupliser grunnlag for $behandlingId fra $forrigeBehandlingId")

        val forrigeGrunnlag = beregningsGrunnlagRepository.finnGrunnlagForBehandling(forrigeBehandlingId)
            ?: throw RuntimeException("Ingen grunnlag funnet for $forrigeBehandlingId")

        if (beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingId) != null) {
            throw RuntimeException("Eksisterende grunnlag funnet for $behandlingId")
        }

        beregningsGrunnlagRepository.lagre(forrigeGrunnlag.copy(behandlingId = behandlingId))
    }
}