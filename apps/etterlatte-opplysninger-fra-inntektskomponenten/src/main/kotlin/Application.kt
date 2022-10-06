package no.nav.etterlatte

import no.nav.etterlatte.libs.common.arbeidsforhold.AaregResponse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.HentOpplysningerFraInntektskomponenten
import no.nav.etterlatte.opplysninger.kilde.inntektskomponenten.InntektsKomponentenResponse
import no.nav.helse.rapids_rivers.RapidApplication
import java.time.LocalDate

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        AppBuilder(env).also { ab ->
            RapidApplication.create(env)
                .also {
                    HentOpplysningerFraInntektskomponenten(
                        it,
                        ab.createInntektsKomponentService(),
                        ab.createAaregService(),
                        ab.createOpplysningsbygger()
                    )
                }
                .start()
        }
    }
}

interface InntektsKomponenten {
    fun hentInntektListe(fnr: Foedselsnummer, doedsdato: LocalDate): InntektsKomponentenResponse
}

interface Aareg {
    fun hentArbeidsforhold(fnr: Foedselsnummer): List<AaregResponse>
}

interface OpplysningsBygger {
    fun byggOpplysninger(
        inntektsKomponentenResponse: InntektsKomponentenResponse,
        arbeidsforholdListe: List<AaregResponse>,
        fnr: Foedselsnummer
    ): List<Grunnlagsopplysning<out Any>>
}