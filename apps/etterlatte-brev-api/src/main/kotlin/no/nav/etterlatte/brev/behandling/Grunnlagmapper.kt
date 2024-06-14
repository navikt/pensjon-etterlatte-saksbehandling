package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
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
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val logger = LoggerFactory.getLogger(Grunnlag::class.java)

fun Grunnlag.mapSoeker(brevutfallDto: BrevutfallDto?): Soeker =
    with(this.soeker) {
        val navn = hentNavn()!!.verdi

        Soeker(
            fornavn = navn.fornavn.storForbokstav(),
            mellomnavn = navn.mellomnavn?.storForbokstav(),
            etternavn = navn.etternavn.storForbokstav(),
            fnr = Foedselsnummer(hentFoedselsnummer()!!.verdi.value),
            under18 = !erOver18(brevutfallDto),
            foreldreloes = sak.hentErForeldreloes()?.verdi ?: false,
            ufoere = sak.hentErUfoere()?.verdi ?: false,
        )
    }

fun Grunnlag.mapAvdoede(): List<Avdoed> =
    with(this.familie) {
        val avdoede = hentAvdoede()

        return avdoede
            .filter { it.hentDoedsdato() != null }
            .map { avdoed ->
                Avdoed(
                    fnr = Foedselsnummer(avdoed.hentFoedselsnummer()!!.verdi.value),
                    navn = avdoed.hentNavn()!!.verdi.fulltNavn(),
                    doedsdato = avdoed.hentDoedsdato()!!.verdi!!,
                )
            }
    }

fun Grunnlag.mapInnsender(): Innsender? =
    with(this.sak) {
        val opplysning = hentKonstantOpplysning<Persongalleri>(Opplysningstype.PERSONGALLERI_V1)

        val persongalleri =
            requireNotNull(opplysning?.verdi) {
                "Sak (id=${metadata.sakId}) mangler opplysningstype PERSONGALLERI_V1"
            }

        persongalleri.innsender?.let {
            Innsender(fnr = Foedselsnummer(it))
        }
    }

fun Grunnlag.mapSpraak(): Spraak =
    with(this.sak) {
        val opplysning = hentKonstantOpplysning<Spraak>(Opplysningstype.SPRAAK)

        requireNotNull(opplysning?.verdi) {
            "Sak (id=${metadata.sakId}) mangler opplysningstype SPRAAK"
        }
    }

fun Grunnlag.mapVerge(
    sakType: SakType,
    brevutfallDto: BrevutfallDto?,
    adresseService: AdresseService,
): Verge? =
    with(this) {
        val verger =
            hentVerger(
                soeker.hentSoekerPdlV1()!!.verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
                soeker.hentFoedselsnummer()?.verdi,
            )
        return if (verger.size == 1) {
            val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident!!
            Vergemaal(
                navnViaAdresse(adresseService, sakType, vergeFnr),
                vergeFnr,
            )
        } else if (verger.size > 1) {
            UkjentVergemaal()
        } else if (sakType == SakType.BARNEPENSJON && !erOver18(brevutfallDto)) {
            hentForelderVerge()
        } else {
            null
        }
    }

private fun navnViaAdresse(
    adresseService: AdresseService,
    sakType: SakType,
    vergeFnr: Folkeregisteridentifikator,
): String =
    runBlocking {
        adresseService
            .hentMottakerAdresse(sakType, vergeFnr.value)
            .navn
    }

private fun Grunnlag.hentForelderVerge(): ForelderVerge? {
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

private fun Navn.fulltNavn(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ") { it.storForbokstav() }

private fun String.storForbokstav() = this.lowercase().storForbokstavEtter("-").storForbokstavEtter(" ")

private fun String.storForbokstavEtter(delim: String) =
    this.split(delim).joinToString(delim) {
        it.replaceFirstChar { c -> c.uppercase() }
    }

private fun Grunnlag.erOver18(brevutfallDto: BrevutfallDto?): Boolean {
    if (brevutfallDto?.aldersgruppe == Aldersgruppe.OVER_18) {
        return true
    }
    // TODO henting fra PDL skal fjernes når migrering er unnagjort. Da kan vi alltid bruke brevutfall
    // TODO denne brukes nå også midlertidig av avslagsbrev siden vi ikke har noe brevutfall der enda
    val dato18Aar =
        requireNotNull(this.soeker.hentFoedselsdato()) {
            "Barnet har ikke fødselsdato i grunnlag. Dette skal ikke skje, vi " +
                "klarer ikke å avgjøre hvor gammelt barnet er"
        }.verdi.plusYears(18)
    return LocalDate.now() >= dato18Aar
}
