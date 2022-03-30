package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.HentOpplysningerFraInntektskomponenten
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenResponse
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also {
                    HentOpplysningerFraInntektskomponenten(it, ab.createInntektsKomponentService(), ab.createOpplysningsbygger())
                }
                .start()
        }
    }
}

interface InntektsKomponenten {
    fun hentInntektListe(fnr: Foedselsnummer): InntektsKomponentenResponse
}

interface OpplysningsBygger {
    fun byggOpplysninger(barnepensjon: Barnepensjon, inntektsKomponentenResponse: InntektsKomponentenResponse):List<Behandlingsopplysning<out Any>>
}