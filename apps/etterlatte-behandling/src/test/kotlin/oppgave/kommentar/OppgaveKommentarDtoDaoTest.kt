package no.nav.etterlatte.oppgave.kommentar

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.OppgaveKommentarDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class OppgaveKommentarDtoDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var oppgaveKommentarDao: OppgaveKommentarDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var sak: Sak

    @BeforeAll
    fun beforeAll() {
        oppgaveKommentarDao = OppgaveKommentarDaoImpl(ConnectionAutoclosingTest(dataSource))
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )

        sak =
            sakSkrivDao.opprettSak(
                fnr = "en bruker",
                type = SakType.OMSTILLINGSSTOENAD,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
    }

    @BeforeEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE oppgave_kommentar CASCADE;").execute()
        }
    }

    @Test
    fun `skal lagre og hente kommentarer for oppgave`() {
        val oppgaveId = UUID.randomUUID()

        for (i in 1..2) {
            oppgaveKommentarDao.opprettKommentar(
                OppgaveKommentarDto(
                    sakId = sak.id,
                    oppgaveId = oppgaveId,
                    saksbehandler = OppgaveSaksbehandler(ident = "abv123", navn = null),
                    kommentar = "kommentar " + i,
                    tidspunkt = Tidspunkt.now(),
                ),
            )
        }

        val oppgaveKommentar = oppgaveKommentarDao.hentKommentarer(oppgaveId)

        oppgaveKommentar!!.size shouldBe 2

        with(oppgaveKommentar.first()) {
            kommentar shouldBe "kommentar 1"
            tidspunkt shouldNotBe null
        }

        with(oppgaveKommentar.last()) {
            kommentar shouldBe "kommentar 2"
        }
    }
}
