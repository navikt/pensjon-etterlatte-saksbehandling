package tilbakekreving.sporing

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.tilbakekreving.sporing.TilbakekrevingSporingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingSporingRepositoryTest {
    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: TilbakekrevingSporingRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = TilbakekrevingSporingRepository(dataSource.apply { migrate() })
    }

    @AfterAll
    fun afterAll() {
        postgres.stop()
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @Test
    fun `skal lagre ned mottatt kravgrunnlag`() {
        val fagsystemId = "2"
        val kravgrunnlagId = "1"
        val kravgrunnlagPayload = "<kravgrunnlag>payload</kravgrunnlag>"

        repository.lagreMottattKravgrunnlag(kravgrunnlagId, fagsystemId, kravgrunnlagPayload)

        val tilbakekrevingSporing = hentTilbakekrevingSporing(kravgrunnlagId)
        tilbakekrevingSporing?.id shouldNotBe null
        tilbakekrevingSporing?.opprettet shouldNotBe null
        tilbakekrevingSporing?.fagsystemId shouldBe fagsystemId
        tilbakekrevingSporing?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingSporing?.kravgrunnlagPayload shouldBe kravgrunnlagPayload
    }

    @Test
    fun `skal oppdatere kravgrunnlag med vedtak request og vedtak response`() {
        val fagsystemId = "2"
        val kravgrunnlagId = "1"
        val kravgrunnlagPayload = "<kravgrunnlag>payload</kravgrunnlag>"
        val vedtakRequest = "request_payload"
        val vedtakResponse = "response_payload"

        repository.lagreMottattKravgrunnlag(kravgrunnlagId, fagsystemId, kravgrunnlagPayload)
        repository.lagreTilbakekrevingsvedtakRequest(kravgrunnlagId, vedtakRequest)
        repository.lagreTilbakekrevingsvedtakResponse(kravgrunnlagId, vedtakResponse)

        val tilbakekrevingSporing = hentTilbakekrevingSporing(kravgrunnlagId)

        tilbakekrevingSporing?.endret shouldNotBe null
        tilbakekrevingSporing?.tilbakekrevingsvedtakRequest shouldBe vedtakRequest
        tilbakekrevingSporing?.tilbakekrevingsvedtakResponse shouldBe vedtakResponse
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE tilbakekreving_sporing").apply { execute() } }
    }

    private fun hentTilbakekrevingSporing(kravgrunnlagId: String): TilbakekrevingSporing? {
        dataSource.connection.use {
            val stmt =
                it.prepareStatement("SELECT * FROM tilbakekreving_sporing WHERE kravgrunnlag_id = ?").apply {
                    setString(1, kravgrunnlagId)
                }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingSporing(
                    id = getString("id"),
                    opprettet = getString("opprettet"),
                    endret = getString("endret"),
                    fagsystemId = getString("fagsystem_id"),
                    kravgrunnlagId = getString("kravgrunnlag_id"),
                    kravgrunnlagPayload = getString("kravgrunnlag_payload"),
                    tilbakekrevingsvedtakRequest = getString("tilbakekrevingsvedtak_request"),
                    tilbakekrevingsvedtakResponse = getString("tilbakekrevingsvedtak_response"),
                )
            }
        }
    }

    private data class TilbakekrevingSporing(
        val id: String,
        val opprettet: String,
        val endret: String?,
        val fagsystemId: String,
        val kravgrunnlagId: String,
        val kravgrunnlagPayload: String,
        val tilbakekrevingsvedtakRequest: String?,
        val tilbakekrevingsvedtakResponse: String?,
    )
}
