package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.navansatt.NavansattKlient
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.pensjon.brevbaker.api.model.Telefonnummer

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val navansattKlient: NavansattKlient,
    private val regoppslagKlient: RegoppslagKlient
) {
    suspend fun hentMottakerAdresse(ident: String): Mottaker {
        val regoppslagResponse = regoppslagKlient.hentMottakerAdresse(ident)

        return Mottaker.fra(
            Folkeregisteridentifikator.of(ident),
            regoppslagResponse
        )
    }

    suspend fun hentAvsender(vedtak: ForenkletVedtak): Avsender = coroutineScope {
        val saksbehandlerNavn = async {
            navansattKlient.hentSaksbehandlerInfo(vedtak.saksbehandlerIdent).fornavnEtternavn
        }

        val saksbehandlerEnhet = async {
            hentEnhet(vedtak.ansvarligEnhet)
        }

        val attestantNavn = async {
            vedtak.attestantIdent?.let { navansattKlient.hentSaksbehandlerInfo(it).fornavnEtternavn }
        }

        mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await(), attestantNavn.await())
    }

    private suspend fun hentEnhet(navEnhetNr: String): Norg2Enhet = norg2Klient.hentEnhet(navEnhetNr)

    private fun mapTilAvsender(
        enhet: Norg2Enhet,
        saksbehandlerNavn: String,
        attestantNavn: String?,
    ): Avsender {
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
            telefonnummer = Telefonnummer(telefon),
            saksbehandler = saksbehandlerNavn,
            attestant = attestantNavn
        )
    }
}