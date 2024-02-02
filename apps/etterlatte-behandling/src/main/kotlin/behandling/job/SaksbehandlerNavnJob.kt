package no.nav.etterlatte.behandling.job

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.config.ApplicationContext
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

@OptIn(DelicateCoroutinesApi::class)
internal fun populerSaksbendlereMedNavn(context: ApplicationContext) {
    if (context.leaderElectionKlient.isLeader()) {
        GlobalScope.launch(newSingleThreadContext("saksbehandlernavnjob")) {
            val logger = LoggerFactory.getLogger("saksbehandlernavnjob")
            val tidbrukt =
                measureTime {
                    logger.info("Starter job for å legge inn saksbehandlere med navn")
                    val sbidenter = context.saksbehandlerInfoDao.hentalleSaksbehandlere().mapNotNull { it }
                    logger.info("Antall identer ${sbidenter.size}")

                    val filtrerteIdenter = sbidenter.filter { !context.saksbehandlerInfoDao.saksbehandlerFinnes(it) }
                    val mappedMedNavn =
                        filtrerteIdenter.map {
                            if (listOf("PESYS", "EY").contains(it)) {
                                it to SaksbehandlerInfo(it, it)
                            } else {
                                it to runBlocking { context.navAnsattKlient.hentSaksbehanderNavn(it) }
                            }
                        }
                    logger.info("mappedMedNavn antall: ${mappedMedNavn.size}")

                    mappedMedNavn.forEach {
                        SaksbehandlerInfo(it.first, it.first)
                        if (it.second == null) {
                            context.saksbehandlerInfoDao.upsertSaksbehandler(SaksbehandlerInfo(it.first, it.first))
                        } else {
                            context.saksbehandlerInfoDao.upsertSaksbehandler(it.second!!)
                        }
                    }
                }
            logger.info("Ferdig, tid brukt $tidbrukt")
        }
    }
}
