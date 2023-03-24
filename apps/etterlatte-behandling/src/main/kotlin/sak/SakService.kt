package no.nav.etterlatte.sak

import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.IngenGeografiskOmraadeFunnetForEnhet
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.klienter.PdlKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

interface SakService {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean)
    fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean
    fun sjekkOmSakHarAdresseBeskyttelse(sakId: Long): Boolean
}

class RealSakService(private val dao: SakDao, private val pdlKlient: PdlKlient, private val norg2Klient: Norg2Klient) :
    SakService {
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

    override fun sjekkOmSakHarAdresseBeskyttelse(fnr: String): Boolean {
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

    private fun finnEnhetForTemaOgOmraade(tema: String, omraade: String): ArbeidsFordelingEnhet {
        val enheter = norg2Klient.hentEnheterForOmraade(tema, omraade)

        return when {
            enheter.isEmpty() -> throw IngenEnhetFunnetException(omraade, tema).also { logger.warn(it.message) }
            else -> enheter.first()
        }
    }

    private fun finnEnhetForPersonOgTema(person: String, tema: String): ArbeidsFordelingEnhet? {
        val tilknytning = pdlKlient.hentGeografiskTilknytning(person)
        val geografiskTilknytning = tilknytning.geografiskTilknytning()

        return when {
            tilknytning.ukjent -> ArbeidsFordelingEnhet(Enheter.DEFAULT.navn, Enheter.DEFAULT.enhetNr)
            geografiskTilknytning == null -> throw IngenGeografiskOmraadeFunnetForEnhet(
                Foedselsnummer.of(person),
                tema
            ).also {
                logger.warn(it.message)
            }

            else -> finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
        }
    }

    override fun finnEllerOpprettSak(person: String, type: SakType) =
        finnSak(person, type) ?: dao.opprettSak(person, type, finnEnhetForPersonOgTema(person, type.tema)?.enhetNr)

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPerson(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }
}