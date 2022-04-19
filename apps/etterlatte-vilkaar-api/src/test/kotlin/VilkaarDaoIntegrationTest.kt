package no.nav.etterlatte

import no.nav.etterlatte.config.DataSourceBuilder
import no.nav.etterlatte.config.StandardJdbcUrlBuilder
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.Grunnlagshendelse
import no.nav.etterlatte.libs.common.vikaar.VilkarIBehandling
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.sql.Types
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarDaoIntegrationTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var flyway: Flyway
    private lateinit var dataSource: DataSource
    private lateinit var vilkaarDao: VilkaarDao
    private val behandlingId = UUID.randomUUID()

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        DataSourceBuilder(
            jdbcUrlBuilder = StandardJdbcUrlBuilder(postgreSQLContainer.jdbcUrl),
            databaseUsername = postgreSQLContainer.username,
            databasePassword = postgreSQLContainer.password
        ).also {
            dataSource = it.dataSource()
            vilkaarDao = VilkaarDaoJdbc(dataSource)

            // TODO ikke optimalt at man må kopiere inn skjema fra annen app for å få testet dette
            flyway = Flyway
                .configure()
                .dataSource(dataSource)
                .load()
        }
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()
        dataSource.lagreGrunnlagsHendelse()
        dataSource.lagreVurdertVilkaar()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    fun DataSource.lagreGrunnlagsHendelse(
        hendelser: List<Grunnlagshendelse> = grunnlagsHendelsesListeForBehandling(behandlingId.toString())
    ) {
        this.connection.use { connection ->
            val stmt =
                connection.prepareStatement("INSERT INTO grunnlagshendelse(behandling, opplysning, kilde, opplysningtype, hendelsenummer, hendelsetype, hendelseref) VALUES(?, ?, ?, ?, ?, ?, ?)")

            stmt.use {
                hendelser.forEach { hendelse ->
                    it.setObject(1, hendelse.behandling)
                    it.setString(2, hendelse.opplysning?.opplysning?.let { objectMapper.writeValueAsString(it) })
                    it.setString(3, hendelse.opplysning?.kilde?.let { objectMapper.writeValueAsString(it) })
                    it.setString(4, hendelse.opplysning?.opplysningType?.name)
                    it.setLong(5, hendelse.hendelsenummer)
                    it.setString(6, hendelse.hendelsetype.name)
                    hendelse.hendelsereferanse?.let { it2 -> it.setLong(7, it2) } ?: it.setNull(
                        7,
                        Types.BIGINT
                    )
                    it.executeUpdate()
                }
            }
        }
    }

    fun DataSource.lagreVurdertVilkaar(
        vilkarIBehandling: VilkarIBehandling = vilkaarResultatForBehandling(behandlingId.toString())
    ) {
        this.connection.use { connection ->
            val stmt =
                connection.prepareStatement("INSERT INTO vurdertvilkaar(behandling, versjon, vilkaarResultat, avdoedSoeknad, soekerSoeknad, soekerPdl, avdoedPdl, gjenlevendePdl) values(?, ?, ?, ?, ?, ?, ?, ?)")

            stmt.use {
                it.setObject(1, vilkarIBehandling.behandling)
                it.setLong(2, vilkarIBehandling.versjon)
                it.setString(3, objectMapper.writeValueAsString(vilkarIBehandling.vilkaarResultat))
                it.setString(4, vilkarIBehandling.grunnlag.avdoedSoeknad?.let { objectMapper.writeValueAsString(it) })
                it.setString(5, vilkarIBehandling.grunnlag.soekerSoeknad?.let { objectMapper.writeValueAsString(it) })
                it.setString(6, vilkarIBehandling.grunnlag.soekerPdl?.let { objectMapper.writeValueAsString(it) })
                it.setString(7, vilkarIBehandling.grunnlag.avdoedPdl?.let { objectMapper.writeValueAsString(it) })
                it.setString(
                    8,
                    vilkarIBehandling.grunnlag.gjenlevendePdl?.let { objectMapper.writeValueAsString(it) })

                it.executeUpdate()
            }
        }


    }

    @Test
    fun `Vilkår lagres korrekt i databasen og kan hentes ut`() {
        // TODO legg til en vilkaarsvurdering for å teste uthenting
        val vilkaarResultat = vilkaarDao.hentVilkaarResultat(behandlingId.toString())
        assertNotNull(vilkaarResultat)
        assertEquals(behandlingId, vilkaarResultat!!.behandling)
        assertNull(vilkaarResultat.grunnlag.avdoedPdl)
        assertNull(vilkaarResultat.grunnlag.avdoedSoeknad)
        assertNull(vilkaarResultat.grunnlag.soekerPdl)
        assertNull(vilkaarResultat.grunnlag.soekerSoeknad)
        assertNull(vilkaarResultat.grunnlag.gjenlevendePdl)
        assertEquals(1, vilkaarResultat.versjon)
        assertEquals(VurderingsResultat.OPPFYLT, vilkaarResultat.vilkaarResultat.resultat)
        assertEquals(0, vilkaarResultat.vilkaarResultat.vilkaar!!.size)
        assertTrue(
            vilkaarResultat.vilkaarResultat.vurdertDato.isBefore(LocalDateTime.now()) and vilkaarResultat.vilkaarResultat.vurdertDato.isAfter(
                LocalDateTime.now().minusSeconds(10)
            )
        )

    }

}