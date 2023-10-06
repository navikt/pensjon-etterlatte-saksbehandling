package vedtaksvurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtakTilbakekrevingRepositoryTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var dataSource: DataSource
    private lateinit var repository: VedtakTilbakekrevingRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        dataSource =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }
        repository = VedtakTilbakekrevingRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `lagreFattetVedtak skal lagre hvis ikke finnes fra foer`() {
        val nyttFattetVedtak =
            TilbakekrevingFattetVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                sakId = 123L,
                sakType = SakType.OMSTILLINGSSTOENAD,
                soeker = Folkeregisteridentifikator.of("08071272487"),
                ansvarligSaksbehandler = "saksbehanlder",
                ansvarligEnhet = "enht",
                tilbakekreving = objectMapper.createObjectNode(),
            )

        val resultat = repository.lagreFattetVedtak(nyttFattetVedtak)

        resultat.asClue {
            it.id shouldNotBe null
            it.behandlingId shouldBe nyttFattetVedtak.tilbakekrevingId
            it.sakId shouldBe nyttFattetVedtak.sakId
            it.sakType shouldBe nyttFattetVedtak.sakType
            it.soeker shouldBe nyttFattetVedtak.soeker
            it.vedtakFattet!!.ansvarligSaksbehandler shouldBe nyttFattetVedtak.ansvarligSaksbehandler
            it.vedtakFattet!!.ansvarligEnhet shouldBe nyttFattetVedtak.ansvarligEnhet
            it.tilbakekreving shouldBe nyttFattetVedtak.tilbakekreving
            it.status shouldBe VedtakStatus.FATTET_VEDTAK
            it.type shouldBe VedtakType.TILBAKEKREVING
        }
    }

    @Test
    fun `lagreFattetVedtak skal oppdatere hvis finnes fra foer`() {
        val nyttFattetVedtak =
            TilbakekrevingFattetVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                sakId = 123L,
                sakType = SakType.OMSTILLINGSSTOENAD,
                soeker = Folkeregisteridentifikator.of("08071272487"),
                ansvarligSaksbehandler = "noen",
                ansvarligEnhet = "enht",
                tilbakekreving = objectMapper.createObjectNode(),
            )
        repository.lagreFattetVedtak(nyttFattetVedtak)
        val oppdatertVedtak =
            nyttFattetVedtak.copy(
                ansvarligSaksbehandler = "noen andre",
                ansvarligEnhet = "fire",
                // TODO EY-2767 tilbakekreving objectnode
            )
        repository.lagreFattetVedtak(oppdatertVedtak).let {
            it.id shouldNotBe null
            it.behandlingId shouldBe oppdatertVedtak.tilbakekrevingId
            it.sakId shouldBe oppdatertVedtak.sakId
            it.sakType shouldBe oppdatertVedtak.sakType
            it.soeker shouldBe oppdatertVedtak.soeker
            it.vedtakFattet!!.ansvarligSaksbehandler shouldBe oppdatertVedtak.ansvarligSaksbehandler
            it.vedtakFattet!!.ansvarligEnhet shouldBe oppdatertVedtak.ansvarligEnhet
            it.tilbakekreving shouldBe oppdatertVedtak.tilbakekreving
            it.status shouldBe VedtakStatus.FATTET_VEDTAK
            it.type shouldBe VedtakType.TILBAKEKREVING
        }
    }
}
