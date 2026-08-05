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
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveFristFilter
import no.nav.etterlatte.libs.common.oppgave.OppgaveOrderBy
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandlerFilter
import no.nav.etterlatte.libs.common.oppgave.OppgaveSoekRequest
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoSoekTest(
    val dataSource: DataSource,
) {
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var sakSkrivDao: SakSkrivDao

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
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

    private val innloggetIdent = "Z123456"

    private fun soek(
        enheter: List<Enhetsnummer> = listOf(Enheter.AALESUND.enhetNr),
        request: OppgaveSoekRequest = OppgaveSoekRequest(),
    ) = oppgaveDao.soekOppgaver(enheter, request, innloggetIdent)

    @Test
    fun `filtrerer på statuser`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveNy = lagNyOppgave(sak)
        val oppgaveFerdigstilt = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveNy)
        oppgaveDao.opprettOppgave(oppgaveFerdigstilt)
        oppgaveDao.endreStatusPaaOppgave(oppgaveFerdigstilt.id, Status.FERDIGSTILT)

        val resultat = soek(request = OppgaveSoekRequest(statuser = listOf(Status.NY)))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(Status.NY, resultat.oppgaver[0].status)
    }

    @Test
    fun `filtrerer på typer`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sak, oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING))
        oppgaveDao.opprettOppgave(lagNyOppgave(sak, oppgaveType = OppgaveType.REVURDERING))
        oppgaveDao.opprettOppgave(lagNyOppgave(sak, oppgaveType = OppgaveType.OMGJOERING))

        val resultat =
            soek(
                request =
                    OppgaveSoekRequest(
                        typer =
                            listOf(
                                OppgaveType.FOERSTEGANGSBEHANDLING,
                                OppgaveType.REVURDERING,
                            ),
                    ),
            )

        assertEquals(2, resultat.totaltAntall)
        assertTrue(
            resultat.oppgaver.all {
                it.type in
                    listOf(
                        OppgaveType.FOERSTEGANGSBEHANDLING,
                        OppgaveType.REVURDERING,
                    )
            },
        )
    }

    @Test
    fun `filtrerer på sakType`() {
        val sakBP = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakOMS = sakSkrivDao.opprettSak("11223344557", SakType.OMSTILLINGSSTOENAD, Enheter.AALESUND.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakBP))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakOMS))

        val resultat = soek(request = OppgaveSoekRequest(sakType = SakType.BARNEPENSJON))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(SakType.BARNEPENSJON, resultat.oppgaver[0].sakType)
    }

    @Test
    fun `enheter fra bruker er sikkerhetsbegrensning - kan ikke se oppgaver utenfor egne enheter selv om man filtrerer på dem`() {
        val sakSteinkjer = sakSkrivDao.opprettSak("11223344557", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakSteinkjer))

        val resultat =
            soek(
                enheter = listOf(Enheter.AALESUND.enhetNr),
                request = OppgaveSoekRequest(enhet = Enheter.STEINKJER.enhetNr.enhetNr),
            )

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `enhet i request innsnevrer søk innenfor brukerens tillatte enheter`() {
        val sakAalesund = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakSteinkjer = sakSkrivDao.opprettSak("11223344557", SakType.BARNEPENSJON, Enheter.STEINKJER.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAalesund))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakSteinkjer))

        val resultat =
            soek(
                enheter = listOf(Enheter.AALESUND.enhetNr, Enheter.STEINKJER.enhetNr),
                request = OppgaveSoekRequest(enhet = Enheter.AALESUND.enhetNr.enhetNr),
            )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(Enheter.AALESUND.enhetNr, resultat.oppgaver[0].enhet)
    }

    @Test
    fun `filtrerer på saksbehandlerFilter TILDELT`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveTildelt = lagNyOppgave(sak)
        val oppgaveIkkeTildelt = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveTildelt)
        oppgaveDao.opprettOppgave(oppgaveIkkeTildelt)
        oppgaveDao.settNySaksbehandler(oppgaveTildelt.id, "Z111111")

        val resultat = soek(request = OppgaveSoekRequest(saksbehandlerFilter = OppgaveSaksbehandlerFilter.TILDELT))

        assertEquals(1, resultat.totaltAntall)
        assertEquals("Z111111", resultat.oppgaver[0].saksbehandler?.ident)
    }

    @Test
    fun `filtrerer på saksbehandlerFilter IKKE_TILDELT`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveTildelt = lagNyOppgave(sak)
        val oppgaveIkkeTildelt = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveTildelt)
        oppgaveDao.opprettOppgave(oppgaveIkkeTildelt)
        oppgaveDao.settNySaksbehandler(oppgaveTildelt.id, "Z111111")

        val resultat = soek(request = OppgaveSoekRequest(saksbehandlerFilter = OppgaveSaksbehandlerFilter.IKKE_TILDELT))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(null, resultat.oppgaver[0].saksbehandler)
    }

    @Test
    fun `filtrerer på saksbehandlerIdent`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveForIdent = lagNyOppgave(sak)
        val oppgaveAnnenIdent = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveForIdent)
        oppgaveDao.opprettOppgave(oppgaveAnnenIdent)
        oppgaveDao.settNySaksbehandler(oppgaveForIdent.id, "Z111111")
        oppgaveDao.settNySaksbehandler(oppgaveAnnenIdent.id, "Z222222")

        val resultat = soek(request = OppgaveSoekRequest(saksbehandlerIdent = "Z111111"))

        assertEquals(1, resultat.totaltAntall)
        assertEquals("Z111111", resultat.oppgaver[0].saksbehandler?.ident)
    }

    @Test
    fun `kunInnloggetBruker viser kun innlogget brukers oppgaver`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveTilInnlogget = lagNyOppgave(sak)
        val oppgaveTilAnnen = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveTilInnlogget)
        oppgaveDao.opprettOppgave(oppgaveTilAnnen)
        oppgaveDao.settNySaksbehandler(oppgaveTilInnlogget.id, innloggetIdent)
        oppgaveDao.settNySaksbehandler(oppgaveTilAnnen.id, "Z999999")

        val resultat = soek(request = OppgaveSoekRequest(kunInnloggetBruker = true))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(innloggetIdent, resultat.oppgaver[0].saksbehandler?.ident)
    }

    @Test
    fun `fristFilter HAR_PASSERT returnerer kun oppgaver med passert frist`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgavePassertFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().minus(1, ChronoUnit.DAYS))
        val oppgaveFremtidigFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(1, ChronoUnit.DAYS))
        val oppgaveUtenFrist = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgavePassertFrist)
        oppgaveDao.opprettOppgave(oppgaveFremtidigFrist)
        oppgaveDao.opprettOppgave(oppgaveUtenFrist)

        val resultat = soek(request = OppgaveSoekRequest(fristFilter = OppgaveFristFilter.HAR_PASSERT))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgavePassertFrist.id, resultat.oppgaver[0].id)
    }

    @Test
    fun `fristFilter MANGLER_FRIST returnerer kun oppgaver uten frist`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveMedFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(1, ChronoUnit.DAYS))
        val oppgaveUtenFrist = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveMedFrist)
        oppgaveDao.opprettOppgave(oppgaveUtenFrist)

        val resultat = soek(request = OppgaveSoekRequest(fristFilter = OppgaveFristFilter.MANGLER_FRIST))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgaveUtenFrist.id, resultat.oppgaver[0].id)
    }

    @Test
    fun `sakEllerFnr med 11 tegn filtrerer på fnr`() {
        val fnrSøkt = "11223344556"
        val fnrAnnen = "66554433221"
        val sakMedFnr = sakSkrivDao.opprettSak(fnrSøkt, SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakAnnen = sakSkrivDao.opprettSak(fnrAnnen, SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sakMedFnr))
        oppgaveDao.opprettOppgave(lagNyOppgave(sakAnnen))

        val resultat = soek(request = OppgaveSoekRequest(sakEllerFnr = fnrSøkt))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(fnrSøkt, resultat.oppgaver[0].fnr)
    }

    @Test
    fun `sakEllerFnr med sakId filtrerer på sak`() {
        val sak1 = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sak2 = sakSkrivDao.opprettSak("66554433221", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sak1))
        oppgaveDao.opprettOppgave(lagNyOppgave(sak2))

        val resultat = soek(request = OppgaveSoekRequest(sakEllerFnr = sak1.id.toString()))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(sak1.id, resultat.oppgaver[0].sakId)
    }

    @Test
    fun `paginering returnerer riktig side og antall`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        repeat(5) { oppgaveDao.opprettOppgave(lagNyOppgave(sak)) }

        val side0 = soek(request = OppgaveSoekRequest(side = 0, antall = 2))
        val side1 = soek(request = OppgaveSoekRequest(side = 1, antall = 2))
        val side2 = soek(request = OppgaveSoekRequest(side = 2, antall = 2))

        assertEquals(2, side0.oppgaver.size)
        assertEquals(2, side1.oppgaver.size)
        assertEquals(1, side2.oppgaver.size)
    }

    @Test
    fun `totaltAntall er korrekt uavhengig av paginering`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        repeat(7) { oppgaveDao.opprettOppgave(lagNyOppgave(sak)) }

        val side0 = soek(request = OppgaveSoekRequest(side = 0, antall = 3))
        val side1 = soek(request = OppgaveSoekRequest(side = 1, antall = 3))

        assertEquals(3, side0.oppgaver.size)
        assertEquals(3, side1.oppgaver.size)
        assertEquals(7L, side0.totaltAntall)
        assertEquals(7L, side1.totaltAntall)
    }

    @Test
    fun `sortering på frist ASC - tidligste frist først`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveSenFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(10, ChronoUnit.DAYS))
        val oppgaveTidligFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(1, ChronoUnit.DAYS))
        oppgaveDao.opprettOppgave(oppgaveSenFrist)
        oppgaveDao.opprettOppgave(oppgaveTidligFrist)

        val resultat = soek(request = OppgaveSoekRequest(orderBy = OppgaveOrderBy.FRIST, orderAsc = true))

        assertEquals(oppgaveTidligFrist.id, resultat.oppgaver[0].id)
        assertEquals(oppgaveSenFrist.id, resultat.oppgaver[1].id)
    }

    @Test
    fun `sortering på frist DESC - seneste frist først`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveSenFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(10, ChronoUnit.DAYS))
        val oppgaveTidligFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(1, ChronoUnit.DAYS))
        oppgaveDao.opprettOppgave(oppgaveSenFrist)
        oppgaveDao.opprettOppgave(oppgaveTidligFrist)

        val resultat = soek(request = OppgaveSoekRequest(orderBy = OppgaveOrderBy.FRIST, orderAsc = false))

        assertEquals(oppgaveSenFrist.id, resultat.oppgaver[0].id)
        assertEquals(oppgaveTidligFrist.id, resultat.oppgaver[1].id)
    }

    @Test
    fun `sortering med NULLS LAST - oppgaver uten frist kommer sist`() {
        val sak = sakSkrivDao.opprettSak("11223344556", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveMedFrist = lagNyOppgave(sak).copy(frist = Tidspunkt.now().plus(1, ChronoUnit.DAYS))
        val oppgaveUtenFrist = lagNyOppgave(sak)
        oppgaveDao.opprettOppgave(oppgaveMedFrist)
        oppgaveDao.opprettOppgave(oppgaveUtenFrist)

        val resultat = soek(request = OppgaveSoekRequest(orderBy = OppgaveOrderBy.FRIST, orderAsc = true))

        assertEquals(oppgaveMedFrist.id, resultat.oppgaver[0].id)
        assertEquals(oppgaveUtenFrist.id, resultat.oppgaver[1].id)
    }

    @Test
    fun `sortering på fnr ASC`() {
        val sak1 = sakSkrivDao.opprettSak("11111111111", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sak2 = sakSkrivDao.opprettSak("22222222222", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        oppgaveDao.opprettOppgave(lagNyOppgave(sak2))
        oppgaveDao.opprettOppgave(lagNyOppgave(sak1))

        val resultat = soek(request = OppgaveSoekRequest(orderBy = OppgaveOrderBy.FNR, orderAsc = true))

        assertEquals("11111111111", resultat.oppgaver[0].fnr)
        assertEquals("22222222222", resultat.oppgaver[1].fnr)
    }
}
