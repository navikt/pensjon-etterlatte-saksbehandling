package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.navansatt.NavansattKlient
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.mottakerFraAdresse
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Telefonnummer

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val navansattKlient: NavansattKlient,
    private val regoppslagKlient: RegoppslagKlient,
) {
    suspend fun hentMottakerAdresse(
        sakType: SakType,
        ident: String,
    ): Mottaker {
        val regoppslag = regoppslagKlient.hentMottakerAdresse(sakType, ident)

        val fnr = Folkeregisteridentifikator.of(ident)

        return if (regoppslag == null) {
            tomMottaker(fnr)
        } else {
            mottakerFraAdresse(fnr, regoppslag)
        }
    }

    suspend fun hentAvsender(request: AvsenderRequest): Avsender =
        coroutineScope {
            val saksbehandlerNavn = async { hentSaksbehandlerNavn(request.saksbehandlerIdent) }

            val saksbehandlerEnhet =
                async {
                    norg2Klient.hentEnhet(request.sakenhet.enhetNr)
                }

            val attestantNavn =
                async {
                    request.attestantIdent?.let { hentSaksbehandlerNavn(it) }
                }

            mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await(), attestantNavn.await())
        }

    private suspend fun hentSaksbehandlerNavn(navn: String) =
        if (navn == Fagsaksystem.EY.navn || navn.contains("gcp:etterlatte:")) {
            null
        } else {
            navansattKlient.hentSaksbehandlerInfo(navn).fornavnEtternavn
        }

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

data class AvsenderRequest(
    val saksbehandlerIdent: String,
    val sakenhet: Enhet,
    val attestantIdent: String? = null,
)
