package no.nav.etterlatte.oppgave

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var saktilgangDao: SakTilgangDao

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        val sakLesDao = SakLesDao(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { sakLesDao.hentSak(it) })
        saktilgangDao = SakTilgangDao(dataSource)
        val user = mockk<SaksbehandlerMedEnheterOgRoller>().also { every { it.name() } returns "SB1" }
        Kontekst.set(
            Context(
                user,
                DatabaseContextTest(dataSource),
                mockedSakTilgangDao(),
                null,
            ),
        )
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `Skal filtrere på saksbehandlerident`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveForIdentFiltrering = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveForIdentFiltrering)
        val saksbehandlerIdent = "Z994762"
        oppgaveDao.settNySaksbehandler(oppgaveForIdentFiltrering.id, saksbehandlerIdent)

        val oppgaveto = lagNyOppgave(sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr))
        oppgaveDao.opprettOppgave(oppgaveto)
        oppgaveDao.settNySaksbehandler(oppgaveto.id, "Z000000")

        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.AALESUND.enhetNr,
                ),
            ),
        )

        val alleOppgavera =
            oppgaveDao.hentOppgaver(
                listOf(Enheter.AALESUND.enhetNr),
                Status.entries.map { it.name },
                null,
            )
        println(alleOppgavera.size)

        val oppgaverKunForSaksbehandler =
            oppgaveDao.hentOppgaver(
                listOf(
                    Enheter.AALESUND.enhetNr,
                ),
                Status.entries.map { it.name },
                saksbehandlerIdent,
            )
        assertEquals(1, oppgaverKunForSaksbehandler.size)
        assertEquals(saksbehandlerIdent, oppgaverKunForSaksbehandler[0].saksbehandler?.ident)
        assertEquals(oppgaveForIdentFiltrering.id, oppgaverKunForSaksbehandler[0].id)

        val alleOppgaver =
            oppgaveDao.hentOppgaver(
                listOf(Enheter.AALESUND.enhetNr),
                Status.entries.map { it.name },
                null,
            )
        assertEquals(3, alleOppgaver.size)
    }

    @Test
    fun `Skal filtrere på status`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.endreStatusPaaOppgave(oppgaveNy.id, Status.FEILREGISTRERT)
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.AALESUND.enhetNr,
                ),
            ),
        )
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.AALESUND.enhetNr,
                ),
            ),
        )

        val hentOppgaver = oppgaveDao.hentOppgaver(listOf(Enheter.AALESUND.enhetNr), listOf(Status.NY.name))
        assertEquals(2, hentOppgaver.size)
    }

    @Test
    fun `Skal returnere alle statuser hvis status er tom eller satt til visAlle`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.endreStatusPaaOppgave(oppgaveNy.id, Status.FEILREGISTRERT)
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.AALESUND.enhetNr,
                ),
            ),
        )
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.AALESUND.enhetNr,
                ),
            ),
        )

        val hentOppgaver = oppgaveDao.hentOppgaver(listOf(Enheter.AALESUND.enhetNr), emptyList())
        assertEquals(3, hentOppgaver.size)

        val hentOppgaverVisAlle = oppgaveDao.hentOppgaver(listOf(Enheter.AALESUND.enhetNr), listOf(VISALLE))
        assertEquals(3, hentOppgaverVisAlle.size)
    }

    @Test
    fun `Vanlig bruker kan kun se oppgaver med brukers enhet AALESUND ikke andre enheter som ikke er strengt fortrolig`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.STEINKJER.enhetNr,
                ),
            ),
        )
        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sakSkrivDao.opprettSak(
                    "fnr",
                    SakType.BARNEPENSJON,
                    Enheter.PORSGRUNN.enhetNr,
                ),
            ),
        )

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                listOf(Enheter.AALESUND.enhetNr),
                Status.entries.map { it.name },
            )
        assertEquals(1, hentOppgaver.size)
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                listOf(Enheter.AALESUND.enhetNr),
                Status.entries.map { it.name },
            )
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `kan tildelesaksbehandler`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val nySaksbehandler = "nysaksbehandler"
        oppgaveDao.settNySaksbehandler(oppgaveNy.id, nySaksbehandler)
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandler, hentetOppgave?.saksbehandler?.ident)
        assertEquals(Status.NY, hentetOppgave?.status)
    }

    @Test
    fun `Kan lege til & fjerne forrige saksbehandler`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val nySaksbehandlerIdent = "nysaksbehandler"
        oppgaveDao.settNySaksbehandler(oppgaveNy.id, nySaksbehandlerIdent)
        oppgaveDao.settForrigeSaksbehandlerFraSaksbehandler(oppgaveNy.id)

        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandlerIdent, hentetOppgave?.forrigeSaksbehandlerIdent)

        oppgaveDao.fjernForrigeSaksbehandler(oppgaveNy.id)
        val fjernetForrigeSaksbehandler = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertNull(fjernetForrigeSaksbehandler!!.forrigeSaksbehandlerIdent)
    }

    @Test
    fun `kan sette oppgave paa vent`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.oppdaterStatusOgMerknad(oppgaveNy.id, "merknad", Status.PAA_VENT)
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(Status.PAA_VENT, hentetOppgave?.status)
    }

    @Test
    fun `kan endre enhet på oppgave`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.endreEnhetPaaOppgave(oppgaveNy.id, Enheter.PORSGRUNN.enhetNr)
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(Enheter.PORSGRUNN.enhetNr, hentetOppgave?.enhet)
    }

    @Test
    fun `Skal ikke kunne hente adressebeskyttede oppgaver fra vanlig hentoppgaver`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        sakSkrivDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val sakutenbeskyttelse = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveUtenbeskyttelse = lagNyOppgave(sakutenbeskyttelse)
        oppgaveDao.opprettOppgave(oppgaveUtenbeskyttelse)

        val hentetOppgave =
            oppgaveDao.hentOppgaver(
                listOf(Enheter.AALESUND.enhetNr),
                Status.entries.map { it.name },
            )
        assertEquals(1, hentetOppgave.size)
    }

    @Test
    fun `Skal kunne hente adressebeskyttede oppgaver fra finnOppgaverForStrengtFortroligOgStrengtFortroligUtland`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        sakSkrivDao.oppdaterAdresseBeskyttelse(sakAalesund.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val ikkeGradertSak = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val ikkeGradertOppgave = lagNyOppgave(ikkeGradertSak)
        oppgaveDao.opprettOppgave(ikkeGradertOppgave)

        val hentetOppgave = oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland()
        assertEquals(1, hentetOppgave.size)
    }

    @Test
    fun `Skal få false om oppgave med type ikke finnes`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val oppgaveFinnesIkke = oppgaveDao.oppgaveMedTypeFinnes(sakAalesund.id, OppgaveType.MANGLER_SOEKNAD)
        assertEquals(false, oppgaveFinnesIkke)
    }

    @Test
    fun `Skal få true om oppgave med type finnes`() {
        val sakAalesund = sakSkrivDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund, oppgaveType = OppgaveType.MANGLER_SOEKNAD)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val oppgaveFinnesIkke = oppgaveDao.oppgaveMedTypeFinnes(sakAalesund.id, OppgaveType.MANGLER_SOEKNAD)
        assertEquals(true, oppgaveFinnesIkke)
    }
}

fun lagNyOppgave(
    sak: Sak,
    oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
    oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
) = OppgaveIntern(
    id = UUID.randomUUID(),
    status = Status.NY,
    enhet = sak.enhet,
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
