package itest

import com.fasterxml.jackson.databind.JsonNode
import grunnlag.statiskUuid
import lagGrunnlagsopplysning
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.BehandlingHendelser
import no.nav.etterlatte.grunnlag.GrunnlagHendelser
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.RealGrunnlagService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver
import java.time.Instant

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

    private val fnr = Foedselsnummer.of("18057404783")
    private val tidspunkt = Instant.now()
    private val kilde = Grunnlagsopplysning.Pdl("pdl", tidspunkt, null, null)
    private val nyOpplysning = Grunnlagsopplysning(
        id = statiskUuid,
        kilde = kilde,
        opplysningType = Opplysningstyper.NAVN,
        meta = objectMapper.createObjectNode(),
        opplysning = "Ola".toJsonNode(),
        attestering = null,
        fnr = fnr
    )

    @Nested
    inner class NyOpplysning {
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
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    @Nested
    inner class Opplysningsbehov {
        private val melding = JsonMessage.newMessage(
            mapOf(
                "@behov" to Opplysningstyper.SOEKER_PDL_V1,
                "opplysning" to listOf(nyOpplysning),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()

        @Test
        fun `svar på opplysningsbehov sender ut melding om at grunnlaget er endret`() {
            inspector.sendTestMessage(melding)
            Assertions.assertEquals("GRUNNLAG:GRUNNLAGENDRET", inspector.inspektør.message(0)[eventNameKey].textValue())
        }

        @Test
        fun `ny enkeltopplysning lagres i databasen med riktige verdier`() {
            assertOpplysningBlirLagret(melding = melding, expectedOpplysning = nyOpplysning)
        }
    }

    @Test
    fun `lagrer og sender ut nytt grunnlag`() {
        val personRolleOpplysning = lagGrunnlagsopplysning(
            opplysningstyper = Opplysningstyper.PERSONROLLE,
            kilde = kilde,
            uuid = statiskUuid,
            verdi = PersonRolle.BARN.toJsonNode(),
            fnr = fnr
        )
        val nyOpplysningMelding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to "OPPLYSNING:NY",
                "opplysning" to listOf(
                    nyOpplysning,
                    personRolleOpplysning
                ),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()
        val behandlingEndretMelding = JsonMessage.newMessage(
            mapOf(
                "@event_name" to "BEHANDLING:GRUNNLAGENDRET",
                "opplysning" to listOf(
                    nyOpplysningMelding,
                    lagGrunnlagsopplysning(
                        opplysningstyper = Opplysningstyper.PERSONROLLE,
                        kilde = kilde,
                        uuid = statiskUuid,
                        verdi = PersonRolle.BARN.toJsonNode(),
                        fnr = fnr
                    )
                ),
                "persongalleri" to Persongalleri(
                    soeker = fnr.value,
                    innsender = null,
                    soesken = listOf(),
                    avdoed = listOf(),
                    gjenlevende = listOf()
                ).toJsonNode(),
                "fnr" to fnr,
                "sakId" to 1
            )
        ).toJson()

        inspector.sendTestMessage(nyOpplysningMelding)
        inspector.sendTestMessage(behandlingEndretMelding)

        val packet = inspector.inspektør.message(1)

        Assertions.assertEquals("BEHANDLING:GRUNNLAGENDRET".toJson(), packet[eventNameKey].toJson())
        Assertions.assertEquals(
            Opplysningsgrunnlag(
                soeker = mapOf(
                    Opplysningstyper.NAVN to Opplysning.Konstant(
                        statiskUuid,
                        nyOpplysning.kilde,
                        nyOpplysning.opplysning
                    ),
                    Opplysningstyper.PERSONROLLE to Opplysning.Konstant(
                        statiskUuid,
                        personRolleOpplysning.kilde,
                        personRolleOpplysning.opplysning
                    )
                ),
                familie = listOf(),
                sak = mapOf(),
                metadata = Metadata(
                    sakId = 1,
                    versjon = 2
                )
            ).toJson(),
            packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagV2Key].toJson()
        )
    }

    private fun assertOpplysningBlirLagret(melding: String, expectedOpplysning: Grunnlagsopplysning<JsonNode>) {
        inspector.sendTestMessage(melding)
        val grunnlagshendelse = opplysningRepo.finnHendelserIGrunnlag(1).first()

        Assertions.assertEquals(grunnlagshendelse.sakId, 1)
        Assertions.assertEquals(grunnlagshendelse.hendelseNummer, 1)

        with(grunnlagshendelse.opplysning) {
            Assertions.assertEquals(this.id, expectedOpplysning.id)
            Assertions.assertEquals(this.kilde, expectedOpplysning.kilde)
            Assertions.assertEquals(this.opplysningType, expectedOpplysning.opplysningType)
            Assertions.assertEquals(this.meta, expectedOpplysning.meta)
            Assertions.assertEquals(this.kilde, expectedOpplysning.kilde)
            Assertions.assertEquals(this.attestering, expectedOpplysning.attestering)
            Assertions.assertEquals(this.fnr!!.value, expectedOpplysning.fnr!!.value)
            Assertions.assertEquals(this.opplysning, expectedOpplysning.opplysning)
        }
    }
}