package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentErForeldreloes
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentKonstantOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.hentVergeadresse
import no.nav.etterlatte.libs.common.grunnlag.hentVergemaalellerfremtidsfullmakt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.ForelderVerge
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.hentRelevantVerge
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.time.LocalDate
import java.util.UUID

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
        )
    }

fun Grunnlag.mapAvdoede(): List<Avdoed> =
    with(this.familie) {
        val avdoede = hentAvdoede()

        return avdoede
            .filter { it.hentDoedsdato() != null }
            .map { avdoed ->
                Avdoed(
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
    behandlingId: UUID,
    brevutfallDto: BrevutfallDto?,
): Verge? =
    with(this) {
        val relevantVerge = hentRelevantVerge(soeker.hentVergemaalellerfremtidsfullmakt()?.verdi)
        if (relevantVerge != null) {
            return hentVergemaal(relevantVerge, behandlingId)
        }

        if (sakType == SakType.BARNEPENSJON) {
            if (erOver18(brevutfallDto)) {
                null
            } else {
                hentForelderVerge()
            }
        } else {
            null
        }
    }

private fun Grunnlag.hentForelderVerge(): ForelderVerge? {
    val soekersAnsvarligeForeldre = this.soeker.hentFamilierelasjon()?.verdi?.ansvarligeForeldre ?: emptyList()
    val gjenlevende = hentPotensiellGjenlevende()
    val gjenlevendeIdent = gjenlevende?.hentFoedselsnummer()?.verdi
    val gjenlevendeHarForeldreansvar = soekersAnsvarligeForeldre.contains(gjenlevendeIdent)
    return if (gjenlevendeHarForeldreansvar) {
        gjenlevende
            ?.hentNavn()
            ?.verdi
            ?.fulltNavn()
            ?.let { ForelderVerge(it) }
    } else {
        null // Vi har ikke navnet på den som har foreldreansvar
    }
}

private fun Grunnlag.hentVergemaal(
    pdlVerge: VergemaalEllerFremtidsfullmakt,
    behandlingId: UUID,
): Vergemaal {
    val vergeadresse = sak.hentVergeadresse()?.verdi

    if (vergeadresse?.navn == null && pdlVerge.vergeEllerFullmektig.navn == null) {
        throw VergeManglerNavnException(behandlingId)
    }
    if (vergeadresse == null) {
        throw VergeManglerAdresseException(behandlingId)
    }
    return Vergemaal(
        mottaker = vergeadresse.copy(navn = vergeadresse.navn ?: pdlVerge.vergeEllerFullmektig.navn!!),
    )
}

private fun Navn.fulltNavn(): String = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ") { it.storForbokstav() }

private fun String.storForbokstav() = this.lowercase().storForbokstavEtter("-").storForbokstavEtter(" ")

private fun String.storForbokstavEtter(delim: String) =
    this.split(delim).joinToString(delim) {
        it.replaceFirstChar { c -> c.uppercase() }
    }

private fun Grunnlag.erOver18(brevutfallDto: BrevutfallDto?): Boolean {
    // TODO er dette ok? Migrering vil ikke finne noe brevutfall og dermed må PDL-fødselsdato brukes
    if (brevutfallDto?.aldersgruppe == Aldersgruppe.OVER_18) {
        return true
    }
    // TODO henting fra PDL skal fjernes når migrering er unnagjort. Da kan vi alltid bruke brevutfall
    val dato18Aar =
        requireNotNull(this.soeker.hentFoedselsdato()) {
            "Barnet har ikke fødselsdato i grunnlag. Dette skal ikke skje, vi " +
                "klarer ikke å avgjøre hvor gammelt barnet er"
        }.verdi.plusYears(18)
    return LocalDate.now() >= dato18Aar
}

class VergeManglerNavnException(
    behandlingId: UUID,
) : InternfeilException("Finner ikke navn for verge i behandling med id=$behandlingId")

class VergeManglerAdresseException(
    behandlingId: UUID,
) : InternfeilException("Finner ikke adresse for verge i behandling med id=$behandlingId")
