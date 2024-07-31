package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.NySoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.OpprettBehandlingRiver
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import rapidsandrivers.initRogR

val sikkerLogg = sikkerlogger()

fun main() =
    initRogR("gyldig-soeknad") { rapidsConnection, rapidEnv ->
        val ab = AppBuilder(rapidEnv)

        NySoeknadRiver(
            rapidsConnection = rapidsConnection,
            behandlingKlient = ab.behandlingKlient,
            journalfoerSoeknadService = ab.journalfoerSoeknadService,
        )

        OpprettBehandlingRiver(
            rapidsConnection,
            ab.behandlingKlient,
        )
    }
