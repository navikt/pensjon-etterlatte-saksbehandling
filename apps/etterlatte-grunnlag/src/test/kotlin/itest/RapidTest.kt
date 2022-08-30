package itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver
import java.time.Instant
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RapidTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var grunnlagService: RealGrunnlagService
    private lateinit var inspector: TestRapid

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dsb.migrate()
        opplysningRepo = OpplysningDao(dsb.dataSource)

        grunnlagService = RealGrunnlagService(opplysningRepo)
        inspector = TestRapid().apply {
            GrunnlagHendelser(this, grunnlagService)
            BehandlingHendelser(this)
            BehandlingEndretHendlese(this, grunnlagService)
            registrerVedlikeholdsriver(grunnlagService)
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private val opplysningsid = UUID.randomUUID()
    private val fnr = Foedselsnummer.of("18057404783")
    private val tidspunkt = Instant.now()
    private val nyOpplysning = Grunnlagsopplysning(
        id = opplysningsid,
        kilde = Grunnlagsopplysning.Pdl("pdl", tidspunkt, null, null),
        opplysningType = Opplysningstyper.NAVN,
        meta = objectMapper.createObjectNode(),
        opplysning = """ "Ola" """,
        attestering = null,
        fnr = fnr
    )

    private val melding = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "OPPLYSNING:NY",
            "opplysning" to listOf(nyOpplysning),
            "fnr" to fnr,
            "sakId" to 1
        )
    ).toJson()

    @Test
    fun `ny enkeltopplysning sender ut melding om at grunnlaget er endret`() {
        inspector.sendTestMessage(melding)
        Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET", inspector.inspektør.message(0)[eventNameKey].textValue())
    }

    @Test
    fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
        inspector.sendTestMessage(melding)
        val grunnlagshendelse = opplysningRepo.finnHendelserIGrunnlag(1).first()

        Assertions.assertEquals(grunnlagshendelse.sakId, 1)
        Assertions.assertEquals(grunnlagshendelse.hendelseNummer, 1)

        with(grunnlagshendelse.opplysning) {
            Assertions.assertEquals(this.id, nyOpplysning.id)
            Assertions.assertEquals(this.kilde, nyOpplysning.kilde)
            Assertions.assertEquals(this.opplysningType, nyOpplysning.opplysningType)
            Assertions.assertEquals(this.meta, nyOpplysning.meta)
            Assertions.assertEquals(this.kilde, nyOpplysning.kilde)
            Assertions.assertEquals(this.attestering, nyOpplysning.attestering)
            Assertions.assertEquals(this.fnr!!.value, nyOpplysning.fnr!!.value)
            Assertions.assertEquals(this.opplysning.textValue(), nyOpplysning.opplysning)
        }
    }

    @Test
    fun `ny enkeltopplysning gir grunnlag v2`() {
        inspector.sendTestMessage(melding)

        Assertions.assertTrue(inspector.inspektør.message(0).has("grunnlagV2"))
    }
}