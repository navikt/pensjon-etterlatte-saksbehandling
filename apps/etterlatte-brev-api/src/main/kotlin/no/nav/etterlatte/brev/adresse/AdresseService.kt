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
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.sikkerLogg
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
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Mottaker> =
        with(personerISak) {
            val soekerFoedselsdato = pdltjenesterKlient.hentFoedselsdato(soeker.fnr.value, brukerTokenInfo)
            val soekerErMyndig = soekerFoedselsdato != null && soekerFoedselsdato.hentAlder() >= 18
            val soekerSkalHaBrev = soekerFoedselsdato == null || soekerFoedselsdato.hentAlder() >= 15

            val soekerMottaker: Mottaker? =
                soeker.fnr.value
                    .takeIf { soekerSkalHaBrev }
                    ?.let { hentMottakerAdresse(sakType, it) }

            // soekerAdresse er gjenlevende hvis det er en OMS saktype, men hvis det er BP må vi sjekke det opp
            val gjenlevendeMottaker: Mottaker? =
                gjenlevende
                    .firstOrNull { soeker.ansvarligeForeldre.contains(it) }
                    ?.takeIf { sakType == SakType.BARNEPENSJON && !soekerErMyndig }
                    ?.let { hentMottakerAdresse(sakType, it) }

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
                        null
                    }
                }

            // Legger til innsender som mottaker dersom innsender ikke er gjenlevende, soeker eller verge
            val innsenderMottaker: Mottaker? =
                innsender
                    ?.fnr
                    ?.value
                    ?.takeIf {
                        Folkeregisteridentifikator.isValid(it) && it !in
                            listOfNotNull(
                                soeker.fnr.value,
                                gjenlevende.firstOrNull(),
                                (verge as? Vergemaal)?.foedselsnummer?.value,
                            )
                    }?.let { hentMottakerAdresse(sakType, it) }

            val mottakereIPrioritertRekkefoelge: List<Mottaker> =
                if (soekerErMyndig || soekerFoedselsdato == null) {
                    listOfNotNull(vergeMottaker, soekerMottaker)
                } else {
                    listOfNotNull(vergeMottaker, gjenlevendeMottaker, innsenderMottaker, soekerMottaker)
                }

            val hovedmottaker =
                mottakereIPrioritertRekkefoelge.firstOrNull()
                    ?: ukjentMottaker().let { ukjentMottaker ->
                        logger.warn(
                            "Kunne ikke utlede mottakere for brev i sak $sakId. Dette betyr at vi gir en tom " +
                                "mottaker til saksbehandler og de må manuelt fylle ut mottakerinformasjon. " +
                                "Se sikkerlogg for hvilke personer vi har sett på i utledning av mottaker.",
                        )
                        sikkerLogg.warn(
                            "Kunne ikke finne en gyldig mottaker i sak $sakId. Personer vi tok inn i saken:" +
                                "$personerISak, soekerErMyndig=$soekerErMyndig, soekerFoedselsdato=$soekerFoedselsdato",
                        )
                        return@let ukjentMottaker
                    }
            val kopimottakere = mottakereIPrioritertRekkefoelge.drop(1)
            listOf(hovedmottaker.copy(type = MottakerType.HOVED)) + kopimottakere.map { it.copy(type = MottakerType.KOPI) }
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
    ): Mottaker {
        val regoppslag = regoppslagKlient.hentMottakerAdresse(sakType, ident)
        val fnr = Folkeregisteridentifikator.of(ident)
        return if (regoppslag == null) {
            tomMottaker(fnr, MottakerType.HOVED)
        } else {
            mottakerFraAdresse(
                fnr,
                regoppslag,
                MottakerType.HOVED,
            )
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

    private fun ukjentMottaker(): Mottaker =
        Mottaker(
            id = UUID.randomUUID(),
            navn = "UKJENT MOTTAKER",
            adresse = Adresse(adresseType = "", landkode = "", land = ""),
        )
}
