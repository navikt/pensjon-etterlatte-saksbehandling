package no.nav.etterlatte.egenansatt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
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
    private val grunnlagKlient: GrunnlagKlient,
    private val oppdaterTilgangService: OppdaterTilgangService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        logger.info("Haandterer skjermet hendelse")
        val saker = sakService.finnSaker(skjermetHendelse.fnr)
        saker
            .map {
                val pg = runBlocking { grunnlagKlient.hentPersongalleri(it.id) }
                SakIdOgPersongalleri(sakId = it.id, persongalleri = pg)
            }.forEach {
                oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId = it.sakId, persongalleri = it.persongalleri)
            }
        logger.info("Ferdighåndtert skjermet hendelse")
    }
}
