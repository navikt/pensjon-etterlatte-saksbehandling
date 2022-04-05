import no.nav.etterlatte.attestering.AttestasjonDao
import no.nav.etterlatte.attestering.AttestasjonService
import no.nav.etterlatte.attestering.AttestertVedtakEksistererAllerede
import no.nav.etterlatte.attestering.KanIkkeEndreAttestertVedtakException
import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.domain.AttestasjonsStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AttestasjonDaoTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var attestasjonDao: AttestasjonDao
    private lateinit var attestasjonService: AttestasjonService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).let {
            dataSource = it.dataSource()
            attestasjonDao = AttestasjonDao { dataSource.connection }
            attestasjonService = AttestasjonService(attestasjonDao)
            it.migrate()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        dataSource.connection.use {
            it.prepareStatement("DELETE FROM attestasjon WHERE vedtak_id IS NOT NULL")
                .apply { execute() }
        }
    }

    @Test
    fun `skal opprette og lagre attestert vedtak i databasen`() {
        val vedtak = vedtak()
        val attestasjon = attestasjon()

        val attestertVedtak = attestasjonService.opprettAttestertVedtak(vedtak, attestasjon)

        assertEquals(vedtak.vedtakId, attestertVedtak.vedtakId)
        assertEquals(attestasjon.attestantId, attestertVedtak.attestantId)
        assertTrue(
            (attestertVedtak.attestasjonstidspunkt!!.isAfter(
                LocalDateTime.now().minusSeconds(20)
            ) and
                    attestertVedtak.attestasjonstidspunkt!!.isBefore(
                        LocalDateTime.now().plusSeconds(0)
                    )
                    )
        )
        assertEquals(AttestasjonsStatus.ATTESTERT, attestertVedtak.attestasjonsstatus)
    }

    @Test
    fun `skal opprette ikke-attestert vedtak i databasen og så attestere vedtaket`() {
        val vedtak = vedtak()
        val attestasjon = attestasjon()

        val uattestertVedtak = attestasjonService.opprettVedtakUtenAttestering(vedtak)

        assertEquals(vedtak.vedtakId, uattestertVedtak!!.vedtakId)
        assertNull(uattestertVedtak!!.attestantId)
        assertNull(uattestertVedtak!!.attestasjonstidspunkt)
        assertEquals(AttestasjonsStatus.TIL_ATTESTERING, uattestertVedtak.attestasjonsstatus)


        val attestertVedtak = attestasjonService.attesterVedtak(vedtak.vedtakId, attestasjon)

        assertEquals(vedtak.vedtakId, attestertVedtak!!.vedtakId)
        assertEquals(attestasjon.attestantId, attestertVedtak!!.attestantId)
        assertTrue(
            (attestertVedtak!!.attestasjonstidspunkt!!.isAfter(
                LocalDateTime.now().minusSeconds(20)
            ) and
                    attestertVedtak.attestasjonstidspunkt!!.isBefore(
                        LocalDateTime.now().plusSeconds(0)
                    )
                    )
        )
        assertEquals(AttestasjonsStatus.ATTESTERT, attestertVedtak.attestasjonsstatus)
    }

    @Test
    fun `skal ikke kunne opprette attestert vedtak eller vedtak uten attestering om det allerede eksisterer`() {

        val vedtak = vedtak()
        val attestasjon = attestasjon()

        val attestertVedtak = attestasjonService.opprettAttestertVedtak(vedtak, attestasjon)
        assertThrows<AttestertVedtakEksistererAllerede> {
            attestasjonService.opprettAttestertVedtak(
                vedtak,
                attestasjon
            )
        }
        assertThrows<AttestertVedtakEksistererAllerede> {
            attestasjonService.opprettVedtakUtenAttestering(
                vedtak
            )
        }
    }

    @Test
    fun `skal ikke kunne attestere allerede attestert vedtak`() {

        val vedtak = vedtak()
        val attestasjon = attestasjon()

        attestasjonService.opprettAttestertVedtak(vedtak, attestasjon)

        assertThrows<KanIkkeEndreAttestertVedtakException> {
            attestasjonService.attesterVedtak(vedtak.vedtakId, attestasjon)
        }
    }
}
