package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentErForeldreloes
import no.nav.etterlatte.libs.common.grunnlag.hentErUfoere
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(Grunnlag::class.java)

fun Grunnlag.mapAvdoede(): List<Avdoed> =
    hentAvdoede()
        .filter { it.hentDoedsdato() != null }
        .map { avdoed ->
            Avdoed(
                fnr = Foedselsnummer(avdoed.hentFoedselsnummer()!!.verdi.value),
                navn = avdoed.hentNavn()!!.verdi.fulltNavn(),
                doedsdato = avdoed.hentDoedsdato()!!.verdi!!,
            )
        }

fun Navn.fulltNavn(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ") { it.storForbokstav() }

fun String.storForbokstav() = this.lowercase().storForbokstavEtter("-").storForbokstavEtter(" ")

fun String.storForbokstavEtter(delim: String) =
    this.split(delim).joinToString(delim) {
        it.replaceFirstChar { c -> c.uppercase() }
    }

fun Grunnlag.mapSoeker(aldersgruppe: Aldersgruppe?): Soeker =
    with(this.soeker) {
        val navn = hentNavn()!!.verdi

        Soeker(
            fornavn = navn.fornavn.storForbokstav(),
            mellomnavn = navn.mellomnavn?.storForbokstav(),
            etternavn = navn.etternavn.storForbokstav(),
            fnr = Foedselsnummer(hentFoedselsnummer()!!.verdi.value),
            under18 = !erOver18(aldersgruppe),
            foreldreloes = sak.hentErForeldreloes()?.verdi ?: false,
            ufoere = sak.hentErUfoere()?.verdi ?: false,
        )
    }

fun Grunnlag.hentGjenlevende(): List<Folkeregisteridentifikator> {
    with(this.sak) {
        val opplysning = hentKonstantOpplysning<Persongalleri>(Opplysningstype.PERSONGALLERI_V1)
        val persongalleri =
            krevIkkeNull(opplysning?.verdi) {
                "Sak (id=${metadata.sakId}) mangler opplysningstype PERSONGALLERI_V1"
            }
        return persongalleri.gjenlevende
    }
}

fun Grunnlag.mapInnsender(): Innsender? =
    with(this.sak) {
        val opplysning = hentKonstantOpplysning<Persongalleri>(Opplysningstype.PERSONGALLERI_V1)

        val persongalleri =
            krevIkkeNull(opplysning?.verdi) {
                "Sak (id=${metadata.sakId}) mangler opplysningstype PERSONGALLERI_V1"
            }

        persongalleri.innsender?.let {
            Innsender(fnr = Foedselsnummer(it.value))
        }
    }

fun Grunnlag.mapSpraak(): Spraak =
    with(this.sak) {
        val opplysning = hentKonstantOpplysning<Spraak>(Opplysningstype.SPRAAK)

        krevIkkeNull(opplysning?.verdi) {
            "Sak (id=${metadata.sakId}) mangler opplysningstype SPRAAK"
        }
    }

fun Grunnlag.hentForelderVerge(): ForelderVerge? {
    val gjenlevende = hentPotensiellGjenlevende()
    return if (gjenlevende != null && erAnsvarligForelder(gjenlevende)) {
        return forelderVerge(gjenlevende)
    } else {
        null
    }
}

private fun Grunnlag.erAnsvarligForelder(gjenlevende: Grunnlagsdata<JsonNode>): Boolean {
    val soekersAnsvarligeForeldre =
        this.soeker
            .hentFamilierelasjon()
            ?.verdi
            ?.ansvarligeForeldre ?: emptyList()

    return soekersAnsvarligeForeldre.contains(gjenlevende.hentFoedselsnummer()?.verdi)
}

private fun forelderVerge(gjenlevende: Grunnlagsdata<JsonNode>): ForelderVerge? {
    val navn =
        gjenlevende.hentNavn()?.verdi?.fulltNavn().also {
            if (it == null) {
                logger.error("Vi har ikke navnet på den som har foreldreansvar")
            }
        }
    val foedselsnummer =
        gjenlevende.hentFoedselsnummer()?.verdi.also {
            if (it == null) {
                logger.error("Vi har ikke fødselsnummer på den som har foreldreansvar")
            }
        }
    return if (navn != null && foedselsnummer != null) {
        ForelderVerge(foedselsnummer, navn)
    } else {
        null
    }
}

fun Grunnlag.erOver18(aldersgruppe: Aldersgruppe?): Boolean {
    if (aldersgruppe == Aldersgruppe.OVER_18) {
        return true
    }
    // TODO henting fra PDL skal fjernes når migrering er unnagjort. Da kan vi alltid bruke brevutfall
    // TODO denne brukes nå også midlertidig av avslagsbrev siden vi ikke har noe brevutfall der enda
    val dato18Aar =
        krevIkkeNull(this.soeker.hentFoedselsdato()) {
            "Barnet har ikke fødselsdato i grunnlag. Dette skal ikke skje, vi " +
                "klarer ikke å avgjøre hvor gammelt barnet er"
        }.verdi.plusYears(18)
    return LocalDate.now() >= dato18Aar
}
