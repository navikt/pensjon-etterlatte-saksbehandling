package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.model.Attestant
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.MottakerRequest
import no.nav.etterlatte.brev.navansatt.NavansattKlient

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val navansattKlient: NavansattKlient,
    private val regoppslagKlient: RegoppslagKlient
) {
    suspend fun hentMottakerAdresse(ident: String): MottakerRequest =
        regoppslagKlient.hentMottakerAdresse(ident).let {
            MottakerRequest.fraRegoppslag(it)
        }

    suspend fun hentAvsenderEnhet(navEnhetNr: String, saksbehandlerNavn: String): Avsender {
        val enhet = norg2Klient.hentEnhet(navEnhetNr)

        return mapTilAvsender(enhet, saksbehandlerNavn)
    }

    suspend fun hentEnhet(navEnhetNr: String): Norg2Enhet = norg2Klient.hentEnhet(navEnhetNr)

    suspend fun hentAvsenderOgAttestant(vedtak: ForenkletVedtak): Pair<Avsender, Attestant?> = coroutineScope {
        val avsender = vedtak.saksbehandler.let {
            val saksbehandlerNavn = async { navansattKlient.hentSaksbehandlerInfo(it.ident).navn }
            val saksbehandlerEnhet = async { hentEnhet(it.enhet) }

            mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await())
        }

        val attestant = vedtak.attestant?.let {
            val attestantNavn = async { navansattKlient.hentSaksbehandlerInfo(it.ident).navn }
            val attestantEnhet = async { hentEnhet(it.enhet).navn ?: "NAV" }

            Attestant(attestantNavn.await(), attestantEnhet.await())
        }

        Pair(
            avsender,
            attestant
        )
    }

    private fun mapTilAvsender(enhet: Norg2Enhet, saksbehandlerNavn: String): Avsender {
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
            telefon = telefon,
            saksbehandler = saksbehandlerNavn
        )
    }
}