package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import trygdetid.beregnetTrygdetid
import trygdetid.trygdetidGrunnlag
import java.time.LocalDate
import java.util.*
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRepositoryTest {

    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var repository: TrygdetidRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = TrygdetidRepository(dataSource.apply { migrate() })
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
    fun `skal opprette trygdetid med opplysninger`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }

        val foedselsdato = LocalDate.of(2000, 1, 1)
        val doedsdato = LocalDate.of(2020, 1, 1)
        val pdl = Grunnlagsopplysning.Pdl("pdl", Tidspunkt.now(), null, "opplysningsId1")
        val opplysninger = mapOf(
            Opplysningstype.FOEDSELSDATO to mockk<Opplysning.Konstant<LocalDate>>().apply {
                every { verdi } returns foedselsdato
                every { kilde } returns pdl
            },
            Opplysningstype.DOEDSDATO to mockk<Opplysning.Konstant<LocalDate?>>().apply {
                every { verdi } returns doedsdato
                every { kilde } returns pdl
            }
        )
        val trygdetid = repository.opprettTrygdetid(behandling, opplysninger)

        trygdetid shouldNotBe null
        trygdetid.behandlingId shouldBe behandlingId

        with(trygdetid.opplysninger[0]) {
            type shouldBe Opplysningstype.FOEDSELSDATO
            opplysning.toLocalDate() shouldBe foedselsdato
            kilde shouldNotBe null
        }
        with(trygdetid.opplysninger[1]) {
            type shouldBe Opplysningstype.DOEDSDATO
            opplysning.toLocalDate() shouldBe doedsdato
            kilde shouldNotBe null
        }
    }

    @Test
    fun `skal opprette og hente trygdetid`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        repository.opprettTrygdetid(behandling, mapOf())

        val trygdetid = repository.hentTrygdetid(behandlingId)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandlingId
    }

    @Test
    fun `skal opprette et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        val trygdetid = repository.opprettTrygdetid(behandling, mapOf())
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)

        val trygdetidMedTrygdetidGrunnlag =
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        val trygdetid = repository.opprettTrygdetid(behandling, mapOf())

        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(trygdetid = 20)
        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetidGrunnlag(behandlingId, endretTrygdetidGrunnlag)

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal hente et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        val trygdetid = repository.opprettTrygdetid(behandling, mapOf())

        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        val hentetTrygdetidGrunnlag = repository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)

        hentetTrygdetidGrunnlag shouldNotBe null
    }

    @Test
    fun `skal oppdatere beregnet trygdetid`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(nasjonal = 10, fremtidig = 2, total = 12, tidspunkt = Tidspunkt.now())
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        repository.opprettTrygdetid(behandling, mapOf())

        val trygdetidMedBeregnetTrygdetid = repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdetid CASCADE").apply { execute() } }
    }
}