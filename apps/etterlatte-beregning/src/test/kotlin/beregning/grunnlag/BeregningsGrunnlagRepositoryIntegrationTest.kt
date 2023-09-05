package beregning.grunnlag

import io.mockk.clearAllMocks
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRepositoryIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var repository: BeregningsGrunnlagRepository
    private lateinit var dataSource: DataSource

    private val foerstePeriodeFra = LocalDate.of(2022, 8, 1)

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
        dataSource.migrate()

        repository = BeregningsGrunnlagRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        dataSource.connection.use {
            it.prepareStatement(""" TRUNCATE bp_beregningsgrunnlag""").execute()
        }
    }

    @Test
    fun `Opprettelse fungerer`() {
        val id = UUID.randomUUID()

        val soeskenMedIBeregning = listOf(SoeskenMedIBeregning(STOR_SNERK, true)).somPeriodisertGrunnlag()
        val institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD)
                )
            )

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now()
                ),
                soeskenMedIBeregning,
                institusjonsoppholdBeregningsgrunnlag
            )
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(soeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(institusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
    }

    @Test
    fun `Oppdatering fungerer`() {
        val id = UUID.randomUUID()

        val initialSoeskenMedIBeregning = listOf(SoeskenMedIBeregning(STOR_SNERK, true)).somPeriodisertGrunnlag()
        val oppdatertSoeskenMedIBeregning = listOf(
            SoeskenMedIBeregning(STOR_SNERK, true),
            SoeskenMedIBeregning(TRIVIELL_MIDTPUNKT, true)
        ).somPeriodisertGrunnlag()

        val initialInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD)
                )
            )
        val oppdatertInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG)
                )
            )

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now()
                ),
                initialSoeskenMedIBeregning,
                initialInstitusjonsoppholdBeregningsgrunnlag
            )
        )

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now()
                ),
                oppdatertSoeskenMedIBeregning,
                oppdatertInstitusjonsoppholdBeregningsgrunnlag
            )
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(oppdatertSoeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(oppdatertInstitusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
        assertEquals("Z654321", result?.kilde?.ident)
    }

    @Test
    fun `skal håndtere at institusjonsopphold er null`() {
        val id = UUID.randomUUID()

        val oppdatertSoeskenMedIBeregning = listOf(
            SoeskenMedIBeregning(STOR_SNERK, true),
            SoeskenMedIBeregning(TRIVIELL_MIDTPUNKT, true)
        ).somPeriodisertGrunnlag()

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now()
                ),
                oppdatertSoeskenMedIBeregning,
                emptyList()
            )
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)
    }

    private companion object {
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
        val TRIVIELL_MIDTPUNKT = Folkeregisteridentifikator.of("19040550081")
    }

    private fun List<SoeskenMedIBeregning>.somPeriodisertGrunnlag(
        periodeFra: LocalDate = foerstePeriodeFra,
        periodeTil: LocalDate? = null
    ): List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> {
        return listOf(
            GrunnlagMedPeriode(
                fom = periodeFra,
                tom = periodeTil,
                data = this
            )
        )
    }
}