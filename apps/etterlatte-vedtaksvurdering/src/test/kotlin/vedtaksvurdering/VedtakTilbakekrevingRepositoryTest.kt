package vedtaksvurdering

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingInnhold
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
            it.status shouldBe VedtakStatus.FATTET_VEDTAK
            it.type shouldBe VedtakType.TILBAKEKREVING
            (it.innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe nyttFattetVedtak.tilbakekreving
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
        repository.lagreFattetVedtak(oppdatertVedtak).asClue {
            it.id shouldNotBe null
            it.behandlingId shouldBe oppdatertVedtak.tilbakekrevingId
            it.sakId shouldBe oppdatertVedtak.sakId
            it.sakType shouldBe oppdatertVedtak.sakType
            it.soeker shouldBe oppdatertVedtak.soeker
            it.vedtakFattet!!.ansvarligSaksbehandler shouldBe oppdatertVedtak.ansvarligSaksbehandler
            it.vedtakFattet!!.ansvarligEnhet shouldBe oppdatertVedtak.ansvarligEnhet
            (it.innhold as VedtakTilbakekrevingInnhold).tilbakekreving shouldBe oppdatertVedtak.tilbakekreving
            it.status shouldBe VedtakStatus.FATTET_VEDTAK
            it.type shouldBe VedtakType.TILBAKEKREVING
        }
    }

    @Test
    fun `lagreAttestertVedtak skal oppdatere vedtak med attestasjon`() {
        val tilbakekrevingId = UUID.randomUUID()
        repository.lagreFattetVedtak(
            TilbakekrevingFattetVedtakDto(
                tilbakekrevingId = tilbakekrevingId,
                sakId = 123L,
                sakType = SakType.OMSTILLINGSSTOENAD,
                soeker = Folkeregisteridentifikator.of("08071272487"),
                ansvarligSaksbehandler = "noen",
                ansvarligEnhet = "enht",
                tilbakekreving = objectMapper.createObjectNode(),
            ),
        )
        val attestasjon =
            TilbakekrevingAttesterVedtakDto(
                tilbakekrevingId = tilbakekrevingId,
                attestant = "saksbehandler",
                attesterendeEnhet = "fire",
            )

        repository.lagreAttestertVedtak(attestasjon).asClue {
            it.attestasjon shouldNotBe null
            it.attestasjon!!.attestant shouldBe attestasjon.attestant
            it.attestasjon!!.attesterendeEnhet shouldBe attestasjon.attesterendeEnhet
            it.attestasjon!!.tidspunkt shouldNotBe null
        }
    }

    @Test
    fun `lagreUnderkjentVedtak skal nullstille vedtak`() {
        val vedtak =
            TilbakekrevingFattetVedtakDto(
                tilbakekrevingId = UUID.randomUUID(),
                sakId = 123L,
                sakType = SakType.OMSTILLINGSSTOENAD,
                soeker = Folkeregisteridentifikator.of("08071272487"),
                ansvarligSaksbehandler = "noen",
                ansvarligEnhet = "enht",
                tilbakekreving = objectMapper.createObjectNode(),
            )
        repository.lagreFattetVedtak(vedtak)

        repository.lagreUnderkjentVedtak(vedtak.tilbakekrevingId).asClue {
            it.soeker shouldBe vedtak.soeker
            it.sakId shouldBe vedtak.sakId
            it.sakType shouldBe vedtak.sakType
            it.behandlingId shouldBe vedtak.tilbakekrevingId
            it.type shouldBe VedtakType.TILBAKEKREVING
            it.status shouldBe VedtakStatus.RETURNERT
            it.vedtakFattet shouldBe null
            it.attestasjon shouldBe null
            // it.tilbakekreving shouldBe null TODO EY-2767 tilbakekreving objectnode
        }
    }
}
