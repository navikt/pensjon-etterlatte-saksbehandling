package no.nav.etterlatte.vilkaarsvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import no.nav.etterlatte.vilkaarsvurdering.services.VilkaarsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.SAK_ID_KEY
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

internal class MigreringVilkaarsvurderingRiverTest {
    private val vilkaarsvurderingServiceMock =
        mockk<VilkaarsvurderingService> {
            coEvery { migrer(any(), any()) } returns mockk()
        }
    private val testRapid =
        TestRapid()
            .apply { MigreringVilkaarsvurderingRiver(this, vilkaarsvurderingServiceMock) }

    @Test
    fun `tar opp migrer vilkaarsvurdering-event, kopierer vilkaarsvurdering og poster ny BEREGN-melding`() {
        val behandlingId = UUID.randomUUID()

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

        val melding =
            JsonMessage.newMessage(
                mapOf(
                    EVENT_NAME_KEY to Migreringshendelser.VILKAARSVURDER,
                    BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1.name,
                    SAK_ID_KEY to 1,
                    BEHANDLING_ID_KEY to behandlingId,
                    HENDELSE_DATA_KEY to request,
                ),
            ).toJson()
        testRapid.sendTestMessage(melding)

        coVerify(exactly = 1) {
            vilkaarsvurderingServiceMock.migrer(behandlingId, false)
        }
        with(testRapid.inspektør.message(0)) {
            assertEquals(Migreringshendelser.TRYGDETID, this[EVENT_NAME_KEY].asText())
        }
    }
}
