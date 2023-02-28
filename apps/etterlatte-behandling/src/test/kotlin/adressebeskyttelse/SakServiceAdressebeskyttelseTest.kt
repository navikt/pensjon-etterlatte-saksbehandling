package no.nav.etterlatte.adressebeskyttelse

import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.sak.SakDaoAdressebeskyttelse
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelseImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakServiceAdressebeskyttelseTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var sakServiceAdressebeskyttelse: SakServiceAdressebeskyttelse
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }

        sakServiceAdressebeskyttelse = SakServiceAdressebeskyttelseImpl(SakDaoAdressebeskyttelse(dataSource))
        sakRepo = SakDao { dataSource.connection }
        behandlingRepo = BehandlingDao { dataSource.connection }
    }

    @Test
    fun `Skal kunne sette adressebeskyttelse på sak`() {
        val fnr = Foedselsnummer.of("08071272487").value
        val sakId = sakRepo.opprettSak(fnr, SakType.BARNEPENSJON).id

        sakServiceAdressebeskyttelse.setAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sakId,
            persongalleri = Persongalleri(
                soeker = "11111",
                innsender = "11111",
                soesken = listOf("11111", "04040", "05050"),
                avdoed = listOf("06060", "11111"),
                gjenlevende = listOf("11111")
            )
        )
        behandlingRepo.opprettBehandling(opprettBehandling)

        val behandlingHarAdressebeskyttelse =
            sakServiceAdressebeskyttelse.behandlingHarAdressebeskyttelse(opprettBehandling.id.toString())

        Assertions.assertEquals(true, behandlingHarAdressebeskyttelse)

        val smokeTestBehandling =
            sakServiceAdressebeskyttelse.behandlingHarAdressebeskyttelse(UUID.randomUUID().toString())

        Assertions.assertEquals(false, smokeTestBehandling)
    }
}