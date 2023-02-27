package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregnBarnepensjonService: BeregnBarnepensjonService,
    private val beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService
) {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    fun hentBeregning(behandlingId: UUID): Beregning? {
        logger.info("Henter beregning for behandlingId=$behandlingId")
        return beregningRepository.hent(behandlingId)
    }

    suspend fun opprettBeregning(behandlingId: UUID, bruker: Bruker): Beregning {
        logger.info("Oppretter beregning for behandlingId=$behandlingId")
        return tilstandssjekkFoerKjoerning(behandlingId, accessToken) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, accessToken)
            val sak = behandlingKlient.hentSak(behandling.sak, accessToken)

            val beregning = when (sak.sakType) {
                SakType.BARNEPENSJON -> beregnBarnepensjonService.beregn(behandling, bruker)
                SakType.OMSTILLINGSSTOENAD -> beregnOmstillingsstoenadService.beregn(behandling, bruker)
            }

            beregningRepository.lagreEllerOppdaterBeregning(beregning).also {
                behandlingKlient.beregn(behandlingId, bruker, true)
            }
        }
    }

    private suspend fun tilstandssjekkFoerKjoerning(
        behandlingId: UUID,
        bruker: Bruker,
        block: suspend () -> Beregning
    ): Beregning {
        val kanBeregne = behandlingKlient.beregn(behandlingId, bruker, false)

        if (!kanBeregne) {
            throw IllegalStateException("Kunne ikke beregne, behandling er i feil state")
        }
        return block()
    }
}