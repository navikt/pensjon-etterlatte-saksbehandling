package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.sak.SakDao
import java.util.*

interface KlageService {
    fun opprettKlage(sakId: Long): Klage
    fun hentKlage(id: UUID): Klage?
    fun hentKlagerISak(sakId: Long): List<Klage>
}

class KlageServiceImpl(private val klageDao: KlageDao, private val sakDao: SakDao) : KlageService {
    override fun opprettKlage(sakId: Long): Klage {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")
        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)
        // Husk hendelser
        return klage
    }

    override fun hentKlage(id: UUID): Klage? {
        return klageDao.hentKlage(id)
    }

    override fun hentKlagerISak(sakId: Long): List<Klage> {
        return klageDao.hentKlagerISak(sakId)
    }
}