package no.nav.etterlatte.grunnlag.aldersovergang

import com.fasterxml.jackson.databind.node.TextNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.ktor.issueSaksbehandlerToken
import no.nav.etterlatte.ktor.runServer
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.serialize
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AldersovergangTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var dataSource: DataSource
    private val server = MockOAuth2Server()

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).also { it.migrate() }

        server.start()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    @Test
    fun hentSaker() {
        val sakId = 1000L
        val fnrInnenfor = SOEKER_FOEDSELSNUMMER
        val opplysningDao = OpplysningDao(dataSource)
        opplysningDao.leggTilOpplysning(sakId, Opplysningstype.FOEDSELSDATO, TextNode("2018-01-01"), fnrInnenfor)
        opplysningDao.leggTilOpplysning(sakId, Opplysningstype.FOEDSELSDATO, TextNode("2020-01-01"), fnrInnenfor)
        opplysningDao.leggTilOpplysning(sakId, Opplysningstype.SOEKER_PDL_V1, TextNode("hei, hallo"), fnrInnenfor)

        val sakIdUtenfor = 2000L
        val fnrUtenfor = HELSOESKEN_FOEDSELSNUMMER
        opplysningDao.leggTilOpplysning(sakIdUtenfor, Opplysningstype.FOEDSELSDATO, TextNode("2020-01-01"), fnrUtenfor)
        opplysningDao.leggTilOpplysning(sakIdUtenfor, Opplysningstype.FOEDSELSDATO, TextNode("2022-01-01"), fnrUtenfor)
        opplysningDao.leggTilOpplysning(sakIdUtenfor, Opplysningstype.SOEKER_PDL_V1, TextNode("søsken her"), fnrUtenfor)

        testApplication {
            val response =
                createHttpClient(AldersovergangService(AldersovergangDao(dataSource))).get("api/grunnlag/aldersovergang/2020-01") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, "Bearer ${server.issueSaksbehandlerToken()}")
                    }
                }

            Assertions.assertEquals(HttpStatusCode.OK, response.status)
            Assertions.assertEquals(serialize(listOf(sakId.toString())), response.body<String>())
        }
    }

    private fun OpplysningDao.leggTilOpplysning(
        sakId: Long,
        opplysningTypeSoeker: Opplysningstype,
        node: TextNode,
        fnr: Folkeregisteridentifikator,
    ) = leggOpplysningTilGrunnlag(
        sakId = sakId,
        behandlingsopplysning =
            Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = Grunnlagsopplysning.UkjentInnsender(Tidspunkt.now()),
                opplysningType = opplysningTypeSoeker,
                meta = mockk(),
                opplysning = node,
            ),
        fnr = fnr,
    )

    private fun ApplicationTestBuilder.createHttpClient(service: AldersovergangService): HttpClient =
        runServer(server, "api/grunnlag") {
            aldersovergangRoutes(service)
        }
}
