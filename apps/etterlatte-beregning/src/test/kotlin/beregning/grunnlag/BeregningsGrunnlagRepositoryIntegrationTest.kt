package beregning.grunnlag

import io.mockk.clearAllMocks
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlagRepository
import no.nav.etterlatte.beregning.grunnlag.Institusjonsopphold
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
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
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRepositoryIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var repository: BeregningsGrunnlagRepository
    private lateinit var dataSource: DataSource

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
    fun `Opprettelse fungere`() {
        val id = UUID.randomUUID()

        val soeskenMedIBeregning = listOf(SoeskenMedIBeregning(STOR_SNERK, true))
        val institusjonsopphold = Institusjonsopphold(false)

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now()
                ),
                soeskenMedIBeregning,
                institusjonsopphold
            )
        )

        val result = repository.finnGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(soeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(institusjonsopphold, result?.institusjonsopphold)
    }

    @Test
    fun `Oppdatering fungere`() {
        val id = UUID.randomUUID()

        val initialSoeskenMedIBeregning = listOf(SoeskenMedIBeregning(STOR_SNERK, true))
        val oppdatertSoeskenMedIBeregning = listOf(
            SoeskenMedIBeregning(STOR_SNERK, true),
            SoeskenMedIBeregning(TRIVIELL_MIDTPUNKT, true)
        )

        val initialInstitusjonsopphold = Institusjonsopphold(false)
        val oppdatertInstitusjonsopphold = Institusjonsopphold(true)

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now()
                ),
                initialSoeskenMedIBeregning,
                initialInstitusjonsopphold
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
                oppdatertInstitusjonsopphold
            )
        )

        val result = repository.finnGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(oppdatertSoeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(oppdatertInstitusjonsopphold, result?.institusjonsopphold)
        assertEquals("Z654321", result?.kilde?.ident)
    }

    private companion object {
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
        val TRIVIELL_MIDTPUNKT = Folkeregisteridentifikator.of("19040550081")
    }
}