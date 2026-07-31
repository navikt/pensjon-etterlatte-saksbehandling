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
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mockedSakTilgangDao
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
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
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

    @Test
    fun `GruppeId fungerer som forventet`() {
        val sak = sakSkrivDao.opprettSak("ident", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)

        repeat(10) {
            oppgaveDao.opprettOppgave(lagNyOppgave(sak, gruppeId = null))
        }

        val gruppeId = UUID.randomUUID().toString()

        repeat(3) {
            oppgaveDao.opprettOppgave(
                lagNyOppgave(
                    sak,
                    oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
                    gruppeId = gruppeId,
                ),
            )
        }

        oppgaveDao.opprettOppgave(
            lagNyOppgave(
                sak,
                oppgaveType = OppgaveType.REVURDERING,
                gruppeId = gruppeId,
            ),
        )

        val oppgaver = oppgaveDao.hentOppgaverForSakMedType(sak.id, OppgaveType.entries)
        assertEquals(14, oppgaver.size)

        val foerstegangsbehandlingGruppert =
            oppgaveDao.hentOppgaverForGruppeId(gruppeId, OppgaveType.FOERSTEGANGSBEHANDLING)
        assertEquals(3, foerstegangsbehandlingGruppert.size)

        val revurderingGruppert = oppgaveDao.hentOppgaverForGruppeId(gruppeId, OppgaveType.REVURDERING)
        assertEquals(1, revurderingGruppert.size)
    }

    @Test
    fun `GruppeId fungerer som forventet - henter ikke avsluttede oppgaver`() {
        val sak = sakSkrivDao.opprettSak("ident", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)

        val gruppeId = UUID.randomUUID().toString()
        oppgaveDao.opprettOppgave(lagNyOppgave(sak, gruppeId = gruppeId))

        val oppgaveAvbrutt =
            lagNyOppgave(sak, gruppeId = gruppeId).also {
                oppgaveDao.opprettOppgave(it)
            }
        val oppgaveFerdigstilt =
            lagNyOppgave(sak, gruppeId = gruppeId).also {
                oppgaveDao.opprettOppgave(it)
            }
        val oppgaveFeilregistrert =
            lagNyOppgave(sak, gruppeId = gruppeId).also {
                oppgaveDao.opprettOppgave(it)
            }

        val oppgaver = oppgaveDao.hentOppgaverForSakMedType(sak.id, OppgaveType.entries)
        assertEquals(4, oppgaver.size)

        val grupperteOppgaver = oppgaveDao.hentOppgaverForGruppeId(gruppeId, OppgaveType.FOERSTEGANGSBEHANDLING)
        assertEquals(4, grupperteOppgaver.size)

        oppgaveDao.endreStatusPaaOppgave(oppgaveAvbrutt.id, Status.AVBRUTT)
        oppgaveDao.endreStatusPaaOppgave(oppgaveFerdigstilt.id, Status.FERDIGSTILT)
        oppgaveDao.endreStatusPaaOppgave(oppgaveFeilregistrert.id, Status.FEILREGISTRERT)

        val grupperteOppgaverEtterEndretStatus = oppgaveDao.hentOppgaverForGruppeId(gruppeId, OppgaveType.FOERSTEGANGSBEHANDLING)
        assertEquals(1, grupperteOppgaverEtterEndretStatus.size)
    }
}

fun lagNyOppgave(
    sak: Sak,
    oppgaveKilde: OppgaveKilde = OppgaveKilde.BEHANDLING,
    oppgaveType: OppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
    gruppeId: String? = null,
) = OppgaveIntern(
    id = UUID.randomUUID(),
    status = Status.NY,
    enhet = sak.enhet,
    sakId = sak.id,
    kilde = oppgaveKilde,
    referanse = "referanse",
    gruppeId = gruppeId,
    merknad = "merknad",
    opprettet = Tidspunkt.now(),
    sakType = sak.sakType,
    fnr = sak.ident,
    frist = null,
    type = oppgaveType,
)
