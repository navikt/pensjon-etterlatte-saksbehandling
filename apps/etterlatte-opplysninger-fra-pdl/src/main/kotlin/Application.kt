package no.nav.etterlatte

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.opplysninger.kilde.pdl.AppBuilder
import no.nav.etterlatte.opplysninger.kilde.pdl.BesvarOpplysningsbehov
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() {
    val rapidEnv = getRapidEnv()
    RapidApplication.create(rapidEnv).also {
        BesvarOpplysningsbehov(it, AppBuilder(Miljoevariabler(rapidEnv)).createPdlService())
    }.start()
}