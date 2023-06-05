package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.token.Bruker
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
        bruker: Bruker
    ): Boolean = when {
        behandlingKlient.beregn(behandlingId, bruker, false) -> {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val kanLagreDetteGrunnlaget = if (behandling.behandlingType == BehandlingType.REVURDERING) {
                // Her vil vi sjekke opp om det vi lagrer ned ikke er modifisert før virk på revurderingen
                val forrigeIverksatte = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
                grunnlagErIkkeEndretFoerVirk(behandling, forrigeIverksatte, barnepensjonBeregningsGrunnlag)
            } else {
                true
            }

            kanLagreDetteGrunnlaget && beregningsGrunnlagRepository.lagre(
                BeregningsGrunnlag(
                    behandlingId = behandlingId,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
                    soeskenMedIBeregning = barnepensjonBeregningsGrunnlag.soeskenMedIBeregning,
                    institusjonsoppholdBeregningsgrunnlag =
                    barnepensjonBeregningsGrunnlag.institusjonsoppholdBeregningsgrunnlag
                )
            )
        }

        else -> false
    }

    private fun grunnlagErIkkeEndretFoerVirk(
        revurdering: DetaljertBehandling,
        forrigeIverksatte: DetaljertBehandling,
        barnepensjonBeregningsGrunnlag: BarnepensjonBeregningsGrunnlag
    ): Boolean {
        val forrigeGrunnlag = beregningsGrunnlagRepository.finnGrunnlagForBehandling(forrigeIverksatte.id)

        // TODO: for periodisert institusjonsopphold må dette sjekkes her i tillegg til søskenjusteringen
        return erGrunnlagLiktFoerEnDato(
            barnepensjonBeregningsGrunnlag.soeskenMedIBeregning,
            forrigeGrunnlag!!.soeskenMedIBeregning,
            revurdering.virkningstidspunkt!!.dato.atDay(1)
        )
    }

    suspend fun hentBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID,
        bruker: Bruker
    ): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        val grunnlag = beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingId)
        if (grunnlag != null) {
            return grunnlag
        }

        // Det kan hende behandlingen er en revurdering, og da må vi finne forrige grunnlag for saken
        val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
        return if (behandling.behandlingType == BehandlingType.REVURDERING) {
            val forrigeIverksatteBehandling = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
            beregningsGrunnlagRepository.finnGrunnlagForBehandling(forrigeIverksatteBehandling.id)
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