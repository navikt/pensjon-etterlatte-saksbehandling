package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.navansatt.NavansattKlient
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Telefonnummer

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val navansattKlient: NavansattKlient,
    private val regoppslagKlient: RegoppslagKlient,
) {
    suspend fun hentMottakerAdresse(ident: String): Mottaker {
        val regoppslagResponse = regoppslagKlient.hentMottakerAdresse(ident)

        return Mottaker.fra(
            Folkeregisteridentifikator.of(ident),
            regoppslagResponse,
        )
    }

    suspend fun hentAvsender(
        sak: Sak,
        saksbehandlerIdent: String,
    ): Avsender =
        coroutineScope {
            val saksbehandlerNavn = async { hentSaksbehandlerNavn(saksbehandlerIdent) }

            val saksbehandlerEnhet = async { hentEnhet(sak.enhet) }

            mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await(), attestantNavn = null)
        }

    suspend fun hentAvsender(vedtak: ForenkletVedtak): Avsender =
        coroutineScope {
            val saksbehandlerNavn = async { hentSaksbehandlerNavn(vedtak.saksbehandlerIdent) }

            val saksbehandlerEnhet =
                async {
                    hentEnhet(vedtak.sakenhet)
                }

            val attestantNavn =
                async {
                    vedtak.attestantIdent?.let { hentSaksbehandlerNavn(it) }
                }

            mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await(), attestantNavn.await())
        }

    private suspend fun hentSaksbehandlerNavn(navn: String) =
        if (navn == Fagsaksystem.EY.navn || navn.contains("gcp:etterlatte:")) {
            null
        } else {
            navansattKlient.hentSaksbehandlerInfo(navn).fornavnEtternavn
        }

    private suspend fun hentEnhet(navEnhetNr: String): Norg2Enhet = norg2Klient.hentEnhet(navEnhetNr)

    private fun mapTilAvsender(
        enhet: Norg2Enhet,
        saksbehandlerNavn: String?,
        attestantNavn: String?,
    ): Avsender {
        val kontor = enhet.navn ?: "NAV"
        val telefon = enhet.kontaktinfo?.telefonnummer ?: ""

        return Avsender(
            kontor = kontor,
            telefonnummer = Telefonnummer(telefon),
            saksbehandler = saksbehandlerNavn,
            attestant = attestantNavn,
        )
    }
}
