package no.nav.etterlatte.behandling.job

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.config.ApplicationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.measureTime

@OptIn(DelicateCoroutinesApi::class)
internal fun populerSaksbehandlereMedNavn(context: ApplicationContext) {
    val logger = LoggerFactory.getLogger("Saksbehandlerdatanavn")
    Thread.sleep(Duration.ofMinutes(3L).toMillis())
    if (context.leaderElectionKlient.isLeader()) {
        logger.info("Henting av Saksbehandlerdatanavn jobb leader is true, starter jobb")
        val subCoroutineExceptionHandler =
            CoroutineExceptionHandler { _, exception ->
                logger.error("Saksbehandlerdatanavn feilet se exception $exception")
            }

        GlobalScope.launch(newSingleThreadContext("saksbehandlernavnjob")) {
            try {
                saksbehandlereNavnJob(logger, context, subCoroutineExceptionHandler)
            } catch (e: Exception) {
                logger.error("Kunne ikke hente navn for saksbehandlere", e)
            }
            try {
                hentEnheterForSaksbehandlereJob(logger, context, subCoroutineExceptionHandler)
            } catch (e: Exception) {
                logger.error("Kunne ikke hente enheter for saksbehandlere", e)
            }
        }
    } else {
        logger.info("Ikke leader, kjører ikke Saksbehandlerdatanavn")
    }
}

val SAKSBEHANDLERPATTERN = Regex("[a-zA-Z]\\d{6}")

internal suspend fun hentEnheterForSaksbehandlereJob(
    logger: Logger,
    context: ApplicationContext,
    subCoroutineExceptionHandler: CoroutineExceptionHandler,
) {
    val tidbrukt =
        measureTime {
            val sbidenter = context.saksbehandlerInfoDao.hentalleSaksbehandlere()
            logger.info("Antall saksbehandlingsidenter vi henter identer for ${sbidenter.size}")

            // SupervisorJob så noen kall kan feile uten å cancle parent job
            val scope = CoroutineScope(SupervisorJob())
            val alleIdenterMedEnheter =
                sbidenter.filter {
                    !listOf(
                        "PESYS",
                        "EY",
                        "GJENOPPRETTA",
                    ).contains(it) && it.length == 7 && SAKSBEHANDLERPATTERN.containsMatchIn(it)
                }
                    .map {
                        it to
                            scope.async(
                                subCoroutineExceptionHandler,
                            ) { context.navAnsattKlient.hentEnhetForSaksbehandler(it) }
                    }
                    .map {
                        try {
                            val enheter = it.second.await()
                            it.first to enheter.map { enhet -> enhet.id }
                        } catch (e: Exception) {
                            logger.error("Kunne ikke hente enheter for saksbehandlerident ${it.first}")
                            null
                        }
                    }
                    .filterNotNull()

            logger.info("Hentet enheter for saksbehandlere antall: ${alleIdenterMedEnheter.size}")

            alleIdenterMedEnheter.forEach {
                context.saksbehandlerInfoDao.upsertSaksbehandlerEnheter(it)
            }
        }

    logger.info("Ferdig, tid brukt for å hente enheter $tidbrukt")
}

internal suspend fun saksbehandlereNavnJob(
    logger: Logger,
    context: ApplicationContext,
    subCoroutineExceptionHandler: CoroutineExceptionHandler,
) {
    val tidbrukt =
        measureTime {
            val sbidenter = context.saksbehandlerInfoDao.hentalleSaksbehandlere()
            logger.info("Antall saksbehandlingsidenter ${sbidenter.size}")
            val filtrerteIdenter = sbidenter.filter { !context.saksbehandlerInfoDao.saksbehandlerFinnes(it) }
            logger.info("Antall saksbehandlingsidenter uten navn i databasen ${sbidenter.size}")

            val egneIdenter =
                filtrerteIdenter.filter { listOf("PESYS", "EY", "GJENOPPRETTA").contains(it) }
                    .map { it to SaksbehandlerInfo(it, it) }

            logger.info("Mappet egne ${sbidenter.size}")

            val hentedeIdenter =
                coroutineScope {
                    filtrerteIdenter.filter {
                        !listOf(
                            "PESYS",
                            "EY",
                            "GJENOPPRETTA",
                        ).contains(it) && it.length == 7 && SAKSBEHANDLERPATTERN.containsMatchIn(it)
                    }
                        .map { it to async(subCoroutineExceptionHandler) { context.navAnsattKlient.hentSaksbehanderNavn(it) } }
                        .map { it.first to it.second.await() }
                }

            val alleIdenterMedNavn = hentedeIdenter + egneIdenter

            logger.info("mappedMedNavn antall: ${alleIdenterMedNavn.size}")

            alleIdenterMedNavn.forEach {
                if (it.second == null) {
                    context.saksbehandlerInfoDao.upsertSaksbehandlerNavn(SaksbehandlerInfo(it.first, it.first))
                } else {
                    context.saksbehandlerInfoDao.upsertSaksbehandlerNavn(it.second!!)
                }
            }
        }
    logger.info("Ferdig, tid brukt for å hente navn $tidbrukt")
}
