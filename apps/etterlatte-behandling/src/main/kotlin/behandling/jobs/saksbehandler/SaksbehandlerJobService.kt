package no.nav.etterlatte.behandling.jobs.saksbehandler

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.EntraProxyKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.libs.ktor.PingResultDown
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

val ugyldigeIdenter =
    listOf(
        "PESYS",
        "EY",
        "GJENOPPRETTA",
    )

val SAKSBEHANDLERPATTERN = Regex("[a-zA-Z]\\d{6}")

class SaksbehandlerJobService(
    private val saksbehandlerInfoDao: SaksbehandlerInfoDao,
    private val navAnsattKlient: NavAnsattKlient,
    private val entraProxyKlient: EntraProxyKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(DelicateCoroutinesApi::class)
    fun run() {
        val subCoroutineExceptionHandler =
            CoroutineExceptionHandler { _, exception ->
                logger.error("Saksbehandlerdatanavn feilet se exception", exception)
            }
        newSingleThreadContext("saksbehandlernavnjob").use { ctx ->
            Runtime.getRuntime().addShutdownHook(Thread { ctx.close() })
            runBlocking(ctx) {
                try {
                    oppdaterSaksbehandlerNavn(logger, saksbehandlerInfoDao, navAnsattKlient, subCoroutineExceptionHandler)
                } catch (e: Exception) {
                    logger.error("Kunne ikke hente navn for saksbehandlere", e)
                }
                try {
                    oppdaterSaksbehandlerEnhet(logger, saksbehandlerInfoDao, entraProxyKlient, subCoroutineExceptionHandler)
                } catch (e: Exception) {
                    logger.error("Kunne ikke hente enheter for saksbehandlere", e)
                }
            }
        }
    }
}

internal suspend fun oppdaterSaksbehandlerEnhet(
    logger: Logger,
    saksbehandlerInfoDao: SaksbehandlerInfoDao,
    entraProxyKlient: EntraProxyKlient,
    subCoroutineExceptionHandler: CoroutineExceptionHandler,
) {
    when (val pingRes = entraProxyKlient.ping()) {
        is PingResultDown -> {
            logger.warn(
                "EntraProxyKlient er ikke ready, forsøker ikke å oppdatere saksbehandleres enheter. ${pingRes.toStringServiceDown()}",
            )
        }
        is PingResultUp -> {
            val tidbrukt =
                measureTime {
                    val sbidenter = saksbehandlerInfoDao.hentAlleSaksbehandlerIdenter()
                    logger.info("Antall saksbehandlingsidenter vi henter identer for ${sbidenter.size}")

                    // SupervisorJob så noen kall kan feile uten å cancle parent job
                    val scope = CoroutineScope(SupervisorJob())
                    val alleIdenterMedEnheter =
                        sbidenter
                            .filter {
                                it !in ugyldigeIdenter && SAKSBEHANDLERPATTERN.matches(it)
                            }.map {
                                it to
                                    scope.async(
                                        subCoroutineExceptionHandler,
                                    ) { entraProxyKlient.hentEnheterForIdent(it) }
                            }.mapNotNull { (ident, enheter) ->
                                try {
                                    val enheterAwait = enheter.await()
                                    if (enheterAwait.isNotEmpty()) {
                                        ident to enheterAwait
                                    } else {
                                        logger.info("Saksbehandler med ident $ident har ingen enheter")
                                        null
                                    }
                                } catch (e: Exception) {
                                    logger.error("Kunne ikke hente enheter for saksbehandlerident $ident", e)
                                    null
                                }
                            }

                    logger.info("Hentet enheter for saksbehandlere antall: ${alleIdenterMedEnheter.size}")

                    alleIdenterMedEnheter.forEach {
                        saksbehandlerInfoDao.upsertSaksbehandlerEnheter(it)
                    }
                }

            logger.info("Ferdig, tid brukt for å hente enheter $tidbrukt")
        }
    }
}

internal suspend fun oppdaterSaksbehandlerNavn(
    logger: Logger,
    saksbehandlerInfoDao: SaksbehandlerInfoDao,
    navAnsattKlient: NavAnsattKlient,
    subCoroutineExceptionHandler: CoroutineExceptionHandler,
) {
    when (val pingRes = navAnsattKlient.ping()) {
        is PingResultDown ->
            logger.warn(
                "navAnsattKlient er ikke ready, forsøker ikke å oppdatere saksbehandleres navn. ${pingRes.toStringServiceDown()}",
            )
        is PingResultUp -> {
            val tidbrukt =
                measureTime {
                    val sbidenter = saksbehandlerInfoDao.hentAlleSaksbehandlerIdenter()
                    logger.info("Antall saksbehandlingsidenter ${sbidenter.size}")
                    val filtrerteIdenter = sbidenter.filter { !saksbehandlerInfoDao.saksbehandlerFinnes(it) }
                    logger.info("Antall saksbehandlingsidenter uten navn i databasen ${sbidenter.size}")

                    val egneIdenter =
                        filtrerteIdenter
                            .filter { it in ugyldigeIdenter }
                            .map { it to SaksbehandlerInfo(it, it) }

                    logger.info("Mappet egne ${sbidenter.size}")

                    val hentedeIdenter =
                        coroutineScope {
                            filtrerteIdenter
                                .filter {
                                    it !in ugyldigeIdenter && SAKSBEHANDLERPATTERN.matches(it)
                                }.map {
                                    it to
                                        async(subCoroutineExceptionHandler) {
                                            navAnsattKlient.hentSaksbehanderNavn(it)
                                        }
                                }.map { it.first to it.second.await() }
                        }

                    val alleIdenterMedNavn = hentedeIdenter + egneIdenter

                    logger.info("mappedMedNavn antall: ${alleIdenterMedNavn.size}")

                    alleIdenterMedNavn.forEach { (ident, saksbehandlerInfo) ->
                        if (saksbehandlerInfo == null) {
                            saksbehandlerInfoDao.upsertSaksbehandlerNavn(SaksbehandlerInfo(ident, ident))
                        } else {
                            saksbehandlerInfoDao.upsertSaksbehandlerNavn(saksbehandlerInfo)
                        }
                    }
                }
            logger.info("Ferdig, tid brukt for å hente navn $tidbrukt")
        }
    }
}
