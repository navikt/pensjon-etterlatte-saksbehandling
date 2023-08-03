import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.TRYGDETID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.trygdetid.kafka.MigreringHendelser
import no.nav.etterlatte.trygdetid.kafka.TrygdetidService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.*

internal class MigreringHendelserTest {

    private val trygdetidService = mockk<TrygdetidService>()
    private val inspector = TestRapid().apply { MigreringHendelser(this, trygdetidService) }

    @Test
    fun `skal oppdatere og beregne trygdetid og returnere siste beregning`() {
        val behandlingId = slot<UUID>()
        val trygdetidDto = TrygdetidDto(
            id = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            beregnetTrygdetid = BeregnetTrygdetidDto(40, Tidspunkt.now()),
            trygdetidGrunnlag = emptyList(),
            opplysninger = GrunnlagOpplysningerDto(
                avdoedDoedsdato = null,
                avdoedFoedselsdato = null,
                avdoedFylteSeksten = null,
                avdoedFyllerSeksti = null
            )
        )
        val fnr = Folkeregisteridentifikator.of("12101376212")
        val request = MigreringRequest(
            pesysId = PesysId("1"),
            enhet = Enhet("4817"),
            soeker = fnr,
            avdoedForelder = listOf(AvdoedForelder(fnr, Tidspunkt.now())),
            gjenlevendeForelder = null,
            virkningstidspunkt = YearMonth.now(),
            beregning = Beregning(
                BigDecimal(1000),
                BigDecimal(1000),
                BigDecimal(40),
                Tidspunkt.now(),
                BigDecimal(100000),
                "",
                "",
                "",
                ""
            ),
            trygdetidsPerioder = listOf(
                Trygdetidsgrunnlag(
                    trygdetidGrunnlagId = 1L,
                    personGrunnlagId = 2L,
                    landTreBokstaver = "NOR",
                    datoFom = Tidspunkt.ofNorskTidssone(LocalDate.parse("2000-01-01"), LocalTime.of(0, 0, 0)),
                    datoTom = Tidspunkt.ofNorskTidssone(LocalDate.parse("2015-01-01"), LocalTime.of(0, 0, 0)),
                    poengIInnAar = false,
                    poengIUtAar = false,
                    ikkeIProrata = false,
                    faktiskTrygdetid = BigDecimal(20.5),
                    fremtidigTrygdetid = BigDecimal(15),
                    anvendtTrygdetid = BigDecimal(35.5)
                ),
                Trygdetidsgrunnlag(
                    trygdetidGrunnlagId = 3L,
                    personGrunnlagId = 2L,
                    landTreBokstaver = "SWE",
                    datoFom = Tidspunkt.ofNorskTidssone(LocalDate.parse("2017-01-01"), LocalTime.of(0, 0, 0)),
                    datoTom = Tidspunkt.ofNorskTidssone(LocalDate.parse("2020-01-01"), LocalTime.of(0, 0, 0)),
                    poengIInnAar = false,
                    poengIUtAar = false,
                    ikkeIProrata = false,
                    faktiskTrygdetid = BigDecimal(20.5),
                    fremtidigTrygdetid = BigDecimal(15),
                    anvendtTrygdetid = BigDecimal(35.5)
                )
            )
        )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns mockk()
        every {
            trygdetidService.beregnTrygdetidGrunnlag(
                capture(behandlingId),
                any()
            )
        } returns trygdetidDto

        val melding = JsonMessage.newMessage(
            Migreringshendelser.TRYGDETID,
            mapOf(
                BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                HENDELSE_DATA_KEY to request
            )
        )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        assertEquals(
            trygdetidDto,
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText())
        )
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 2) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
    }

    @Test
    fun `skal ikke opprette grunnlagsperioder dersom det ikke finnes perioder`() {
        val behandlingId = slot<UUID>()
        val trygdetidDto = TrygdetidDto(
            id = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            beregnetTrygdetid = BeregnetTrygdetidDto(40, Tidspunkt.now()),
            trygdetidGrunnlag = emptyList(),
            opplysninger = GrunnlagOpplysningerDto(
                avdoedDoedsdato = null,
                avdoedFoedselsdato = null,
                avdoedFylteSeksten = null,
                avdoedFyllerSeksti = null
            )
        )
        val fnr = Folkeregisteridentifikator.of("12101376212")
        val request = MigreringRequest(
            pesysId = PesysId("1"),
            enhet = Enhet("4817"),
            soeker = fnr,
            avdoedForelder = listOf(AvdoedForelder(fnr, Tidspunkt.now())),
            gjenlevendeForelder = null,
            virkningstidspunkt = YearMonth.now(),
            beregning = Beregning(
                BigDecimal(1000),
                BigDecimal(1000),
                BigDecimal(40),
                Tidspunkt.now(),
                BigDecimal(100000),
                "",
                "",
                "",
                ""
            ),
            trygdetidsPerioder = emptyList()
        )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns trygdetidDto
        every { trygdetidService.beregnTrygdetidGrunnlag(any(), any()) } returns trygdetidDto

        val melding = JsonMessage.newMessage(
            Migreringshendelser.TRYGDETID,
            mapOf(
                BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                HENDELSE_DATA_KEY to request
            )
        )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        assertEquals(
            trygdetidDto,
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText())
        )
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 0) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
    }
}