package no.nav.etterlatte.sak

import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.IngenGeografiskOmraadeFunnetForEnhet
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory

enum class SakServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettMedEnhetId("pensjon-etterlatte.opprett-sak-med-enhet-id");

    override fun key() = key
}

interface SakService : PersonTilgangsSjekk {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean)
    fun sjekkOmFnrHarEnSakMedAdresseBeskyttelse(fnr: String): Boolean
    fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean
}

class RealSakService(
    private val dao: SakDao,
    private val pdlKlient: PdlKlient,
    private val norg2Klient: Norg2Klient,
    private val featureToggleService: FeatureToggleService
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentSaker(): List<Sak> {
        return dao.hentSaker()
    }

    private fun finnSakerForPerson(person: String) = dao.finnSaker(person)

    override fun finnSaker(person: String): List<Sak> {
        return inTransaction {
            finnSakerForPerson(person)
        }
    }

    override fun slettSak(id: Long) {
        dao.slettSak(id)
    }

    override fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean) {
        inTransaction {
            dao.markerSakerMedSkjerming(sakIder, skjermet)
        }
    }

    override fun sjekkOmFnrHarEnSakMedAdresseBeskyttelse(fnr: String): Boolean {
        val sakIder = this.finnSaker(fnr).map { it.id }
        return inTransaction {
            dao.enAvSakeneHarAdresseBeskyttelse(sakIder)
        }
    }

    override fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean {
        return inTransaction {
            dao.enAvSakeneHarAdresseBeskyttelse(listOf(sakId))
        }
    }

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        bruker: Saksbehandler
    ): Boolean {
        return !this.sjekkOmFnrHarEnSakMedAdresseBeskyttelse(foedselsnummer.value)
    }

    override fun finnEllerOpprettSak(person: String, type: SakType) =
        finnSak(person, type) ?: dao.opprettSak(
            person,
            type,
            finnEnhetForPersonOgTema(person, type.tema)?.enhetNr
        )

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPerson(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }

    private fun finnEnhetForTemaOgOmraade(tema: String, omraade: String) =
        norg2Klient.hentEnheterForOmraade(tema, omraade).firstOrNull()
            .also { logger.info("Enhet for $tema, $omraade was $it") } ?: throw IngenEnhetFunnetException(omraade, tema)

    private fun finnEnhetForPersonOgTema(person: String, tema: String): ArbeidsFordelingEnhet? {
        logger.info("Checking feature toggle service")
        if (featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false)) {
            logger.info("Feature toggle service enabled")

            val tilknytning = pdlKlient.hentGeografiskTilknytning(person)

            logger.info("Tilknytning $tilknytning")

            val geografiskTilknytning = tilknytning.geografiskTilknytning()

            logger.info("Geografisk Tilknytning $geografiskTilknytning")

            return when {
                tilknytning.ukjent -> ArbeidsFordelingEnhet(Enheter.DEFAULT.navn, Enheter.DEFAULT.enhetNr)
                geografiskTilknytning == null -> throw IngenGeografiskOmraadeFunnetForEnhet(
                    Folkeregisteridentifikator.of(person),
                    tema
                ).also {
                    logger.warn(it.message)
                }

                else -> finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
            }
        } else {
            logger.info("Feature toggle service disabled")

            return null
        }
    }
}