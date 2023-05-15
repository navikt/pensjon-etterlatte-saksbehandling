package no.nav.etterlatte.beregning.grunnlag

import no.nav.etterlatte.beregning.klienter.BehandlingKlient
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
            beregningsGrunnlagRepository.lagre(
                BeregningsGrunnlag(
                    behandlingId = behandlingId,
                    kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
                    soeskenMedIBeregning = barnepensjonBeregningsGrunnlag.soeskenMedIBeregningPerioder,
                    institusjonsopphold = barnepensjonBeregningsGrunnlag.institusjonsopphold
                )
            )
        }

        else -> false
    }

    fun hentBarnepensjonBeregningsGrunnlag(
        behandlingId: UUID
    ): BeregningsGrunnlag? {
        logger.info("Henter grunnlag $behandlingId")
        return beregningsGrunnlagRepository.finnGrunnlagForBehandling(behandlingId)
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