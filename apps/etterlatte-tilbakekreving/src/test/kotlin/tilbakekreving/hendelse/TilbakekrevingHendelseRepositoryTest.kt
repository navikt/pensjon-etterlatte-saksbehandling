package no.nav.etterlatte.tilbakekreving.hendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.database.singleOrNull
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
    fun `skal lagre hendelse for mottatt kravgrunnlag`() {
        val kravgrunnlagId = "1"
        val kravgrunnlag = "<kravgrunnlag>payload</kravgrunnlag>"

        repository.lagreMottattKravgrunnlag(kravgrunnlagId, kravgrunnlag)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe kravgrunnlag
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.KRAVGRUNNLAG_MOTTATT
    }

    @Test
    fun `skal lagre hendelse for sendt tilbakekrevingsvedtak`() {
        val kravgrunnlagId = "1"
        val vedtakRequest = "request_payload"

        repository.lagreTilbakekrevingsvedtakSendt(kravgrunnlagId, vedtakRequest)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe vedtakRequest
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_SENDT
    }

    @Test
    fun `skal lagre hendelse for tilbakekrevingsvedtak kvittering`() {
        val kravgrunnlagId = "1"
        val vedtakResponse = "response_payload"

        repository.lagreTilbakekrevingsvedtakKvitteringMottatt(kravgrunnlagId, vedtakResponse)

        val tilbakekrevingHendelse =
            hentTilbakekrevingHendelse(kravgrunnlagId, TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING)

        tilbakekrevingHendelse?.id shouldNotBe null
        tilbakekrevingHendelse?.opprettet shouldNotBe null
        tilbakekrevingHendelse?.kravgrunnlagId shouldBe kravgrunnlagId
        tilbakekrevingHendelse?.payload shouldBe vedtakResponse
        tilbakekrevingHendelse?.type shouldBe TilbakekrevingHendelseType.TILBAKEKREVINGSVEDTAK_KVITTERING
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE tilbakekreving_hendelse").apply { execute() } }
    }

    private fun hentTilbakekrevingHendelse(
        kravgrunnlagId: String,
        type: TilbakekrevingHendelseType,
    ): TilbakekrevingHendelse? {
        dataSource.connection.use {
            val stmt =
                it.prepareStatement("SELECT * FROM tilbakekreving_hendelse WHERE kravgrunnlag_id = ? AND type = ?")
                    .apply {
                        setString(1, kravgrunnlagId)
                        setString(2, type.name)
                    }

            return stmt.executeQuery().singleOrNull {
                TilbakekrevingHendelse(
                    id = getString("id"),
                    opprettet = getString("opprettet"),
                    kravgrunnlagId = getString("kravgrunnlag_id"),
                    payload = getString("payload"),
                    type = TilbakekrevingHendelseType.valueOf(getString("type")),
                )
            }
        }
    }

    private data class TilbakekrevingHendelse(
        val id: String,
        val opprettet: String,
        val kravgrunnlagId: String,
        val payload: String,
        val type: TilbakekrevingHendelseType,
    )
}
