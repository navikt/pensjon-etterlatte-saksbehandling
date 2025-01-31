package no.nav.etterlatte.brev.adresse

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.adresse.saksbehandler.SaksbehandlerKlient
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.VERGENAVN_FOR_MOTTAKER
import no.nav.etterlatte.brev.model.mottakerFraAdresse
import no.nav.etterlatte.brev.model.tomMottaker
import no.nav.etterlatte.brev.pdl.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentAlder
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class AdresseService(
    private val norg2Klient: Norg2Klient,
    private val saksbehandlerKlient: SaksbehandlerKlient,
    private val regoppslagKlient: RegoppslagKlient,
    private val pdltjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentMottakere(
        sakType: SakType,
        personerISak: PersonerISak,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Mottaker> =
        with(personerISak) {
            val soekerFoedselsdato = pdltjenesterKlient.hentFoedselsdato(soeker.fnr.value, brukerTokenInfo)

            val soekerAdresse =
                if (soekerFoedselsdato == null || soekerFoedselsdato.hentAlder() > 18) {
                    hentMottakerAdresse(sakType, soeker.fnr.value, MottakerType.HOVED)
                } else if (soekerFoedselsdato.hentAlder() < 15) {
                    null
                } else {
                    hentMottakerAdresse(sakType, soeker.fnr.value, MottakerType.KOPI)
                }

            val vergeMottakerType =
                if (soekerAdresse?.type == MottakerType.HOVED) {
                    MottakerType.KOPI
                } else {
                    MottakerType.HOVED
                }

            val vergeAdresse: Mottaker? =
                when (verge) {
                    is Vergemaal -> {
                        logger.warn("Er verge, kan ikke ferdigstille uten å legge til adresse manuelt.")
                        tomVergeMottaker(MottakerFoedselsnummer(verge.foedselsnummer.value), vergeMottakerType)
                    }

                    is UkjentVergemaal -> {
                        logger.warn("Verge med ukjent vergemål, kan ikke ferdigstille uten å legge til adresse manuelt.")
                        tomVergeMottaker(type = vergeMottakerType)
                    }

                    else ->
                        if (
                            Folkeregisteridentifikator.isValid(innsender?.fnr?.value) &&
                            innsender!!.fnr.value != soeker.fnr.value
                        ) {
                            hentMottakerAdresse(sakType, innsender.fnr.value, vergeMottakerType)
                        } else {
                            null
                        }
                }

            listOfNotNull(soekerAdresse, vergeAdresse)
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

    @Deprecated(message = "Midlertidig løsning for å støtte gammel flyt. Burde hente navn fra Grunnlag eller PDL")
    suspend fun hentNavn(
        sakType: SakType,
        ident: String,
    ): String = hentMottakerAdresse(sakType, ident, MottakerType.KOPI).navn

    private suspend fun hentMottakerAdresse(
        sakType: SakType,
        ident: String,
        type: MottakerType,
    ): Mottaker {
        val regoppslag = regoppslagKlient.hentMottakerAdresse(sakType, ident)

        val fnr = Folkeregisteridentifikator.of(ident)

        return if (regoppslag == null) {
            tomMottaker(fnr, type)
        } else {
            mottakerFraAdresse(fnr, regoppslag, type)
        }
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
        val kontor = enhet.navn ?: "Nav"
        val telefon = enhet.kontaktinfo?.telefonnummer ?: ""

        return Avsender(
            kontor = kontor,
            telefonnummer = Telefonnummer(telefon),
            saksbehandler = saksbehandlerNavn,
            attestant = attestantNavn,
        )
    }

    private fun tomVergeMottaker(
        fnr: MottakerFoedselsnummer? = null,
        type: MottakerType,
    ): Mottaker =
        Mottaker(
            UUID.randomUUID(),
            navn = VERGENAVN_FOR_MOTTAKER,
            foedselsnummer = fnr,
            orgnummer = null,
            adresse = Adresse(adresseType = "", landkode = "", land = ""),
            type = type,
        )
}
