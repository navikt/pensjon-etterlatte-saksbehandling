package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.adresse.saksbehandler.SaksbehandlerKlient
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.mottakerFraAdresse
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.slf4j.LoggerFactory

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val saksbehandlerKlient: SaksbehandlerKlient,
    private val regoppslagKlient: RegoppslagKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

    suspend fun hentAvsender(
        request: AvsenderRequest,
        bruker: BrukerTokenInfo,
    ): Avsender =
        coroutineScope {
            logger.info("Henter avsendere og enhet: $request")

            val saksbehandlerNavn = async { hentSaksbehandlerNavn(request.saksbehandlerIdent, bruker) }

            val saksbehandlerEnhet =
                async {
                    norg2Klient.hentEnhet(request.sakenhet)
                }

            val attestantNavn =
                async {
                    request.attestantIdent?.let { hentSaksbehandlerNavn(it, bruker) }
                }

            mapTilAvsender(saksbehandlerEnhet.await(), saksbehandlerNavn.await(), attestantNavn.await())
        }

    private suspend fun hentSaksbehandlerNavn(
        ident: String,
        bruker: BrukerTokenInfo,
    ) = if (ident == Fagsaksystem.EY.navn || ident.contains("gcp:etterlatte:")) {
        null
    } else {
        saksbehandlerKlient.hentSaksbehandlerNavn(ident, bruker)
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
