package no.nav.etterlatte.brev.adresse

import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Mottaker

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val regoppslagKlient: RegoppslagKlient
) {
    suspend fun hentMottakerAdresse(ident: String): Mottaker =
        regoppslagKlient.hentMottakerAdresse(ident).let {
            Mottaker.fraRegoppslag(it)
        }

    suspend fun hentAvsenderEnhet(navEnhetNr: String): Avsender {
        val enhet = norg2Klient.hentEnhet(navEnhetNr)

        // TODO: Hva gjør vi dersom postadresse mangler?
        val postadresse = enhet.kontaktinfo?.postadresse

        val kontor = enhet.navn ?: "NAV"
        val adresse = when (postadresse?.type) {
            "stedsadresse" -> postadresse.let { "${it.gatenavn} ${it.husnummer}${it.husbokstav ?: ""}" }
            "postboksadresse" -> "Postboks ${postadresse.postboksnummer} ${postadresse.postboksanlegg ?: ""}".trim()
            else -> throw Exception("Ukjent type postadresse ${postadresse?.type}")
        }
        val postnr = postadresse.let { "${it.postnummer} ${it.poststed}" }
        val telefon = enhet.kontaktinfo?.telefonnummer ?: ""

        return Avsender(
            kontor = kontor,
            adresse = adresse,
            postnummer = postnr,
            telefon = telefon
        )
    }
}
