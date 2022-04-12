
import com.zaxxer.hikari.HikariConfig
import no.nav.etterlatte.model.VilkaarService
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import vilkaar.VurderteVilkaarDao
import vilkaar.grunnlag.Vilkaarsgrunnlag
import vilkaar.grunnlag.VilkarIBehandling
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Database {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(HikariConfig().also { it.jdbcUrl= postgreSQLContainer.jdbcUrl })
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `db integration test`() {

        val behandling = UUID.randomUUID()
        VurderteVilkaarDao(dataSource.connection).also {
            Assertions.assertTrue(it.hentOppVurderinger(behandling).isEmpty())
            it.lagreVurdering(VilkarIBehandling(behandling, Vilkaarsgrunnlag(), 1, VilkaarService().mapVilkaar(Vilkaarsgrunnlag())))
            assertThrows<SQLException> {
                it.lagreVurdering(VilkarIBehandling(behandling, Vilkaarsgrunnlag(), 1, VilkaarService().mapVilkaar(Vilkaarsgrunnlag())))
            }
            Assertions.assertEquals(1, it.hentOppVurderinger(behandling).size)
        }


    }
}