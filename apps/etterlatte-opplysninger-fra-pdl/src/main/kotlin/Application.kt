package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.opplysninger.kilde.pdl.AppBuilder
import no.nav.etterlatte.opplysninger.kilde.pdl.BesvarOpplysningsbehov
import no.nav.etterlatte.opplysninger.kilde.pdl.MigreringHendelser
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.getRapidEnv

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    val rapidEnv = getRapidEnv()
    val pdlService = AppBuilder(Miljoevariabler(rapidEnv)).createPdlService()
    val rapidsConnection = RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(rapidEnv))
        .withKtorModule {
            restModule(sikkerLogg, routePrefix = "api") {
                PdlRoute(pdlService)
            }
        }.build().apply {
            BesvarOpplysningsbehov(this, pdlService)
            MigreringHendelser(this, pdlService)
        }

    setReady().also { rapidsConnection.start() }
}