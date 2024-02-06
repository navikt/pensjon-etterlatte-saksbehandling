package no.nav.etterlatte.behandling.job

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.config.ApplicationContext
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

@OptIn(DelicateCoroutinesApi::class)
internal fun populerSaksbehandlereMedNavn(context: ApplicationContext) {
    val logger = LoggerFactory.getLogger("saksbehandlernavnjob")
    if (context.leaderElectionKlient.isLeader()) {
        logger.info("Henting av saksbehandlernavn jobb leader is true, starter jobb")
        GlobalScope.launch(newSingleThreadContext("saksbehandlernavnjob")) {
            val tidbrukt =
                measureTime {
                    val sbidenter = context.saksbehandlerInfoDao.hentalleSaksbehandlere()
                    logger.info("Antall identer ${sbidenter.size}")

                    val filtrerteIdenter = sbidenter.filter { !context.saksbehandlerInfoDao.saksbehandlerFinnes(it) }

                    val egneIdenter =
                        filtrerteIdenter.filter { listOf("PESYS", "EY").contains(it) }
                            .map { it to SaksbehandlerInfo(it, it) }

                    val hentedeIdenter =
                        coroutineScope {
                            filtrerteIdenter.filter { !listOf("PESYS", "EY").contains(it) }
                                .map { it to async { context.navAnsattKlient.hentSaksbehanderNavn(it) } }
                                .map { it.first to it.second.await() }
                        }

                    val alleIdenterMedNavn = hentedeIdenter + egneIdenter

                    logger.info("mappedMedNavn antall: ${alleIdenterMedNavn.size}")

                    alleIdenterMedNavn.forEach {
                        if (it.second == null) {
                            context.saksbehandlerInfoDao.upsertSaksbehandler(SaksbehandlerInfo(it.first, it.first))
                        } else {
                            context.saksbehandlerInfoDao.upsertSaksbehandler(it.second!!)
                        }
                    }
                }
            logger.info("Ferdig, tid brukt $tidbrukt")
        }
    } else {
        logger.info("Ikke leader, kjører ikke saksbehandlernavnjob")
    }
}
