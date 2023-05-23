package no.nav.etterlatte.trygdetid

import io.kotest.matchers.collections.shouldBeEmpty
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
import trygdetid.trygdetid
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
        val behandling = behandlingMock()

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

        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, opplysninger = opplysninger)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)

        trygdetid shouldNotBe null
        trygdetid.behandlingId shouldBe behandling.id
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
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
    }

    @Test
    fun `skal opprette et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val lagretTrygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedTrygdetidGrunnlag =
            repository.oppdaterTrygdetid(lagretTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlag))

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }
    }

    @Test
    fun `skal slette et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val lagretTrygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedTrygdetidGrunnlag =
            repository.oppdaterTrygdetid(lagretTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlag))

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }

        val trygdetidUtenGrunnlag = trygdetidMedTrygdetidGrunnlag.copy(trygdetidGrunnlag = emptyList())
        val lagretTrygdetidUtenGrunnlag = repository.oppdaterTrygdetid(trygdetidUtenGrunnlag)

        lagretTrygdetidUtenGrunnlag shouldNotBe null
        lagretTrygdetidUtenGrunnlag.trygdetidGrunnlag.shouldBeEmpty()
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, trygdetidGrunnlag = listOf(trygdetidGrunnlag))

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = "Polen")
        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetid(trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(endretTrygdetidGrunnlag))

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere beregnet trygdetid`() {
        val beregnetTrygdetid = beregnetTrygdetid(total = 12, tidspunkt = Tidspunkt.now())
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedBeregnetTrygdetid =
            repository.oppdaterTrygdetid(trygdetid.oppdaterBeregnetTrygdetid(beregnetTrygdetid))

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
    }

    private fun behandlingMock() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns 123L
        }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdetid CASCADE").apply { execute() } }
    }
}