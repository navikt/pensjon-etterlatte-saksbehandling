package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val behandlingKlient: BehandlingKlient,
    private val inntektAvkortingService: InntektAvkortingService,
    private val avkortingRepository: AvkortingRepository,
    private val beregningRepository: BeregningRepository
) {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    fun hentAvkorting(behandlingId: UUID): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
    }

    suspend fun lagreAvkorting(
        behandlingId: UUID,
        bruker: Bruker,
        avkortingGrunnlag: AvkortingGrunnlag
    ): Avkorting = tilstandssjekk(behandlingId, bruker) {
        // TODO EY-2127 transaksjonshandtering
        logger.info("Lagre grunnlag for avkorting for behandlingId=$behandlingId")

        val inntektavkorting = inntektAvkortingService.beregnInntektsavkorting(avkortingGrunnlag)
        val avkortingMedGrunnlag = avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(
            behandlingId,
            avkortingGrunnlag.copy(beregnetAvkorting = inntektavkorting)
        )

        val beregning = beregningRepository.hent(behandlingId)
            ?: throw Exception("Mangler beregning for behandlingId=$behandlingId")
        val beregnetAvkortetYtelse = inntektAvkortingService.beregnAvkortetYtelse(
            beregning.beregningsperioder,
            avkortingMedGrunnlag.avkortingGrunnlag
        )

        val avkortetYtelse = avkortingRepository.lagreEllerOppdaterAvkortetYtelse(behandlingId, beregnetAvkortetYtelse)
        behandlingKlient.avkort(behandlingId, bruker, true)
        avkortetYtelse
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Avkorting): Avkorting {
        val kanAvkorte = behandlingKlient.avkort(behandlingId, bruker, commit = false)
        return if (kanAvkorte) {
            block()
        } else {
            throw Exception("Kan ikke avkorte da behandlingen er i feil tilstand")
        }
    }
}