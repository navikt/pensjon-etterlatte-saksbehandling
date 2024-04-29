package oppgave

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveDao
import no.nav.etterlatte.oppgave.OppgaveDaoImpl
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporing
import no.nav.etterlatte.oppgave.OppgaveDaoMedEndringssporingImpl
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveDaoReguleringTest(val dataSource: DataSource) {
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var oppgaveDaoMedEndringssporing: OppgaveDaoMedEndringssporing
    private lateinit var sakDao: SakDao

    @BeforeAll
    fun beforeAll() {
        oppgaveDao = OppgaveDaoImpl(ConnectionAutoclosingTest(dataSource))
        oppgaveDaoMedEndringssporing = OppgaveDaoMedEndringssporingImpl(oppgaveDao, ConnectionAutoclosingTest(dataSource))
        sakDao = SakDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave CASCADE;").execute()
        }
    }

    @Test
    fun `skal tilbakestille oppgaver under attestering`() {
        val sakEn = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakTo = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakTre = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)
        val sakFire = sakDao.opprettSak("fnr", SakType.BARNEPENSJON, Enheter.AALESUND.enhetNr)

        val sakerTilRegulering = Saker(listOf(sakEn, sakTo, sakTre))

        val oppgaveAttestertEn =
            lagOppgave(sakId = sakEn.id, status = Status.ATTESTERING).also {
                oppgaveDao.opprettOppgave(it)
            }
        val oppgaveAttestertTo =
            lagOppgave(sakId = sakTo.id, status = Status.ATTESTERING).also {
                oppgaveDao.opprettOppgave(it)
            }
        val oppgaveIkkeAttestert =
            lagOppgave(sakId = sakTre.id, status = Status.NY).also {
                oppgaveDao.opprettOppgave(it)
            }
        val ikkeMedIRegulering =
            lagOppgave(sakId = sakFire.id, status = Status.ATTESTERING).also {
                oppgaveDao.opprettOppgave(it)
            }

        oppgaveDaoMedEndringssporing.tilbakestillOppgaveUnderAttestering(sakerTilRegulering)

        oppgaveDao.hentOppgave(oppgaveAttestertEn.id)!!.status shouldBe Status.UNDER_BEHANDLING
        oppgaveDao.hentOppgave(oppgaveAttestertTo.id)!!.status shouldBe Status.UNDER_BEHANDLING

        oppgaveDao.hentOppgave(oppgaveIkkeAttestert.id)!!.status shouldBe Status.NY

        oppgaveDao.hentOppgave(ikkeMedIRegulering.id)!!.status shouldBe Status.ATTESTERING
    }

    private fun lagOppgave(
        status: Status = Status.UNDER_BEHANDLING,
        sakId: Long,
    ) = OppgaveIntern(
        id = UUID.randomUUID(),
        status = status,
        enhet = "",
        sakId = sakId,
        kilde = OppgaveKilde.BEHANDLING,
        type = OppgaveType.REVURDERING,
        saksbehandler = null,
        referanse = "",
        merknad = "",
        opprettet = Tidspunkt.now(),
        sakType = SakType.BARNEPENSJON,
        fnr = null,
        frist = null,
    )

    private fun lagSak(sakId: Long) =
        Sak(
            id = sakId,
            ident = "",
            sakType = SakType.BARNEPENSJON,
            enhet = "",
        )
}
