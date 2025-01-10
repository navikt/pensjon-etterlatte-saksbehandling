package no.nav.etterlatte

import no.nav.etterlatte.gyldigsoeknad.NySoeknadRiver
import no.nav.etterlatte.gyldigsoeknad.OpprettBehandlingRiver
import no.nav.etterlatte.gyldigsoeknad.config.AppBuilder
import no.nav.etterlatte.inntektsjustering.InntektsjusteringRiver
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.omsendring.OmsMeldtInnEndringRiver
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

        InntektsjusteringRiver(
            rapidsConnection,
            behandlingKlient = ab.behandlingKlient,
            journalfoerInntektsjusteringService = ab.journalfoerInntektsjusteringService,
        )

        OmsMeldtInnEndringRiver(
            rapidsConnection,
            behandlingKlient = ab.behandlingKlient,
            journalfoerService = ab.journalfoerOmsMeldtInnEndringService,
        )
    }
