package oppgave

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.mockSaksbehandler
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporing
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoReguleringTest(
    val dataSource: DataSource,
) {
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing
    private lateinit var sakDao: SakDao
    private lateinit var oppgaveService: OppgaveService

    private lateinit var saksbehandlerInfoDao: SaksbehandlerInfoDao

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        oppgaveDaoMedEndringssporing = OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource))
        sakDao = SakDao(ConnectionAutoclosingTest(dataSource))
        oppgaveService = OppgaveService(oppgaveDaoMedEndringssporing, sakDao, mockk(), mockk())

        saksbehandlerInfoDao = SaksbehandlerInfoDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBruker(mockSaksbehandler("Z123456"))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `Skal tilbakestille oppgaver under attestering`() {
        val sakEn = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakTo = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakTre = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)

        val sakerTilRegulering = listOf(sakEn.id, sakTo.id)

        val oppgaveAttestert = lagOppgave(sakId = sakEn.id, status = Status.ATTESTERING)
        val oppgaveIkkeAttestert = lagOppgave(sakId = sakTo.id, status = Status.NY)
        val ikkeMedIRegulering = lagOppgave(sakId = sakTre.id, status = Status.ATTESTERING)

        oppgaveService.tilbakestillOppgaverUnderAttestering(sakerTilRegulering)

        oppgaveDao.hentOppgave(oppgaveAttestert.id)!!.status shouldBe Status.UNDER_BEHANDLING
        oppgaveDao.hentOppgave(oppgaveIkkeAttestert.id)!!.status shouldBe Status.NY
        oppgaveDao.hentOppgave(ikkeMedIRegulering.id)!!.status shouldBe Status.ATTESTERING
    }

    @Test
    fun `Setter forrige saksbehandler ved tilbakestilling`() {
        val sak = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val oppgaveAttestert = lagOppgave(sakId = sak.id, status = Status.UNDER_BEHANDLING)
        oppgaveDaoMedEndringssporing.oppdaterStatusOgMerknad(oppgaveAttestert.id, "", Status.ATTESTERING)
        oppgaveDao.settNySaksbehandler(oppgaveAttestert.id, "Ikke Ole")

        oppgaveService.tilbakestillOppgaverUnderAttestering(listOf(sak.id))

        oppgaveDao.hentOppgave(oppgaveAttestert.id)!!.let {
            it.status shouldBe Status.UNDER_BEHANDLING
            it.saksbehandler shouldBe OppgaveSaksbehandler("Ole", "Ole")
        }
    }

    private fun lagOppgave(
        status: Status = Status.UNDER_BEHANDLING,
        sakId: Long,
        saksbehandler: String = "Ole",
    ): OppgaveIntern {
        val oppgave =
            OppgaveIntern(
                id = UUID.randomUUID(),
                status = status,
                enhet = "",
                sakId = sakId,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.REVURDERING,
                saksbehandler = OppgaveSaksbehandler(saksbehandler),
                referanse = "",
                merknad = "",
                opprettet = Tidspunkt.now(),
                sakType = SakType.BARNEPENSJON,
                fnr = null,
                frist = null,
            )
        oppgaveDao.opprettOppgave(oppgave)
        saksbehandlerInfoDao.upsertSaksbehandlerNavn(SaksbehandlerInfo(saksbehandler, saksbehandler))
        return oppgave
    }
}
