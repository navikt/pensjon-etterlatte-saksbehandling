package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import org.slf4j.LoggerFactory

data class SakIdOgPersongalleri(
    val sakId: SakId,
    val persongalleri: Persongalleri,
)

class EgenAnsattService(
    private val sakService: SakService,
    private val grunnlagService: GrunnlagService,
    private val oppdaterTilgangService: OppdaterTilgangService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        logger.info("Haandterer skjermet hendelse")
        val saker = sakService.finnSaker(skjermetHendelse.fnr)
        saker
            .map {
                val pg = grunnlagService.hentPersongalleri(it.id)!!
                SakIdOgPersongalleri(sakId = it.id, persongalleri = pg)
            }.forEach {
                oppdaterTilgangService.haandtergraderingOgEgenAnsatt(
                    sakId = it.sakId,
                    persongalleri = it.persongalleri,
                    grunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(it.sakId),
                )
            }

        logger.info("Ferdighåndtert skjermet hendelse")
    }

    fun oppdaterGraderingOgEgenAnsatt(sakId: SakId) {
        logger.info("Oppdaterer gradering og egen ansatt")
        val persongalleri = grunnlagService.hentPersongalleri(sakId)!!

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(
            sakId = sakId,
            persongalleri = persongalleri,
            grunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(sakId),
        )
        logger.info("Ferdig oppdatert gradering og egen ansatt")
    }

    fun hentSkjermedeSaker(sakType: SakType): List<SakId> = sakService.hentSakerMedSkjerming(sakType)
}
