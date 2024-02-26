package no.nav.etterlatte.oppgave

import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
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
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoTest(val dataSource: DataSource) {
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var sakDao: SakDao
    private lateinit var saktilgangDao: SakTilgangDao

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        sakDao = SakDao(ConnectionAutoclosingTest(dataSource))
        saktilgangDao = SakTilgangDao(dataSource)
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()
        Kontekst.set(
            Context(
                user,
                DatabaseContextTest(dataSource),
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
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveForIdentFiltrering = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveForIdentFiltrering)
        val saksbehandlerIdent = "Z994762"
        oppgaveDao.settNySaksbehandler(oppgaveForIdentFiltrering.id, saksbehandlerIdent)

        val oppgaveto = lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr))
        oppgaveDao.opprettOppgave(oppgaveto)
        oppgaveDao.settNySaksbehandler(oppgaveto.id, "Z000000")

        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)))

        val alleOppgavera =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                true,
                Status.entries.map {
                    it.name
                },
                null,
            )
        println(alleOppgavera.size)

        val oppgaverKunForSaksbehandler =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(
                    Enheter.AALESUND.enhetNr,
                ),
                false,
                Status.entries.map {
                    it.name
                },
                saksbehandlerIdent,
            )
        assertEquals(1, oppgaverKunForSaksbehandler.size)
        assertEquals(saksbehandlerIdent, oppgaverKunForSaksbehandler[0].saksbehandler?.ident)
        assertEquals(oppgaveForIdentFiltrering.id, oppgaverKunForSaksbehandler[0].id)

        val alleOppgaver =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                false,
                Status.entries.map {
                    it.name
                },
                null,
            )
        assertEquals(3, alleOppgaver.size)
    }

    @Test
    fun `Skal filtrere på status`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.endreStatusPaaOppgave(oppgaveNy.id, Status.FEILREGISTRERT)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)))

        val hentOppgaver = oppgaveDao.hentOppgaver(OppgaveType.entries, listOf(Enheter.AALESUND.enhetNr), false, listOf(Status.NY.name))
        assertEquals(2, hentOppgaver.size)
    }

    @Test
    fun `Skal returnere alle statuser hvis status er tom eller satt til visAlle`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.endreStatusPaaOppgave(oppgaveNy.id, Status.FEILREGISTRERT)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)))

        val hentOppgaver = oppgaveDao.hentOppgaver(OppgaveType.entries, listOf(Enheter.AALESUND.enhetNr), false, emptyList())
        assertEquals(3, hentOppgaver.size)

        val hentOppgaverVisAlle = oppgaveDao.hentOppgaver(OppgaveType.entries, listOf(Enheter.AALESUND.enhetNr), false, listOf(VISALLE))
        assertEquals(3, hentOppgaverVisAlle.size)
    }

    @Test
    fun `Vanlig bruker kan kun se oppgaver med brukers enhet AALESUND ikke andre enheter som ikke er strengt fortrolig`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)))

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                false,
                Status.entries.map { it.name },
            )
        assertEquals(1, hentOppgaver.size)
    }

    @Test
    fun `superbrukerbruker kan se alle oppgaver som ikke er strengt fortrolig`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)))

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                true,
                Status.entries.map { it.name },
            )
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `legg til oppgaver og hent oppgaver`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                false,
                Status.entries.map { it.name },
            )
        assertEquals(3, hentOppgaver.size)
    }

    @Test
    fun `opprett oppgave av type ATTESTERING`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund, OppgaveKilde.BEHANDLING, OppgaveType.ATTESTERING)
        oppgaveDao.opprettOppgave(oppgaveNy)

        val hentOppgaver =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                false,
                Status.entries.map { it.name },
            )
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
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(nySaksbehandler, hentetOppgave?.saksbehandler?.ident)
        assertEquals(Status.UNDER_BEHANDLING, hentetOppgave?.status)
    }

    @Test
    fun `kan sette oppgave paa vent`() {
        val sakAalesund = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sakAalesund)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.oppdaterStatusOgMerknad(oppgaveNy.id, "merknad", Status.PAA_VENT)
        val hentetOppgave = oppgaveDao.hentOppgave(oppgaveNy.id)
        assertEquals(Status.PAA_VENT, hentetOppgave?.status)
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

        val hentetOppgave =
            oppgaveDao.hentOppgaver(
                OppgaveType.entries,
                listOf(Enheter.AALESUND.enhetNr),
                false,
                Status.entries.map { it.name },
            )
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
    behandlingId = null,
)
