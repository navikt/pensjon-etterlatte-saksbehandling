package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import org.slf4j.LoggerFactory

interface BrukerService {
    fun finnEnhetForPersonOgTema(
        fnr: String,
        tema: String,
        saktype: SakType,
    ): ArbeidsFordelingEnhet

    suspend fun finnNavkontorForPerson(
        fnr: String,
        saktype: SakType,
    ): Navkontor
}

val sikkerLogg = sikkerlogger()

class BrukerServiceImpl(
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val norg2Klient: Norg2Klient,
) : BrukerService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun finnNavkontorForPerson(
        fnr: String,
        saktype: SakType,
    ): Navkontor {
        val tilknytning = pdltjenesterKlient.hentGeografiskTilknytning(fnr, saktype)

        return when {
            tilknytning.ukjent -> {
                Navkontor(navn = "Ukjent kontor", enhetNr = "ukjent enhetsnummer")
            }

            else -> {
                val geografiskTilknytning = tilknytning.geografiskTilknytning()
                when {
                    tilknytning.harBareLandTilknytning() -> {
                        if (tilknytning.land!! == "NO") {
                            Navkontor(navn = "Ukjent kontor, men har Norge som landkode", enhetNr = "ukjent enhetsnummer")
                        } else {
                            Navkontor(navn = "Utlandssak - ikke tilknyttet et navkontor", enhetNr = Enhet.UTLAND.enhetNr)
                        }
                    }
                    geografiskTilknytning == null -> {
                        Navkontor(navn = "Utlandssak - ingen geografisk område", enhetNr = Enhet.UTLAND.enhetNr)
                    }
                    else -> {
                        norg2Klient.hentNavkontorForOmraade(geografiskTilknytning)
                    }
                }
            }
        }
    }

    private fun GeografiskTilknytning.harBareLandTilknytning() = bydel == null && kommune == null && land != null

    override fun finnEnhetForPersonOgTema(
        fnr: String,
        tema: String,
        saktype: SakType,
    ): ArbeidsFordelingEnhet {
        val tilknytning = pdltjenesterKlient.hentGeografiskTilknytning(fnr, saktype)

        return when {
            tilknytning.ukjent -> {
                ArbeidsFordelingEnhet(
                    Enhet.defaultEnhet.navn,
                    Enhet.defaultEnhet,
                )
            }

            tilknytning.harBareLandTilknytning() -> {
                if (tilknytning.land!! == "NO") {
                    ArbeidsFordelingEnhet(
                        Enhet.defaultEnhet.navn,
                        Enhet.defaultEnhet,
                    )
                } else {
                    ArbeidsFordelingEnhet(
                        Enhet.UTLAND.navn,
                        Enhet.UTLAND,
                    )
                }
            }
            else -> {
                val geografiskTilknytning = tilknytning.geografiskTilknytning()
                when (geografiskTilknytning) {
                    null -> {
                        ArbeidsFordelingEnhet(
                            Enhet.UTLAND.navn,
                            Enhet.UTLAND,
                        )
                    }
                    else -> {
                        val finnEnhetForTemaOgOmraade =
                            finnEnhetForTemaOgOmraade(ArbeidsFordelingRequest(tema = tema, geografiskOmraade = geografiskTilknytning))
                        logger.info("Fant enhet fra norg $finnEnhetForTemaOgOmraade")
                        finnEnhetForTemaOgOmraade
                    }
                }
            }
        }
    }

    private fun finnEnhetForTemaOgOmraade(arbeidsFordelingRequest: ArbeidsFordelingRequest) =
        norg2Klient.hentArbeidsfordelingForOmraadeOgTema(arbeidsFordelingRequest).firstOrNull()
            ?: throw IngenEnhetFunnetException(arbeidsFordelingRequest)
}

open class EnhetException(
    override val message: String,
) : Exception(message)

class IngenEnhetFunnetException(
    val arbeidsFordelingRequest: ArbeidsFordelingRequest,
) : EnhetException(
        message = "Ingen enheter funnet for tema ${arbeidsFordelingRequest.tema} og omraade ${arbeidsFordelingRequest.geografiskOmraade}",
    )
