package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.NySoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.OpprettBehandlingRiver
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.ktor.setReady
import rapidsandrivers.initRogR

val sikkerLogg = sikkerlogger()

fun main() =
    initRogR(
        restModule = null,
        setReady = { setReady() },
    ) { rapidsConnection, rapidEnv ->
        val ab = AppBuilder(Miljoevariabler(rapidEnv))

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
