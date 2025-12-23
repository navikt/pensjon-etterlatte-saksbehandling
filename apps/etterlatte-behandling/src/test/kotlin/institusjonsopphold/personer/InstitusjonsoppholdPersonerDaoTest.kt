package no.nav.etterlatte.institusjonsopphold.personer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.User
import no.nav.etterlatte.institusjonsopphold.model.Institusjonsopphold
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.random.Random
import kotlin.use

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class InstitusjonsoppholdPersonerDaoTest(
    val dataSource: DataSource,
) {
    private val dao = InstitusjonsoppholdPersonerDao(ConnectionAutoclosingTest(dataSource))

    @BeforeAll
    fun setup() {
        nyKontekstMedBrukerOgDatabase(
            mockk<User>().also { every { it.name() } returns this::class.java.simpleName },
            dataSource,
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE institusjonsopphold_personer CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE institusjonsopphold_hentet CASCADE """).executeUpdate()
        }
    }

    @Test
    fun skalOppretteKjoeringTabell() {
        dao.lagreKjoering("123" to 42L)

        val ubehandledePersoner = dao.hentUbehandledePersoner(10)
        ubehandledePersoner.first() shouldBe "123"
    }

    @Test
    fun skalOppdatereStatus() {
        dao.lagreKjoering("123" to 1L)
        dao.lagreKjoering("456" to 2L)

        dao.markerSomFerdig(listOf("123"))

        val ubehandledePersoner = dao.hentUbehandledePersoner(10)
        ubehandledePersoner.first() shouldBe "456"
    }

    @Test
    fun `skal lagre opphold`() {
        val opphold =
            Institusjonsopphold(
                oppholdId = Random.nextLong(),
                institusjonstype = "fengsel",
                startdato = LocalDate.now().minusDays(6),
                faktiskSluttdato = null,
                forventetSluttdato = LocalDate.now().plusMonths(18),
                institusjonsnavn = "Feng Shel fengsel",
                organisasjonsnummer = "957838938",
            )
        dao.lagreInstitusjonsopphold(
            "123",
            opphold,
        )

        with(
            dao
                .hentInstitusjonsopphold("123")
                .single(),
        ) {
            oppholdId shouldBe opphold.oppholdId
            institusjonstype shouldBe opphold.institusjonstype
            startdato shouldBe opphold.startdato
            faktiskSluttdato shouldBe opphold.faktiskSluttdato
            forventetSluttdato shouldBe opphold.forventetSluttdato
        }
    }
}
