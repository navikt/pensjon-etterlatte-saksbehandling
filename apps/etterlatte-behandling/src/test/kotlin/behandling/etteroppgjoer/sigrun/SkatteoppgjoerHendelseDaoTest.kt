package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class SkatteoppgjoerHendelseDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var skatteoppgjoerHendelserDao: SkatteoppgjoerHendelserDao

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        skatteoppgjoerHendelserDao = SkatteoppgjoerHendelserDao(ConnectionAutoclosingTest(dataSource))

        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE skatteoppgjoer_hendelse_kjoringer CASCADE""").executeUpdate()
        }
    }

    @Test
    fun `skal lagre kjoering og hente siste kjoering`() {
        for (sekvensnummer in 1..5) {
            val kjoering =
                HendelserKjoering(
                    sisteSekvensnummer = sekvensnummer.toLong(),
                    antallHendelser = 100 + sekvensnummer,
                    antallBehandlet = 100 + sekvensnummer,
                    antallRelevante = 10 + sekvensnummer,
                )

            skatteoppgjoerHendelserDao.lagreKjoering(kjoering)
        }

        with(skatteoppgjoerHendelserDao.hentSisteKjoering()) {
            sisteSekvensnummer shouldBe 5
            antallHendelser shouldBe 105
            antallBehandlet shouldBe 105
            antallRelevante shouldBe 15
        }
    }
}
