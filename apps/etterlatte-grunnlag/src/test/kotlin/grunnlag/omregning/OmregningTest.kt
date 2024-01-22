package no.nav.etterlatte.grunnlag.omregning

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.mockk
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmregningTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var dataSource: DataSource

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
    }

    @Test
    fun hentSaker() {
        val sakId = 1L
        val fnrInnenfor = SOEKER_FOEDSELSNUMMER
        val opplysningDao = OpplysningDao(dataSource)
        opplysningDao.leggTilOpplysning(sakId, Opplysningstype.FOEDSELSDATO, TextNode("2020-01-01"), fnrInnenfor)
        opplysningDao.leggTilOpplysning(sakId, Opplysningstype.SOEKER_PDL_V1, TextNode("hei, hallo"), fnrInnenfor)

        val sakIdUtenfor = 2L
        val fnrUtenfor = HELSOESKEN_FOEDSELSNUMMER
        opplysningDao.leggTilOpplysning(sakIdUtenfor, Opplysningstype.FOEDSELSDATO, TextNode("2022-01-01"), fnrUtenfor)
        opplysningDao.leggTilOpplysning(sakIdUtenfor, Opplysningstype.SOEKER_PDL_V1, TextNode("søsken her"), fnrUtenfor)

        val service = OmregningService(OmregningDao(dataSource))
        val saker = service.hentSoekereFoedtIEnGittMaaned(YearMonth.of(2020, Month.JANUARY))
        Assertions.assertEquals(1, saker.size)
        Assertions.assertEquals(sakId.toString(), saker[0])
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
}
