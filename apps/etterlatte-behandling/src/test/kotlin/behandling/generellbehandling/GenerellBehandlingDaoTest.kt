package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.Dokumenter
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GenerellBehandlingDaoTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var dao: GenerellBehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        val connection = dataSource.connection
        dao = GenerellBehandlingDao { connection }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE generellbehandling CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `opprette kun med type`() {
        val generellBehandlingUtland = GenerellBehandling.opprettUtland(1L, null)
        val hentetGenBehandling = dao.opprettGenerellbehandling(generellBehandlingUtland)

        Assertions.assertEquals(generellBehandlingUtland.id, hentetGenBehandling.id)
        Assertions.assertEquals(generellBehandlingUtland.innhold, hentetGenBehandling.innhold)
    }

    @Test
    fun `Assert skal catche at man oppretter med feil type`() {
        assertThrows<IllegalArgumentException> {
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.Annen("content"),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        }
    }

    @Test
    fun `Kan opprette og hente en generell behandling utland`() {
        val kravpakkeUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.KravpakkeUtland(
                    listOf("AFG"),
                    Dokumenter(
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                    ),
                    "2grwg2",
                    "rita",
                ),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        val hentetGenBehandling = dao.opprettGenerellbehandling(kravpakkeUtland)

        Assertions.assertEquals(kravpakkeUtland.id, hentetGenBehandling.id)
        Assertions.assertEquals(kravpakkeUtland.innhold, hentetGenBehandling.innhold)
    }

    @Test
    fun `Kan hente for sak`() {
        val sakId = 1L
        val kravpakkeUtland =
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.KRAVPAKKE_UTLAND,
                Innhold.KravpakkeUtland(
                    listOf("AFG"),
                    Dokumenter(
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                        DokumentMedSendtDato(true, LocalDate.now()),
                    ),
                    "2grwg2",
                    "rita",
                ),
                status = GenerellBehandling.Status.OPPRETTET,
            )
        val annengenerebehandling =
            GenerellBehandling(
                UUID.randomUUID(),
                1L,
                Tidspunkt.now(),
                GenerellBehandling.GenerellBehandlingType.ANNEN,
                Innhold.Annen("vlabla"),
                status = GenerellBehandling.Status.OPPRETTET,
            )

        dao.opprettGenerellbehandling(kravpakkeUtland)
        dao.opprettGenerellbehandling(annengenerebehandling)
        val hentetGenBehandling = dao.hentGenerellBehandlingForSak(sakId)
        Assertions.assertEquals(2, hentetGenBehandling.size)
        val generellBehandling = hentetGenBehandling.single { it.innhold is Innhold.KravpakkeUtland }
        Assertions.assertEquals(kravpakkeUtland.id, generellBehandling.id)
        Assertions.assertEquals(kravpakkeUtland.innhold, generellBehandling.innhold)
    }
}
