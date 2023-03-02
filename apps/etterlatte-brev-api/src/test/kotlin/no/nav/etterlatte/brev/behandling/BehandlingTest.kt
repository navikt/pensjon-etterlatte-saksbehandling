package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class BehandlingTest {

    @Test
    fun `MapSoeker mapper til Soeker`() {
        val grunnlag = opprettGrunnlag()

        Assertions.assertEquals(Soeker("Søker Barn", "16021254243"), grunnlag.mapSoeker())
    }

    @Test
    fun `MapAvdoed mapper til Avdoed`() {
        val grunnlag = opprettGrunnlag()

        Assertions.assertEquals(Avdoed("Død Far", LocalDate.of(2022, 8, 17)), grunnlag.mapAvdoed())
    }

    @Test
    fun `MapInnsender mapper til Innsender`() {
        val grunnlag = opprettGrunnlag()

        Assertions.assertEquals(Innsender("Innsend Innsender", "11057523044"), grunnlag.mapInnsender())
    }

    @Test
    fun `MapSpraak mapper til Spraak`() {
        val grunnlag = opprettGrunnlag()

        Assertions.assertEquals(Spraak.NB, grunnlag.mapSpraak())
    }

    @Test
    fun `HentBeloep returnerer korrekt beloep ved kun en periode`() {
        val beregningsperioder = opprettEnkelBeregningsperiode()

        Assertions.assertEquals(3063, beregningsperioder.hentUtbetaltBeloep())
    }

    @Test
    fun `HentBeloep returnerer korrekt beloep ved to perioder`() {
        val beregningsperioder = opprettToBeregningsperioder()

        Assertions.assertEquals(3163, beregningsperioder.hentUtbetaltBeloep())
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
        101011,
        0,
        beloep,
        10000
    )

    private fun opprettGrunnlag() = GrunnlagTestData(
        opplysningsmapSakOverrides = mapOf(
            Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
            Opplysningstype.INNSENDER_SOEKNAD_V1 to opprettOpplysning(
                InnsenderSoeknad(
                    PersonType.INNSENDER,
                    "Innsend",
                    "Innsender",
                    Foedselsnummer.of("11057523044")
                ).toJsonNode()
            )
        )
    ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Tidspunkt.now().instant, null, null),
            jsonNode
        )
}