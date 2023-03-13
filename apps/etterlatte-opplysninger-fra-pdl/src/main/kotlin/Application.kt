package no.nav.etterlatte

import no.nav.etterlatte.opplysninger.kilde.pdl.AppBuilder
import no.nav.etterlatte.opplysninger.kilde.pdl.BesvarOpplysningsbehov
import no.nav.etterlatte.rapidsandrivers.init
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() = init(
    { AppBuilder(it) },
    { rc: RapidsConnection, ab: AppBuilder ->
        BesvarOpplysningsbehov(rc, ab.createPdlService())
    }
)