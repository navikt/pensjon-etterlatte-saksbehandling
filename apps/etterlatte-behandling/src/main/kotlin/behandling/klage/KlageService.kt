package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageHendelseType
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.sak.SakDao
import java.util.*

interface KlageService {
    fun opprettKlage(sakId: Long): Klage
    fun hentKlage(id: UUID): Klage?
    fun hentKlagerISak(sakId: Long): List<Klage>
}

class KlageServiceImpl(
    private val klageDao: KlageDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveServiceNy: OppgaveServiceNy
) : KlageService {
    override fun opprettKlage(sakId: Long): Klage {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        val klage = Klage.ny(sak)
        klageDao.lagreKlage(klage)

        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse = klage.id.toString(),
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.EKSTERN,
            oppgaveType = OppgaveType.KLAGE,
            merknad = null
        )

        hendelseDao.klageHendelse(
            klageId = klage.id,
            sakId = klage.sak.id,
            hendelse = KlageHendelseType.OPPRETTET,
            inntruffet = Tidspunkt.now(),
            saksbehandler = null,
            kommentar = null,
            begrunnelse = null
        )

        return klage
    }

    override fun hentKlage(id: UUID): Klage? {
        return klageDao.hentKlage(id)
    }

    override fun hentKlagerISak(sakId: Long): List<Klage> {
        return klageDao.hentKlagerISak(sakId)
    }
}