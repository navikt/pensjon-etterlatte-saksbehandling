package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.IngenGeografiskOmraadeFunnetForEnhet
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
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

class GeografiskTilknytningMangler : IkkeFunnetException(
    code = "BRUKER_MANGLER_GEOGRAFISKTILKNYTNING",
    detail = "Fant ikke geografisk tilknytning for bruker",
)

class BrukerServiceImpl(
    private val pdlKlient: PdlKlient,
    private val norg2Klient: Norg2Klient,
) : BrukerService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun finnNavkontorForPerson(
        fnr: String,
        saktype: SakType,
    ): Navkontor {
        val tilknytning = pdlKlient.hentGeografiskTilknytning(fnr, saktype)

        return when {
            tilknytning.ukjent -> {
                Navkontor(navn = "UKjent kontor", enhetNr = "ukjent enhetsnummer")
            }

            else -> {
                val geografiskTilknytning = tilknytning.geografiskTilknytning() ?: throw GeografiskTilknytningMangler()
                norg2Klient.hentNavkontorForOmraade(geografiskTilknytning)
            }
        }
    }

    override fun finnEnhetForPersonOgTema(
        fnr: String,
        tema: String,
        saktype: SakType,
    ): ArbeidsFordelingEnhet {
        val tilknytning = pdlKlient.hentGeografiskTilknytning(fnr, saktype)
        val geografiskTilknytning = tilknytning.geografiskTilknytning()

        return when {
            tilknytning.ukjent ->
                ArbeidsFordelingEnhet(
                    Enheter.defaultEnhet.navn,
                    Enheter.defaultEnhet.enhetNr,
                )

            geografiskTilknytning == null -> throw IngenGeografiskOmraadeFunnetForEnhet(
                Folkeregisteridentifikator.of(fnr),
                tema,
            ).also {
                logger.warn(it.message)
            }

            else -> finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
        }
    }

    private fun finnEnhetForTemaOgOmraade(
        tema: String,
        omraade: String,
    ) = norg2Klient.hentEnheterForOmraade(tema, omraade).firstOrNull() ?: throw IngenEnhetFunnetException(omraade, tema)
}
