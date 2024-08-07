package no.nav.etterlatte

import no.nav.etterlatte.opplysningerfrasoknad.StartUthentingFraSoeknadRiver
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import rapidsandrivers.initRogR

fun main() =
    initRogR("opplysninger-fra-soeknad") { rapidsConnection, _ ->
        StartUthentingFraSoeknadRiver(rapidsConnection, Opplysningsuthenter())
    }
