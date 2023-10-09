package tilbakekreving.hendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.tilbakekreving.hendelse.TilbakekrevingHendelseRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TilbakekrevingHendelseRepositoryTest {
    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: TilbakekrevingHendelseRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = TilbakekrevingHendelseRepository(dataSource.apply { migrate() })
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

        val tilbakekrevingHendelse = hentTilbakekrevingHendelse(kravgrunnlagId)
        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.fagsystemId shouldBe fagsystemId
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.kravgrunnlagPayload shouldBe kravgrunnlagPayload
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

        val tilbakekrevingHendelse = hentTilbakekrevingHendelse(kravgrunnlagId)

        tilbakekrevingHendelse?.endret shouldNotBe null
        tilbakekrevingHendelse?.tilbakekrevingsvedtakRequest shouldBe vedtakRequest
        tilbakekrevingHendelse?.tilbakekrevingsvedtakResponse shouldBe vedtakResponse
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE tilbakekreving_hendelse").apply { execute() } }
    }

    private fun hentTilbakekrevingHendelse(kravgrunnlagId: String): TilbakekrevingHendelse? {
        dataSource.connection.use {
            val stmt =
                it.prepareStatement("SELECT * FROM tilbakekreving_hendelse WHERE kravgrunnlag_id = ?").apply {
                    setString(1, kravgrunnlagId)
                }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingHendelse(
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

    private data class TilbakekrevingHendelse(
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
