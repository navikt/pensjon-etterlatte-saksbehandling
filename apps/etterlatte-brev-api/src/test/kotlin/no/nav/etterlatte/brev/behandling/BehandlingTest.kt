package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BehandlingTest {
    private val innsenderJson =
        """{"type":"INNSENDER","fornavn":"PRATSOM","etternavn":"TRAFIKKORK","foedselsnummer":"11057523044"}"""

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
    fun `map med dobbelnavn og bindestrek`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL-KAR", "FRISK-IS", "HERRESYKKEL"),
                avdoedNavn = Navn("RIV-JERN", "KUL-KAR", "BADEBALL-SOMMER"),
            )

        assertEquals(
            Soeker("Unormal-Kar", "Frisk-Is", "Herresykkel", Foedselsnummer("16021254243")),
            grunnlag.mapSoeker(),
        )
        assertEquals(Avdoed("Riv-Jern Kul-Kar Badeball-Sommer", LocalDate.of(2022, 8, 17)), grunnlag.mapAvdoed())
    }

    @Test
    fun `map med dobbelnavn og mellomrom`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL KAR", "FRISK IS", "HERRESYKKEL"),
                avdoedNavn = Navn("RIV JERN", "KUL KAR", "BADEBALL-SOMMER"),
            )

        assertEquals(
            Soeker("Unormal Kar", "Frisk Is", "Herresykkel", Foedselsnummer("16021254243")),
            grunnlag.mapSoeker(),
        )
        assertEquals(Avdoed("Riv Jern Kul Kar Badeball-Sommer", LocalDate.of(2022, 8, 17)), grunnlag.mapAvdoed())
    }

    @Test
    fun `map med dobbelnavn, bindestrek og mellomrom`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL-KAR KIS", "FRISK-IS TAK", "HERRESYKKEL BOM"),
                avdoedNavn = Navn("RIV-JERN TRE", "KUL-KAR GULV", "BADEBALL-SOMMER VINTER"),
            )

        assertEquals(
            Soeker("Unormal-Kar Kis", "Frisk-Is Tak", "Herresykkel Bom", Foedselsnummer("16021254243")),
            grunnlag.mapSoeker(),
        )
        assertEquals(
            Avdoed("Riv-Jern Tre Kul-Kar Gulv Badeball-Sommer Vinter", LocalDate.of(2022, 8, 17)),
            grunnlag.mapAvdoed(),
        )
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

    @Test // TODO ...
    @Disabled("Skrus av inntil håndtering av verge er avklart på tvers av appene i Gjenny")
    fun `Verge finnes i grunnlag`() {
        val soekerFnr = Folkeregisteridentifikator.of("16021254243")
        val forventetVergeNavn = "Test Vergenavn"

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                soekerFnr.toJsonNode(),
                            ),
                    ),
                familie = emptyList(),
                sak =
                    mapOf(
                        Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT to
                            opprettOpplysning(
                                listOf(
                                    VergemaalEllerFremtidsfullmakt(
                                        null,
                                        null,
                                        VergeEllerFullmektig(soekerFnr, forventetVergeNavn, null, false),
                                    ),
                                ).toJsonNode(),
                            ),
                    ),
                metadata = mockk(),
            )

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON)
        assertEquals(forventetVergeNavn, vergeBarnepensjon!!.navn)

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD)
        assertEquals(forventetVergeNavn, vergeOmstillingsstoenad!!.navn)
    }

    @Test
    fun `Ingen verge i grunnlag`() {
        val gjenlevendeNavn = Navn("Elegang", "Mellomstor", "Barnevogn")

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                Folkeregisteridentifikator.of("16021254243").toJsonNode(),
                            ),
                    ),
                familie =
                    listOf(
                        mapOf(
                            Opplysningstype.PERSONROLLE to opprettOpplysning(PersonRolle.GJENLEVENDE.toJsonNode()),
                            Opplysningstype.NAVN to opprettOpplysning(gjenlevendeNavn.toJsonNode()),
                        ),
                    ),
                sak = emptyMap(),
                metadata = mockk(),
            )

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON)
        assertEquals(gjenlevendeNavn.toString(), vergeBarnepensjon!!.navn)

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD)
        assertNull(vergeOmstillingsstoenad, "Verge skal ikke settes for OMS når verge mangler i grunnlaget")
    }

    private fun opprettEnkelBeregningsperiode() =
        listOf(
            opprettBeregningsperiode(
                LocalDate.now(),
                beloep = 3063,
            ),
        )

    private fun opprettToBeregningsperioder() =
        listOf(
            opprettBeregningsperiode(
                LocalDate.of(2022, 11, 1),
                YearMonth.now().minusMonths(1).atEndOfMonth(),
                beloep = 3063,
            ),
            opprettBeregningsperiode(
                YearMonth.now().atDay(1),
                beloep = 3163,
            ),
        )

    private fun opprettBeregningsperiode(
        fom: LocalDate,
        tom: LocalDate? = null,
        beloep: Int,
    ) = Beregningsperiode(
        fom,
        tom,
        Kroner(101011),
        0,
        Kroner(beloep),
        10000,
    )

    private fun opprettGrunnlag(
        soekerNavn: Navn = Navn("UNORMAL", "FRISK", "HERRESYKKEL"),
        avdoedNavn: Navn = Navn("FRISK", "MELLOMSTOR", "GAUPE"),
    ) = GrunnlagTestData(
        opplysningsmapSakOverrides =
            mapOf(
                Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
                Opplysningstype.INNSENDER_SOEKNAD_V1 to
                    opprettOpplysning(
                        objectMapper.readTree(innsenderJson),
                    ),
            ),
        opplysningsmapSoekerOverrides =
            mapOf(
                Opplysningstype.NAVN to opprettOpplysning(soekerNavn.toJsonNode()),
            ),
        opplysningsmapAvdoedOverrides =
            mapOf(
                Opplysningstype.NAVN to opprettOpplysning(avdoedNavn.toJsonNode()),
            ),
    ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            jsonNode,
        )
}
