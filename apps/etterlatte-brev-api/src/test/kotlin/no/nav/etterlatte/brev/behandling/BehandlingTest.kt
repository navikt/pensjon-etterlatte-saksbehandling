package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class BehandlingTest {

    @Test
    fun `MapSoeker mapper til Soeker`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(Soeker("Unormal", "Frisk", "Herresykkel", Foedselsnummer("16021254243")), grunnlag.mapSoeker())
    }

    @Test
    fun `MapAvdoed mapper til Avdoed`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(Avdoed("Frisk Mellomstor Gaupe", LocalDate.of(2022, 8, 17)), grunnlag.mapAvdoed())
    }

    @Test
    fun `MapInnsender mapper til Innsender`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(Innsender("Pratsom Trafikkork", Foedselsnummer("11057523044")), grunnlag.mapInnsender())
    }

    @Test
    fun `MapSpraak mapper til Spraak`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(Spraak.NB, grunnlag.mapSpraak())
    }

    @Test
    fun `HentBeloep returnerer korrekt beloep ved kun en periode`() {
        val beregningsperioder = opprettEnkelBeregningsperiode()

        assertEquals(3063, beregningsperioder.hentUtbetaltBeloep())
    }

    @Test
    fun `HentBeloep returnerer korrekt beloep ved to perioder`() {
        val beregningsperioder = opprettToBeregningsperioder()

        assertEquals(3163, beregningsperioder.hentUtbetaltBeloep())
    }

    private fun opprettEnkelBeregningsperiode() = listOf(
        opprettBeregningsperiode(
            LocalDate.now(),
            beloep = 3063
        )
    )

    private fun opprettToBeregningsperioder() = listOf(
        opprettBeregningsperiode(
            LocalDate.of(2022, 11, 1),
            YearMonth.now().minusMonths(1).atEndOfMonth(),
            beloep = 3063
        ),
        opprettBeregningsperiode(
            YearMonth.now().atDay(1),
            beloep = 3163
        )
    )

    private fun opprettBeregningsperiode(fom: LocalDate, tom: LocalDate? = null, beloep: Int) = Beregningsperiode(
        fom,
        tom,
        Kroner(101011),
        0,
        Kroner(beloep),
        10000
    )

    private fun opprettGrunnlag() = GrunnlagTestData(
        opplysningsmapSakOverrides = mapOf(
            Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
            Opplysningstype.INNSENDER_SOEKNAD_V1 to opprettOpplysning(
                objectMapper.readTree("""{"type":"INNSENDER","fornavn":"PRATSOM","etternavn":"TRAFIKKORK","foedselsnummer":"11057523044"}""")
            )
        ),
        opplysningsmapSoekerOverrides = mapOf(
            Opplysningstype.NAVN to opprettOpplysning(Navn("UNORMAL", "FRISK", "HERRESYKKEL").toJsonNode())
        ),
        opplysningsmapAvdoedOverrides = mapOf(
            Opplysningstype.NAVN to opprettOpplysning(Navn("FRISK", "MELLOMSTOR", "GAUPE").toJsonNode())
        )
    ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Tidspunkt.now(), null, null),
            jsonNode
        )
}