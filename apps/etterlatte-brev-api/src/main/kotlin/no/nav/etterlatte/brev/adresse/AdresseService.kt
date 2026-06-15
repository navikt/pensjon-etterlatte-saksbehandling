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
            val soekerSkalHaBrev = soekerFoedselsdato == null || soekerFoedselsdato.hentAlder() >= 15

            val soekerMottaker: Mottaker? =
                if (soekerSkalHaBrev) hentMottakerAdresse(sakType, soeker.fnr.value) else null

            // soekerAdresse er gjenlevende hvis det er en OMS saktype, men hvis det er BP må vi sjekke det opp
            val gjenlevendeMottaker: Mottaker? =
                if (sakType == SakType.BARNEPENSJON &&
                    gjenlevende.isNotEmpty() &&
                    gjenlevende.first() !in listOfNotNull(soeker.fnr.value, innsender?.fnr?.value)
                    // TODO høre med Øyvind/Liv inger om gjenlevende skal være mottaker om soeker er over 18
                ) {
                    hentMottakerAdresse(sakType, gjenlevende.first())
                } else {
                    null
                }

            val vergeMottaker: Mottaker? =
                when (verge) {
                    is Vergemaal -> {
                        logger.warn("Er verge, kan ikke ferdigstille uten å legge til adresse manuelt.")
                        tomVergeMottaker(MottakerFoedselsnummer(verge.foedselsnummer.value))
                    }

                    is UkjentVergemaal -> {
                        logger.warn("Verge med ukjent vergemål, kan ikke ferdigstille uten å legge til adresse manuelt.")
                        tomVergeMottaker()
                    }

                    else -> {
                        if (
                            Folkeregisteridentifikator.isValid(innsender?.fnr?.value) &&
                            innsender!!.fnr.value != soeker.fnr.value
                        ) {
                            hentMottakerAdresse(sakType, innsender.fnr.value)
                        } else {
                            null
                        }
                    }
                }

            val mottakereIPrioritertRekkefoelge: List<Mottaker?> =
                if (soekerFoedselsdato == null || soekerFoedselsdato.hentAlder() >= 18) {
                    listOf(soekerMottaker, gjenlevendeMottaker, vergeMottaker)
                } else {
                    listOf(gjenlevendeMottaker, vergeMottaker, soekerMottaker)
                }

            // Default er .HOVED, og setter alle som kommer etter til .KOPI avhengig av prioritet som bestemt over
            mottakereIPrioritertRekkefoelge
                .filterNotNull()
                .mapIndexed { i, mottaker -> mottaker.copy(type = if (i == 0) MottakerType.HOVED else MottakerType.KOPI) }
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

    private suspend fun hentMottakerAdresse(
        sakType: SakType,
        ident: String,
    ): Mottaker = hentMottakerAdresse(sakType, ident, MottakerType.HOVED)

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

    private fun tomVergeMottaker(fnr: MottakerFoedselsnummer? = null): Mottaker =
        Mottaker(
            UUID.randomUUID(),
            navn = VERGENAVN_FOR_MOTTAKER,
            foedselsnummer = fnr,
            orgnummer = null,
            adresse = Adresse(adresseType = "", landkode = "", land = ""),
        )
}
