package no.nav.etterlatte.oppgave

import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakTilgangDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoTest {
    private val dataSource = DatabaseExtension.dataSource()
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var sakDao: SakDao
    private lateinit var saktilgangDao: SakTilgangDao

    @BeforeAll
    fun beforeAll() {
        val connection = dataSource.connection
        oppgaveDao = OppgaveDaoImpl { connection }
        sakDao = SakDao { connection }
        saktilgangDao = SakTilgangDao(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))

        val hentOppgaver = oppgaveDao.hentOppgaver(OppgaveType.entries)
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `opprett oppgave av type ATTESTERING`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund, OppgaveKilde.BEHANDLING, OppgaveType.ATTESTERING)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val hentOppgaver = oppgaveDao.hentOppgaver(OppgaveType.entries)
        assertEquals(1, hentOppgaver.size)
        assertEquals(OppgaveType.ATTESTERING, hentOppgaver[0].type)
    }

    @Test
    fun `kan tildelesaksbehandler`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val nySaksbehandler = "nysaksbehandler"
        oppgaveDao.settNySaksbehandler(oppgaveNy.id, nySaksbehandler)
        val hentOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandler, hentOppgave?.saksbehandler)
        assertEquals(Status.UNDER_BEHANDLING, hentOppgave?.status)
    }

    @Test
    fun `Skal ikke kunne hente adressebeskyttede oppgaver fra vanlig hentoppgaver`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        sakDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val sakutenbeskyttelse = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveUtenbeskyttelse = lagNyOppgave(sakutenbeskyttelse)
        oppgaveDao.opprettOppgave(oppgaveUtenbeskyttelse)

        val hentetOppgave = oppgaveDao.hentOppgaver(OppgaveType.entries)
        assertEquals(1, hentetOppgave.size)
    }

    @Test
    fun `Skal kunne hente adressebeskyttede oppgaver fra finnOppgaverForStrengtFortroligOgStrengtFortroligUtland`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        sakDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val hentetOppgave =
            oppgaveDao
                .finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(OppgaveType.entries)
        assertEquals(1, hentetOppgave.size)
    }

    fun lagNyOppgave(
        sak: Sak,
        oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
        oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = Status.NY,
        enhet = Enheter.AALESUND.enhetNr,
        sakId = sak.id,
        kilde = oppgaveKilde,
        referanse = "referanse",
        merknad = "merknad",
        opprettet = Tidspunkt.now(),
        sakType = sak.sakType,
        fnr = sak.ident,
        frist = null,
        type = oppgaveType,
    )
}
