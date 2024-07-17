package no.nav.etterlatte.trygdetid

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidServiceIntegrationTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val saksbehandler = simpleSaksbehandler()

    private val repository = TrygdetidRepository(dataSource)
    private lateinit var trygdetidService: TrygdetidService

    private val pdlKilde: Grunnlagsopplysning.Pdl = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")
    private val regelKilde: Grunnlagsopplysning.RegelKilde = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")

    private val grunnlagKlient: GrunnlagKlient = mockk<GrunnlagKlient>()

    @BeforeAll
    fun beforeAll() {
        trygdetidService =
            TrygdetidServiceImpl(
                repository,
                mockk<BehandlingKlient>(),
                grunnlagKlient,
                TrygdetidBeregningService,
            )
    }

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `skal hente trygdetid med differanse i opplysninger`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        val nyDoedsdato =
            grunnlagTestData.avdoede
                .first()
                .doedsdato!!
                .plusDays(6)
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

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }

        with(trygdetider.firstOrNull()?.opplysningerDifferanse!!) {
            differanse shouldBe true
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoede.first().foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe nyDoedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(66)
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

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        val toLocalDate: (OpplysningsgrunnlagDto?) -> LocalDate? = { dto ->
            dto?.let { deserialize<LocalDate>(it.opplysning.toJson()) }
        }
        with(trygdetider.firstOrNull()?.opplysningerDifferanse!!) {
            differanse shouldBe false
            with(oppdaterteGrunnlagsopplysninger) {
                toLocalDate(avdoedFoedselsdato) shouldBe grunnlagTestData.avdoede.first().foedselsdato
                toLocalDate(avdoedDoedsdato) shouldBe grunnlagTestData.avdoede.first().doedsdato
                toLocalDate(avdoedFylteSeksten) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(16)
                toLocalDate(avdoedFyllerSeksti) shouldBe
                    grunnlagTestData.avdoede
                        .first()
                        .foedselsdato!!
                        .plusYears(66)
            }
        }
    }

    @Test
    fun `skal hente trygdetid uten opplysningsgrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()
        val standardOpplysningsgrunnlag = grunnlagTestData.hentOpplysningsgrunnlag()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns
            Grunnlag(
                soeker = standardOpplysningsgrunnlag.soeker,
                familie = emptyList(),
                sak = standardOpplysningsgrunnlag.sak,
                metadata = standardOpplysningsgrunnlag.metadata,
            )
        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = SecureRandom().nextLong(100_000),
                ident = UKJENT_AVDOED,
                opplysninger = emptyList(),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        with(trygdetider.first()) {
            ident shouldBe UKJENT_AVDOED
            opplysningerDifferanse!! shouldBeEqual OpplysningerDifferanse(false, GrunnlagOpplysningerDto.tomt())
        }
    }

    @Test
    fun `skal slette trygdetid hvis ident ikke lenger finnes i familie`() {
        val behandlingId = UUID.randomUUID()
        val grunnlagTestData = GrunnlagTestData()

        coEvery {
            grunnlagKlient.hentGrunnlag(any(), any())
        } returns grunnlagTestData.hentOpplysningsgrunnlag()

        repository.opprettTrygdetid(
            trygdetid(
                behandlingId = behandlingId,
                sakId = SecureRandom().nextLong(100_000),
                ident = "123",
                opplysninger = opplysningsgrunnlag(grunnlagTestData),
            ),
        )

        val trygdetider = runBlocking { trygdetidService.hentTrygdetiderIBehandling(behandlingId, saksbehandler) }

        trygdetider shouldBe emptyList()
    }

    private fun opplysningsgrunnlag(grunnlagTestData: GrunnlagTestData): List<Opplysningsgrunnlag> {
        val foedselsdato = grunnlagTestData.avdoede.first().foedselsdato!!
        val doedsdato = grunnlagTestData.avdoede.first().doedsdato!!
        val seksten =
            grunnlagTestData.avdoede
                .first()
                .foedselsdato!!
                .plusYears(16)
        val sekstiseks =
            grunnlagTestData.avdoede
                .first()
                .foedselsdato!!
                .plusYears(66)
        return listOf(
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdlKilde, foedselsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdlKilde, doedsdato),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regelKilde, seksten),
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regelKilde, sekstiseks),
        )
    }

    private fun grunnlagMedNyDoedsdato(nyDoedsdato: LocalDate): Grunnlag {
        val grunnlagTestData =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides =
                    mapOf(Opplysningstype.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, nyDoedsdato.toJsonNode())),
            )
        return grunnlagTestData.hentOpplysningsgrunnlag()
    }
}
