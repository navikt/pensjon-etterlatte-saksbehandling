package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
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
import trygdetid.beregnetTrygdetidGrunnlag
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
        val seksten = LocalDate.of(2016, 1, 1)
        val seksti = LocalDate.of(2066, 1, 1)

        val pdl = Grunnlagsopplysning.Pdl("pdl", Tidspunkt.now(), null, "opplysningsId1")
        val regel = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")
        val opplysninger = listOf(
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdl, foedselsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdl, doedsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regel, seksten),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regel, seksti)
        )
        val trygdetid = repository.transaction { tx -> repository.opprettTrygdetid(behandling, opplysninger, tx) }

        trygdetid shouldNotBe null
        trygdetid.behandlingId shouldBe behandlingId
        trygdetid.opplysninger.size shouldBe 4

        with(trygdetid.opplysninger[0]) {
            type shouldBe TrygdetidOpplysningType.FOEDSELSDATO
            opplysning shouldBe foedselsdato.toJsonNode()
            kilde shouldBe pdl
        }
        with(trygdetid.opplysninger[1]) {
            type shouldBe TrygdetidOpplysningType.DOEDSDATO
            opplysning shouldBe doedsdato.toJsonNode()
            kilde shouldBe pdl
        }
        with(trygdetid.opplysninger[2]) {
            type shouldBe TrygdetidOpplysningType.FYLT_16
            opplysning shouldBe seksten.toJsonNode()
            kilde shouldBe regel
        }
        with(trygdetid.opplysninger[3]) {
            type shouldBe TrygdetidOpplysningType.FYLLER_66
            opplysning shouldBe seksti.toJsonNode()
            kilde shouldBe regel
        }
    }

    @Test
    fun `skal opprette og hente trygdetid`() {
        val behandlingId = randomUUID()
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }
        repository.transaction { tx -> repository.opprettTrygdetid(behandling, emptyList(), tx) }

        val trygdetid = repository.hentTrygdetid(behandlingId)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandlingId
    }

    @Test
    fun `skal opprette et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())

        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }

        val trygdetidMedTrygdetidGrunnlag = repository.transaction { tx ->
            repository.opprettTrygdetid(behandling, emptyList(), tx)
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag, tx)
        }

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
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = "Polen")

        val trygdetidMedOppdatertGrunnlag = repository.transaction { tx ->
            repository.opprettTrygdetid(behandling, emptyList(), tx)
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag, tx)
            repository.oppdaterTrygdetidGrunnlag(behandlingId, endretTrygdetidGrunnlag, tx)
        }

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal hente et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())

        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }

        val hentetTrygdetidGrunnlag = repository.transaction { tx ->
            repository.opprettTrygdetid(behandling, emptyList(), tx)
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag, tx)

            repository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id, tx)
        }

        hentetTrygdetidGrunnlag shouldNotBe null
    }

    @Test
    fun `skal oppdatere beregnet trygdetid`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(total = 12, tidspunkt = Tidspunkt.now())
        val behandling = mockk<DetaljertBehandling>().apply {
            every { id } returns behandlingId
            every { sak } returns 123L
        }

        val trygdetidMedBeregnetTrygdetid = repository.transaction { tx ->
            repository.opprettTrygdetid(behandling, emptyList(), tx)

            repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid, tx)
        }

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdetid CASCADE").apply { execute() } }
    }
}