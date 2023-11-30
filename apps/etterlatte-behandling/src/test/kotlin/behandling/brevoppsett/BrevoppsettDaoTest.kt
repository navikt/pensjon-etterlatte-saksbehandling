package no.nav.etterlatte.behandling.brevoppsett

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevoppsettDaoTest {
    private lateinit var dataSource: DataSource
    private lateinit var dao: BrevoppsettDao

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    @BeforeAll
    fun setup() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }

        dao = BrevoppsettDao { dataSource.connection }
    }

    @BeforeEach
    fun reset() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE brevoppsett""").executeUpdate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal lagre brevoppsett`() {
        val brevoppsett = brevoppsett()

        val lagretBrevoppsett = dao.lagre(brevoppsett)

        lagretBrevoppsett shouldNotBe null
        lagretBrevoppsett.etterbetaling?.fom shouldBe brevoppsett.etterbetaling?.fom
        lagretBrevoppsett.etterbetaling?.tom shouldBe brevoppsett.etterbetaling?.tom
        lagretBrevoppsett.aldersgruppe shouldBe brevoppsett.aldersgruppe
        lagretBrevoppsett.brevtype shouldBe brevoppsett.brevtype
        lagretBrevoppsett.kilde shouldBe brevoppsett.kilde
    }

    @Test
    fun `skal hente brevoppsett`() {
        val brevoppsett = brevoppsett()

        dao.lagre(brevoppsett)
        val lagretBrevoppsett = dao.hent(brevoppsett.behandlingId)

        lagretBrevoppsett shouldNotBe null
    }

    @Test
    fun `skal oppdatere brevoppsett`() {
        val brevoppsett = brevoppsett()

        val lagretBrevoppsett = dao.lagre(brevoppsett)

        val oppdatertBrevoppsett =
            lagretBrevoppsett.copy(
                etterbetaling =
                    Etterbetaling(
                        fom = brevoppsett.etterbetaling!!.fom.minusYears(1),
                        tom = brevoppsett.etterbetaling!!.tom.minusYears(1),
                    ),
                aldersgruppe = Aldersgruppe.OVER_18,
                brevtype = Brevtype.UTLAND,
            )

        val lagretOppdatertBrevoppsett = dao.lagre(oppdatertBrevoppsett)

        lagretOppdatertBrevoppsett shouldNotBe null
        lagretOppdatertBrevoppsett.etterbetaling?.fom shouldBe brevoppsett.etterbetaling?.fom?.minusYears(1)
        lagretOppdatertBrevoppsett.etterbetaling?.tom shouldBe brevoppsett.etterbetaling?.tom?.minusYears(1)
        lagretOppdatertBrevoppsett.aldersgruppe shouldBe Aldersgruppe.OVER_18
        lagretOppdatertBrevoppsett.brevtype shouldBe Brevtype.UTLAND
        lagretOppdatertBrevoppsett.kilde shouldNotBe null
    }

    private fun brevoppsett() =
        Brevoppsett(
            behandlingId = UUID.randomUUID(),
            etterbetaling =
                Etterbetaling(
                    fom = YearMonth.of(2023, 11),
                    tom = YearMonth.of(2023, 12),
                ),
            aldersgruppe = Aldersgruppe.UNDER_18,
            brevtype = Brevtype.NASJONAL,
            kilde = Grunnlagsopplysning.Saksbehandler("Z1234567", Tidspunkt.now()),
        )
}
