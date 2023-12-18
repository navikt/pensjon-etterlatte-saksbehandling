import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.TRYGDETID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.etterlatte.trygdetid.kafka.MigreringHendelserRiver
import no.nav.etterlatte.trygdetid.kafka.TrygdetidService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

internal class MigreringHendelserRiverTest {
    private val trygdetidService = mockk<TrygdetidService>()
    private val inspector = TestRapid().apply { MigreringHendelserRiver(this, trygdetidService) }

    @Test
    fun `skal oppdatere og beregne trygdetid og returnere siste beregning`() {
        val behandlingId = slot<UUID>()
        val trygdetidDto =
            TrygdetidDto(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
                        Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger =
                    GrunnlagOpplysningerDto(
                        avdoedDoedsdato = null,
                        avdoedFoedselsdato = null,
                        avdoedFylteSeksten = null,
                        avdoedFyllerSeksti = null,
                    ),
                overstyrtNorskPoengaar = null,
                ident = AVDOED_FOEDSELSNUMMER.value,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = SOEKER_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                dodAvYrkesskade = false,
                gjenlevendeForelder = null,
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 3500,
                        netto = 3500,
                        anvendtTrygdetid = 40,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                    ),
                trygdetid =
                    Trygdetid(
                        listOf(
                            Trygdetidsgrunnlag(
                                trygdetidGrunnlagId = 1L,
                                personGrunnlagId = 2L,
                                landTreBokstaver = "NOR",
                                datoFom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2000-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                datoTom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2015-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                poengIInnAar = false,
                                poengIUtAar = false,
                                ikkeIProrata = false,
                            ),
                            Trygdetidsgrunnlag(
                                trygdetidGrunnlagId = 3L,
                                personGrunnlagId = 2L,
                                landTreBokstaver = "SWE",
                                datoFom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2017-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                datoTom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2020-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                poengIInnAar = false,
                                poengIUtAar = false,
                                ikkeIProrata = false,
                            ),
                        ),
                    ),
                spraak = Spraak.NN,
            )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns mockk()
        every {
            trygdetidService.beregnTrygdetidGrunnlag(
                capture(behandlingId),
                any(),
            )
        } returns trygdetidDto

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.TRYGDETID,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        assertEquals(
            trygdetidDto,
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText()),
        )
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 2) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
    }

    @Test
    fun `skal overstyre resultat dersom det ikke finnes perioder`() {
        val behandlingId = slot<UUID>()
        val beregnetTrygdetid = slot<DetaljertBeregnetTrygdetidResultat>()
        val trygdetidDto =
            TrygdetidDto(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
                        Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger =
                    GrunnlagOpplysningerDto(
                        avdoedDoedsdato = null,
                        avdoedFoedselsdato = null,
                        avdoedFylteSeksten = null,
                        avdoedFyllerSeksti = null,
                    ),
                overstyrtNorskPoengaar = null,
                ident = AVDOED_FOEDSELSNUMMER.value,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = SOEKER_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                dodAvYrkesskade = false,
                gjenlevendeForelder = null,
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 3500,
                        netto = 3500,
                        anvendtTrygdetid = 40,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                    ),
                trygdetid = Trygdetid(emptyList()),
                spraak = Spraak.NN,
            )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns trygdetidDto
        every { trygdetidService.beregnTrygdetidGrunnlag(any(), any()) } returns trygdetidDto
        every {
            trygdetidService.overstyrBeregnetTrygdetid(
                any(),
                capture(beregnetTrygdetid),
            )
        } returns
            trygdetidDto.copy(
                beregnetTrygdetid =
                    trygdetidDto.beregnetTrygdetid!!.copy(
                        resultat =
                            trygdetidDto.beregnetTrygdetid!!.resultat.copy(
                                overstyrt = true,
                            ),
                    ),
            )

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.TRYGDETID,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val trygdetidKafka: TrygdetidDto =
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText())
        assertTrue(beregnetTrygdetid.captured.overstyrt)
        assertTrue(trygdetidKafka.beregnetTrygdetid!!.resultat.overstyrt)
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 0) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
        coVerify(exactly = 1) {
            trygdetidService.overstyrBeregnetTrygdetid(
                behandlingId.captured,
                beregnetTrygdetid.captured,
            )
        }
    }

    @Test
    fun `skal overstyre resultat dersom det feiler naar man legger til periode fra Pesys`() {
        val behandlingId = slot<UUID>()
        val beregnetTrygdetid = slot<DetaljertBeregnetTrygdetidResultat>()
        val trygdetidDto =
            TrygdetidDto(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
                        Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger =
                    GrunnlagOpplysningerDto(
                        avdoedDoedsdato = null,
                        avdoedFoedselsdato = null,
                        avdoedFylteSeksten = null,
                        avdoedFyllerSeksti = null,
                    ),
                overstyrtNorskPoengaar = null,
                ident = AVDOED_FOEDSELSNUMMER.value,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = SOEKER_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                dodAvYrkesskade = false,
                gjenlevendeForelder = null,
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 3500,
                        netto = 3500,
                        anvendtTrygdetid = 40,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                    ),
                trygdetid =
                    Trygdetid(
                        listOf(
                            Trygdetidsgrunnlag(
                                trygdetidGrunnlagId = 1L,
                                personGrunnlagId = 2L,
                                landTreBokstaver = "NOR",
                                datoFom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2000-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                datoTom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2015-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                poengIInnAar = false,
                                poengIUtAar = false,
                                ikkeIProrata = false,
                            ),
                        ),
                    ),
                spraak = Spraak.NN,
            )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns trygdetidDto
        every {
            trygdetidService.beregnTrygdetidGrunnlag(
                any(),
                any(),
            )
        } throws Exception("Noe feil skjedde ved opprettelse av periode")
        every {
            trygdetidService.overstyrBeregnetTrygdetid(
                any(),
                capture(beregnetTrygdetid),
            )
        } returns
            trygdetidDto.copy(
                beregnetTrygdetid =
                    trygdetidDto.beregnetTrygdetid!!.copy(
                        resultat =
                            trygdetidDto.beregnetTrygdetid!!.resultat.copy(
                                overstyrt = true,
                            ),
                    ),
            )

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.TRYGDETID,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val trygdetidKafka: TrygdetidDto =
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText())
        assertTrue(beregnetTrygdetid.captured.overstyrt)
        assertTrue(trygdetidKafka.beregnetTrygdetid!!.resultat.overstyrt)
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
        coVerify(exactly = 1) {
            trygdetidService.overstyrBeregnetTrygdetid(
                behandlingId.captured,
                beregnetTrygdetid.captured,
            )
        }
    }

    @Test
    fun `skal overstyre resultat dersom det ikke stemmer overens med anvendt fra Pesys`() {
        val behandlingId = slot<UUID>()
        val beregnetTrygdetid = slot<DetaljertBeregnetTrygdetidResultat>()
        val trygdetidDto =
            TrygdetidDto(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
                        Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger =
                    GrunnlagOpplysningerDto(
                        avdoedDoedsdato = null,
                        avdoedFoedselsdato = null,
                        avdoedFylteSeksten = null,
                        avdoedFyllerSeksti = null,
                    ),
                overstyrtNorskPoengaar = null,
                ident = AVDOED_FOEDSELSNUMMER.value,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = SOEKER_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                dodAvYrkesskade = false,
                gjenlevendeForelder = null,
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 3500,
                        netto = 3500,
                        anvendtTrygdetid = 30,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                    ),
                trygdetid =
                    Trygdetid(
                        listOf(
                            Trygdetidsgrunnlag(
                                trygdetidGrunnlagId = 1L,
                                personGrunnlagId = 2L,
                                landTreBokstaver = "NOR",
                                datoFom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2000-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                datoTom =
                                    Tidspunkt.ofNorskTidssone(
                                        LocalDate.parse("2020-01-01"),
                                        LocalTime.of(0, 0, 0),
                                    ),
                                poengIInnAar = false,
                                poengIUtAar = false,
                                ikkeIProrata = false,
                            ),
                        ),
                    ),
                spraak = Spraak.NN,
            )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns trygdetidDto
        every { trygdetidService.reberegnUtenFremtidigTrygdetid(capture(behandlingId)) } returns
            trygdetidDto.copy(
                trygdetidGrunnlag = trygdetidDto.trygdetidGrunnlag.filter { it.type == TrygdetidType.FAKTISK.toString() },
            )
        every {
            trygdetidService.beregnTrygdetidGrunnlag(
                any(),
                any(),
            )
        } returns trygdetidDto
        every {
            trygdetidService.overstyrBeregnetTrygdetid(
                any(),
                capture(beregnetTrygdetid),
            )
        } returns
            trygdetidDto.copy(
                beregnetTrygdetid =
                    trygdetidDto.beregnetTrygdetid!!.copy(
                        resultat =
                            trygdetidDto.beregnetTrygdetid!!.resultat.copy(
                                overstyrt = true,
                            ),
                    ),
            )

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.TRYGDETID,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        val trygdetidKafka: TrygdetidDto =
            objectMapper.readValue<TrygdetidDto>(inspector.inspektør.message(0).get(TRYGDETID_KEY).asText())
        assertTrue(beregnetTrygdetid.captured.overstyrt)
        assertTrue(trygdetidKafka.beregnetTrygdetid!!.resultat.overstyrt)
        assertEquals(request.beregning.anvendtTrygdetid, beregnetTrygdetid.captured.samletTrygdetidNorge)
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
        coVerify(exactly = 1) { trygdetidService.reberegnUtenFremtidigTrygdetid(behandlingId.captured) }
        coVerify(exactly = 1) {
            trygdetidService.overstyrBeregnetTrygdetid(
                behandlingId.captured,
                beregnetTrygdetid.captured,
            )
        }
    }

    @Test
    fun `skal opprette eget grunnlag for yrkesskade`() {
        val behandlingId = slot<UUID>()
        val trygdetidDto =
            TrygdetidDto(
                id = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
                        Tidspunkt.now(),
                    ),
                trygdetidGrunnlag = emptyList(),
                opplysninger =
                    GrunnlagOpplysningerDto(
                        avdoedDoedsdato = null,
                        avdoedFoedselsdato = null,
                        avdoedFylteSeksten = null,
                        avdoedFyllerSeksti = null,
                    ),
                overstyrtNorskPoengaar = null,
                ident = AVDOED_FOEDSELSNUMMER.value,
                opplysningerDifferanse = OpplysningerDifferanse(false, mockk<GrunnlagOpplysningerDto>()),
            )
        val request =
            MigreringRequest(
                pesysId = PesysId(1),
                enhet = Enhet("4817"),
                soeker = SOEKER_FOEDSELSNUMMER,
                avdoedForelder = listOf(AvdoedForelder(AVDOED_FOEDSELSNUMMER, Tidspunkt.now())),
                dodAvYrkesskade = true,
                gjenlevendeForelder = null,
                foersteVirkningstidspunkt = YearMonth.now(),
                beregning =
                    Beregning(
                        brutto = 3500,
                        netto = 3500,
                        anvendtTrygdetid = 40,
                        datoVirkFom = Tidspunkt.now(),
                        prorataBroek = null,
                        g = 100_000,
                    ),
                trygdetid = Trygdetid(emptyList()),
                spraak = Spraak.NN,
            )
        every { trygdetidService.beregnTrygdetid(capture(behandlingId)) } returns trygdetidDto
        every { trygdetidService.beregnTrygdetidGrunnlag(any(), any()) } returns trygdetidDto
        every { trygdetidService.opprettGrunnlagVedYrkesskade(any()) } returns trygdetidDto

        val melding =
            JsonMessage.newMessage(
                Migreringshendelser.TRYGDETID,
                mapOf(
                    BEHANDLING_ID_KEY to "a9d42eb9-561f-4320-8bba-2ba600e66e21",
                    VILKAARSVURDERT_KEY to "vilkaarsvurdert",
                    HENDELSE_DATA_KEY to request,
                ),
            )

        inspector.sendTestMessage(melding.toJson())

        assertEquals(UUID.fromString("a9d42eb9-561f-4320-8bba-2ba600e66e21"), behandlingId.captured)
        assertEquals(1, inspector.inspektør.size)
        coVerify(exactly = 1) { trygdetidService.beregnTrygdetid(behandlingId.captured) }
        coVerify(exactly = 0) { trygdetidService.beregnTrygdetidGrunnlag(behandlingId.captured, any()) }
        coVerify(exactly = 0) { trygdetidService.overstyrBeregnetTrygdetid(behandlingId.captured, any()) }
        coVerify(exactly = 1) { trygdetidService.opprettGrunnlagVedYrkesskade(behandlingId.captured) }
    }
}
