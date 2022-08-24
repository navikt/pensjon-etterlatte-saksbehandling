package no.nav.etterlatte.sak

interface SakService {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(person: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
}

class RealSakService(private val dao: SakDao) : SakService {
    override fun hentSaker(): List<Sak> {
        return dao.hentSaker()
    }

    override fun finnSaker(person: String): List<Sak> {
        return dao.finnSaker(person)
    }

    override fun slettSak(id: Long) {
        dao.slettSak(id)
    }

    override fun finnEllerOpprettSak(person: String, type: SakType): Sak {
        val eksisterendeSak = finnSak(person, type)
        return eksisterendeSak ?: dao.opprettSak(person, type)
    }

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSaker(person).find { it.sakType == type }
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id)
    }
}