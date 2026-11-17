package no.nav.etterlatte.intern

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.etterlatte.beregning.AnvendtTrygdetidPeriodeUtrekning
import no.nav.etterlatte.beregning.AnvendtTrygdetidRepository
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.TrygdetidGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.anvendtTrygdetidRegel
import no.nav.etterlatte.beregning.regler.samletTrygdetid
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.ktor.startRandomPort
import no.nav.etterlatte.ktor.token.issueSystembrukerToken
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.regler.PeriodisertResultat
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.regler.Beregningstall
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AnvendtTrygdetidCrudTest(
    private val dataSource: DataSource,
) {
    private val repository = AnvendtTrygdetidRepository(dataSource)
    private val oauth = MockOAuth2Server()
    private val behandlingApp = "dev-gcp:etterlatte:etterlatte-behandling"

    @BeforeAll
    fun start() {
        oauth.startRandomPort()
    }

    @AfterAll
    fun stopp() {
        oauth.shutdown()
    }

    @Test
    fun `CRUD lagrer samme regeltre som eksisterende repository og beholder historikken`() {
        val behandlingId = UUID.randomUUID()
        val periode = RegelPeriode(LocalDate.of(2024, 1, 1))
        val resultat =
            anvendtTrygdetidRegel.anvend(
                TrygdetidGrunnlag(
                    samletTrygdetid(
                        BeregningsMetode.NASJONAL,
                        samletTrygdetidNorge = Beregningstall(40),
                        ident = "avdoed",
                    ),
                ),
                periode,
            )
        val utrekning =
            AnvendtTrygdetidPeriodeUtrekning(
                anvendt = listOf(GrunnlagMedPeriode(listOf(resultat.verdi), periode.fraDato, null)),
                utrekning = listOf(PeriodisertResultat(periode, resultat, "test")),
            )
        repository.lagreAnvendtTrygdetid(behandlingId, utrekning) shouldBe 1

        testApplication {
            runServer(oauth) {
                beregningCrudRoutes(
                    beregningRepository = mockk(),
                    beregningsGrunnlagRepository = mockk(),
                    anvendtTrygdetidRepository = repository,
                    avkortingRepository = mockk(),
                    etteroppgjoerRepository = mockk(),
                    sanksjonRepository = mockk(),
                    behandlingApp = behandlingApp,
                    aktivert = true,
                )
            }

            val response =
                client.post("/api/beregning/intern/anvendt-trygdetid/$behandlingId") {
                    bearerAuth(oauth.issueSystembrukerToken(behandlingApp))
                    contentType(ContentType.Application.Json)
                    setBody(utrekning.toJson())
                }
            response.status shouldBe HttpStatusCode.Created
        }

        val lagredeUtrekninger =
            dataSource.transaction { tx ->
                tx.run(
                    queryOf("SELECT * FROM anvendt_trygdetid WHERE behandling_id = ?", behandlingId)
                        .map { row ->
                            Pair(
                                objectMapper.readTree(row.string("foer_kombinering")),
                                objectMapper.readTree(row.string("etter_kombinering")),
                            )
                        }.asList,
                )
            }
        val forventet = Pair(objectMapper.readTree(utrekning.utrekning.toJson()), objectMapper.readTree(utrekning.anvendt.toJson()))
        lagredeUtrekninger shouldBe listOf(forventet, forventet)
    }
}
