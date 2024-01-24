package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.maskerFnr
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
                Navkontor(navn = "UKjent kontor", enhetNr = "ukjent enhetsnummer")
            }

            else -> {
                val geografiskTilknytning = tilknytning.geografiskTilknytning()
                when {
                    tilknytning.harBareLandTilknytning() -> {
                        if (tilknytning.land!! == "NO") {
                            Navkontor(navn = "UKjent kontor, men har Norge som landkode", enhetNr = "ukjent enhetsnummer")
                        }
                        Navkontor(navn = "Utlandssak - ikke tilknyttet et navkontor", enhetNr = Enheter.UTLAND.enhetNr)
                    }
                    geografiskTilknytning == null -> {
                        Navkontor(navn = "Utlandssak - ingen geografisk område", enhetNr = Enheter.UTLAND.enhetNr)
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
            tilknytning.ukjent ->
                ArbeidsFordelingEnhet(
                    Enheter.defaultEnhet.navn,
                    Enheter.defaultEnhet.enhetNr,
                )

            tilknytning.harBareLandTilknytning() -> {
                if (tilknytning.land!! == "NO") {
                    ArbeidsFordelingEnhet(
                        Enheter.defaultEnhet.navn,
                        Enheter.defaultEnhet.enhetNr,
                    )
                } else {
                    ArbeidsFordelingEnhet(
                        Enheter.UTLAND.navn,
                        Enheter.UTLAND.navn,
                    )
                }
            }
            else -> {
                logger.warn("Fant ikke geografisk omraade for ${fnr.maskerFnr()} og tema $tema")
                val geografiskTilknytning = tilknytning.geografiskTilknytning()
                when (geografiskTilknytning) {
                    null -> {
                        ArbeidsFordelingEnhet(
                            Enheter.UTLAND.navn,
                            Enheter.UTLAND.enhetNr,
                        )
                    }
                    else -> {
                        finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
                    }
                }
            }
        }
    }

    private fun finnEnhetForTemaOgOmraade(
        tema: String,
        omraade: String,
    ) = norg2Klient.hentEnheterForOmraade(tema, omraade).firstOrNull() ?: throw IngenEnhetFunnetException(omraade, tema)
}
