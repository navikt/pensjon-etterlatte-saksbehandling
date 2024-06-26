package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode as CommonBeregningsperiode

class BeregningTest {
    @Test
    fun `kan mappe overstyrt beregning fra dto`() {
        val dto =
            BeregningDTO(
                beregningId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                type = Beregningstype.BP,
                beregningsperioder = listOf(),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(sakId = 0, versjon = 0),
                overstyrBeregning = OverstyrBeregningDTO("Overstyrt i pesys", OverstyrtBeregningKategori.UKJENT_KATEGORI),
            )
        val statistikkBeregning = Beregning.fraBeregningDTO(dto)
        Assertions.assertTrue(statistikkBeregning.overstyrtBeregning == true)
    }

    @Test
    fun `mapper felter for trygdetid i beregningsperioder riktig`() {
        val periode1 =
            CommonBeregningsperiode(
                datoFOM = YearMonth.now(),
                datoTOM = YearMonth.now(),
                utbetaltBeloep = 23,
                soeskenFlokk = listOf(),
                institusjonsopphold = null,
                grunnbelopMnd = 0,
                grunnbelop = 0,
                trygdetid = 23,
                trygdetidForIdent = null,
                beregningsMetode = BeregningsMetode.NASJONAL,
                samletNorskTrygdetid = 223,
                samletTeoretiskTrygdetid = 223,
                broek = null,
                regelResultat = null,
                regelVersjon = null,
                kilde = null,
            )
        val periode2 =
            CommonBeregningsperiode(
                datoFOM = YearMonth.now().plusMonths(1),
                datoTOM = null,
                utbetaltBeloep = 24,
                soeskenFlokk = listOf(),
                institusjonsopphold = null,
                grunnbelopMnd = 0,
                grunnbelop = 0,
                trygdetid = 0,
                trygdetidForIdent = null,
                beregningsMetode = BeregningsMetode.PRORATA,
                samletNorskTrygdetid = 12,
                samletTeoretiskTrygdetid = 23,
                broek = IntBroek(12, 23),
                regelResultat = null,
                regelVersjon = null,
                kilde = null,
            )
        val beregningDto =
            BeregningDTO(
                beregningId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                type = Beregningstype.BP,
                beregningsperioder =
                    listOf(
                        periode1,
                        periode2,
                    ),
                beregnetDato = Tidspunkt.now(),
                grunnlagMetadata = Metadata(sakId = 0, versjon = 0),
                overstyrBeregning = null,
            )

        val statistikkBeregning = Beregning.fraBeregningDTO(beregningDto)
        Assertions.assertEquals(2, statistikkBeregning.beregningsperioder.size)

        val statistikkPeriode1 = statistikkBeregning.beregningsperioder.first()
        Assertions.assertEquals(periode1.beregningsMetode, statistikkPeriode1.beregningsMetode)
        Assertions.assertEquals(periode1.samletNorskTrygdetid, statistikkPeriode1.samletNorskTrygdetid)
        Assertions.assertEquals(periode1.samletTeoretiskTrygdetid, statistikkPeriode1.samletTeoretiskTrygdetid)
        Assertions.assertEquals(periode1.broek, statistikkPeriode1.broek)

        val statistikkPeriode2 = statistikkBeregning.beregningsperioder.last()
        Assertions.assertEquals(periode2.beregningsMetode, statistikkPeriode2.beregningsMetode)
        Assertions.assertEquals(periode2.samletNorskTrygdetid, statistikkPeriode2.samletNorskTrygdetid)
        Assertions.assertEquals(periode2.samletTeoretiskTrygdetid, statistikkPeriode2.samletTeoretiskTrygdetid)
        Assertions.assertEquals(periode2.broek, statistikkPeriode2.broek)
    }
}
