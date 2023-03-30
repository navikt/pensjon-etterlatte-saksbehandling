package no.nav.etterlatte

import no.nav.etterlatte.opplysningerfrasoknad.StartUthentingFraSoeknad
import no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter.Opplysningsuthenter
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    RapidApplication.create(getRapidEnv())
        .also { rapidsConnection ->
            StartUthentingFraSoeknad(rapidsConnection, Opplysningsuthenter())
        }
        .start()
}