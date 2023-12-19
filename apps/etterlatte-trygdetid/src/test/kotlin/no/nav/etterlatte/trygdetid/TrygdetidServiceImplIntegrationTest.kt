package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.VilkaarsvuderingKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import trygdetid.trygdetid
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidServiceImplIntegrationTest {
    val saksbehandler = Saksbehandler("token", "ident", null)

    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: TrygdetidRepository
    private lateinit var dataSource: DataSource
    private lateinit var trygdetidService: TrygdetidServiceImpl

    private val pdlKilde: Grunnlagsopplysning.Pdl = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
    private val regelKilde: Grunnlagsopplysning.RegelKilde = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")

    private lateinit var vilkaarsvuderingKlient: VilkaarsvuderingKlient
    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = TrygdetidRepository(dataSource.apply { migrate() })
        vilkaarsvuderingKlient = vilkaarsvurderingKlientMock()
        trygdetidService =
            TrygdetidServiceImpl(
                repository,
                mockk<BehandlingKlient>(),
                grunnlagKlient,
                vilkaarsvuderingKlient,
                TrygdetidBeregningService,
                DummyFeatureToggleService(),
            )
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
    fun `skal hente trygdetid med differanse i opplysninger`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val nyDoedsdato = grunnlagTestData.avdoed.doedsdato!!.plusDays(6)
        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagMedNyDoedsdato(nyDoedsdato)

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = SecureRandom().nextLong(100_000),
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetid = runBlocking { trygdetidService.hentTrygdetid(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }
        with(trygdetid?.opplysningerDifferanse!!) {
            differanse shouldBe true
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoed.foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe nyDoedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe grunnlagTestData.avdoed.foedselsdato!!.plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe grunnlagTestData.avdoed.foedselsdato!!.plusYears(66)
            }
        }
    }

    @Test
    fun `skal hente trygdetid uten differanse i opplysninger`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagTestData.hentOpplysningsgrunnlag()

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = SecureRandom().nextLong(100_000),
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetid = runBlocking { trygdetidService.hentTrygdetid(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }
        with(trygdetid?.opplysningerDifferanse!!) {
            differanse shouldBe false
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoed.foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe grunnlagTestData.avdoed.doedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe grunnlagTestData.avdoed.foedselsdato!!.plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe grunnlagTestData.avdoed.foedselsdato!!.plusYears(66)
            }
        }
    }

    private fun opplysningsgrunnlag(grunnlagTestData: GrunnlagTestData): List<Opplysningsgrunnlag> {
        val foedselsdato = grunnlagTestData.avdoed.foedselsdato!!
        val doedsdato = grunnlagTestData.avdoed.doedsdato!!
        val seksten = grunnlagTestData.avdoed.foedselsdato!!.plusYears(16)
        val sekstiseks = grunnlagTestData.avdoed.foedselsdato!!.plusYears(66)
        return listOf(
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdlKilde, foedselsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdlKilde, doedsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regelKilde, seksten),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regelKilde, sekstiseks),
        )
    }

    private fun vilkaarsvurderingKlientMock(): VilkaarsvuderingKlient {
        val dtoMock = mockk<VilkaarsvurderingDto>()
        every { dtoMock.isYrkesskade() } returns false

        val klient = mockk<VilkaarsvuderingKlient>()
        coEvery {
            klient.hentVilkaarsvurdering(any(), any())
        } returns dtoMock
        return klient
    }

    private fun grunnlagMedNyDoedsdato(nyDoedsdato: LocalDate): Grunnlag {
        val grunnlagTestData =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides =
                    mapOf(Opplysningstype.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, nyDoedsdato.toJsonNode())),
            )
        return grunnlagTestData.hentOpplysningsgrunnlag()
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdetid CASCADE").apply { execute() } }
    }
}
