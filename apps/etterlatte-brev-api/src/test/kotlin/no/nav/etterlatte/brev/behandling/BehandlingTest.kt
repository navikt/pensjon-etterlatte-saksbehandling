package no.nav.etterlatte.brev.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.VERGE_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BehandlingTest {
    private val adresseService: AdresseService = mockk()

    @Test
    fun `MapSoeker mapper til Soeker`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(
            Soeker("Unormal", "Frisk", "Herresykkel", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value), false),
            grunnlag.mapSoeker(null),
        )
    }

    @Test
    fun `MapAvdoed mapper til Avdoed`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(
            listOf(Avdoed(Foedselsnummer("08498224343"), "Frisk Mellomstor Gaupe", LocalDate.of(2022, 8, 17))),
            grunnlag.mapAvdoede(),
        )
    }

    @Test
    fun `map med dobbelnavn og bindestrek`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL-KAR", "FRISK-IS", "HERRESYKKEL"),
                avdoedNavn = Navn("RIV-JERN", "KUL-KAR", "BADEBALL-SOMMER"),
            )

        assertEquals(
            Soeker("Unormal-Kar", "Frisk-Is", "Herresykkel", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value), false),
            grunnlag.mapSoeker(null),
        )
        assertEquals(
            listOf(Avdoed(Foedselsnummer("08498224343"), "Riv-Jern Kul-Kar Badeball-Sommer", LocalDate.of(2022, 8, 17))),
            grunnlag.mapAvdoede(),
        )
    }

    @Test
    fun `map med dobbelnavn og mellomrom`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL KAR", "FRISK IS", "HERRESYKKEL"),
                avdoedNavn = Navn("RIV JERN", "KUL KAR", "BADEBALL-SOMMER"),
            )

        assertEquals(
            Soeker("Unormal Kar", "Frisk Is", "Herresykkel", Foedselsnummer(SOEKER_FOEDSELSNUMMER.value), false),
            grunnlag.mapSoeker(null),
        )
        assertEquals(
            listOf(Avdoed(Foedselsnummer("08498224343"), "Riv Jern Kul Kar Badeball-Sommer", LocalDate.of(2022, 8, 17))),
            grunnlag.mapAvdoede(),
        )
    }

    @Test
    fun `map med dobbelnavn, bindestrek og mellomrom`() {
        val grunnlag =
            opprettGrunnlag(
                soekerNavn = Navn("UNORMAL-KAR KIS", "FRISK-IS TAK", "HERRESYKKEL BOM"),
                avdoedNavn = Navn("RIV-JERN TRE", "KUL-KAR GULV", "BADEBALL-SOMMER VINTER"),
            )

        assertEquals(
            Soeker(
                "Unormal-Kar Kis",
                "Frisk-Is Tak",
                "Herresykkel Bom",
                Foedselsnummer(SOEKER_FOEDSELSNUMMER.value),
                false,
            ),
            grunnlag.mapSoeker(null),
        )
        assertEquals(
            listOf(Avdoed(Foedselsnummer("08498224343"), "Riv-Jern Tre Kul-Kar Gulv Badeball-Sommer Vinter", LocalDate.of(2022, 8, 17))),
            grunnlag.mapAvdoede(),
        )
    }

    @Test
    fun `MapInnsender mapper til Innsender`() {
        val grunnlag = opprettGrunnlag()

        assertEquals(Innsender(Foedselsnummer("11057523044")), grunnlag.mapInnsender())
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

    @Test
    fun `Verge finnes i grunnlag`() {
        val soekerFnr = SOEKER_FOEDSELSNUMMER
        val vergesFnr = VERGE_FOEDSELSNUMMER

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                soekerFnr.toJsonNode(),
                            ),
                        Opplysningstype.FOEDSELSDATO to
                            opprettOpplysning(
                                // 11 år gammel
                                LocalDate.now().minusYears(11).toJsonNode(),
                            ),
                        Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT to
                            opprettOpplysning(
                                listOf(
                                    VergemaalEllerFremtidsfullmakt(
                                        null,
                                        null,
                                        VergeEllerFullmektig(vergesFnr, "Vergenavn", null, false),
                                    ),
                                ).toJsonNode(),
                            ),
                    ),
                familie = emptyList(),
                sak = emptyMap(),
                metadata = mockk(),
            )

        coEvery {
            adresseService.hentMottakerAdresse(any<SakType>(), vergesFnr.value)
        } returns Mottaker("Viggo Verge", null, null, mockk())

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON, null, adresseService)
        assertEquals("Viggo Verge", vergeBarnepensjon!!.navn())

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD, null, adresseService)
        assertEquals("Viggo Verge", vergeOmstillingsstoenad!!.navn())
    }

    @Test
    fun `Ingen verge i grunnlag gjenlevende har foreldreansvar`() {
        val gjenlevendeNavn = Navn("Elegang", "Mellomstor", "Barnevogn")

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                SOEKER_FOEDSELSNUMMER.toJsonNode(),
                            ),
                        Opplysningstype.FOEDSELSDATO to
                            opprettOpplysning(
                                // 11 år gammel
                                LocalDate.now().minusYears(11).toJsonNode(),
                            ),
                        Opplysningstype.FAMILIERELASJON to
                            opprettOpplysning(
                                FamilieRelasjon(
                                    ansvarligeForeldre = listOf(GJENLEVENDE_FOEDSELSNUMMER),
                                    foreldre = listOf(),
                                    barn = listOf(),
                                    personerUtenIdent = listOf(),
                                ).toJsonNode(),
                            ),
                    ),
                familie =
                    listOf(
                        mapOf(
                            Opplysningstype.PERSONROLLE to opprettOpplysning(PersonRolle.GJENLEVENDE.toJsonNode()),
                            Opplysningstype.NAVN to opprettOpplysning(gjenlevendeNavn.toJsonNode()),
                            Opplysningstype.FOEDSELSNUMMER to opprettOpplysning(GJENLEVENDE_FOEDSELSNUMMER.toJsonNode()),
                        ),
                    ),
                sak =
                    mapOf(
                        Opplysningstype.PERSONGALLERI_V1 to
                            opprettOpplysning(
                                Persongalleri(
                                    soeker = SOEKER_FOEDSELSNUMMER.value,
                                    innsender = GJENLEVENDE_FOEDSELSNUMMER.value,
                                    soesken = listOf(),
                                    avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                                    gjenlevende = listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
                                    personerUtenIdent = listOf(),
                                ).toJsonNode(),
                            ),
                    ),
                metadata = mockk(),
            )

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON, null, adresseService)
        assertEquals(gjenlevendeNavn.toString(), vergeBarnepensjon!!.navn())

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD, null, adresseService)
        assertNull(vergeOmstillingsstoenad, "Verge skal ikke settes for OMS når verge mangler i grunnlaget")
    }

    @Test
    fun `Ingen verge i grunnlag gjenlevende har ikke foreldreansvar`() {
        val gjenlevendeNavn = Navn("Elegang", "Mellomstor", "Barnevogn")

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                SOEKER_FOEDSELSNUMMER.toJsonNode(),
                            ),
                        Opplysningstype.FOEDSELSDATO to
                            opprettOpplysning(
                                // 11 år gammel
                                LocalDate.now().minusYears(11).toJsonNode(),
                            ),
                        Opplysningstype.FAMILIERELASJON to
                            opprettOpplysning(
                                FamilieRelasjon(
                                    ansvarligeForeldre = listOf(AVDOED2_FOEDSELSNUMMER),
                                    foreldre = listOf(),
                                    barn = listOf(),
                                    personerUtenIdent = listOf(),
                                ).toJsonNode(),
                            ),
                    ),
                familie =
                    listOf(
                        mapOf(
                            Opplysningstype.PERSONROLLE to opprettOpplysning(PersonRolle.GJENLEVENDE.toJsonNode()),
                            Opplysningstype.NAVN to opprettOpplysning(gjenlevendeNavn.toJsonNode()),
                            Opplysningstype.FOEDSELSNUMMER to opprettOpplysning(GJENLEVENDE_FOEDSELSNUMMER.toJsonNode()),
                        ),
                    ),
                sak = emptyMap(),
                metadata = mockk(),
            )

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON, null, adresseService)
        assertNull(vergeBarnepensjon)

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD, null, adresseService)
        assertNull(vergeOmstillingsstoenad, "Verge skal ikke settes for OMS når verge mangler i grunnlaget")
    }

    @Test
    fun `Ingen verge i grunnlag gjenlevende har foreldreansvar soeker er over 18 ifølge brevutfall`() {
        val gjenlevendeNavn = Navn("Elegang", "Mellomstor", "Barnevogn")

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                SOEKER_FOEDSELSNUMMER.toJsonNode(),
                            ),
                        Opplysningstype.FOEDSELSDATO to
                            opprettOpplysning(
                                // 11 år gammel
                                LocalDate.now().minusYears(11).toJsonNode(),
                            ),
                        Opplysningstype.FAMILIERELASJON to
                            opprettOpplysning(
                                FamilieRelasjon(
                                    ansvarligeForeldre = listOf(GJENLEVENDE_FOEDSELSNUMMER),
                                    foreldre = listOf(),
                                    barn = listOf(),
                                    personerUtenIdent = listOf(),
                                ).toJsonNode(),
                            ),
                    ),
                familie =
                    listOf(
                        mapOf(
                            Opplysningstype.PERSONROLLE to opprettOpplysning(PersonRolle.GJENLEVENDE.toJsonNode()),
                            Opplysningstype.NAVN to opprettOpplysning(gjenlevendeNavn.toJsonNode()),
                            Opplysningstype.FOEDSELSNUMMER to opprettOpplysning(GJENLEVENDE_FOEDSELSNUMMER.toJsonNode()),
                        ),
                    ),
                sak = emptyMap(),
                metadata = mockk(),
            )

        val brevutfallDto =
            BrevutfallDto(
                UUID.randomUUID(),
                Aldersgruppe.OVER_18,
                LavEllerIngenInntekt.NEI,
                Feilutbetaling(FeilutbetalingValg.NEI, null),
                Grunnlagsopplysning.Saksbehandler("Casey", Tidspunkt.now()),
            )
        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON, brevutfallDto, adresseService)
        assertNull(vergeBarnepensjon)

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD, null, adresseService)
        assertNull(vergeOmstillingsstoenad, "Verge skal ikke settes for OMS når verge mangler i grunnlaget")
    }

    @Test
    fun `Ingen verge og over 18 år gir ingen verge barnepensjon eller oms`() {
        val gjenlevendeNavn = Navn("Elegang", "Mellomstor", "Barnevogn")

        val grunnlag =
            Grunnlag(
                soeker =
                    mapOf(
                        Opplysningstype.FOEDSELSNUMMER to
                            opprettOpplysning(
                                SOEKER_FOEDSELSNUMMER.toJsonNode(),
                            ),
                        Opplysningstype.FOEDSELSDATO to
                            opprettOpplysning(
                                // 19 år gammel
                                LocalDate.now().minusYears(19).toJsonNode(),
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

        val vergeBarnepensjon = grunnlag.mapVerge(SakType.BARNEPENSJON, null, adresseService)
        assertNull(vergeBarnepensjon, "Verge skal ikke settes for barnepensjon hvis barnet er over 18 år")

        val vergeOmstillingsstoenad = grunnlag.mapVerge(SakType.OMSTILLINGSSTOENAD, null, adresseService)
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
        null,
        null,
        false,
        BeregningsMetode.NASJONAL,
        BeregningsMetode.BEST,
    )

    private fun opprettGrunnlag(
        soekerNavn: Navn = Navn("UNORMAL", "FRISK", "HERRESYKKEL"),
        avdoedNavn: Navn = Navn("FRISK", "MELLOMSTOR", "GAUPE"),
    ) = GrunnlagTestData(
        opplysningsmapSakOverrides =
            mapOf(
                Opplysningstype.SPRAAK to opprettOpplysning(Spraak.NB.toJsonNode()),
            ),
        opplysningsmapSoekerOverrides =
            mapOf(
                Opplysningstype.NAVN to opprettOpplysning(soekerNavn.toJsonNode()),
            ),
        opplysningsmapAvdoedOverrides =
            mapOf(
                Opplysningstype.NAVN to opprettOpplysning(avdoedNavn.toJsonNode()),
            ),
        opplysningsmapGjenlevendeOverrides =
            mapOf(
                Opplysningstype.FOEDSELSNUMMER to
                    opprettOpplysning(
                        Folkeregisteridentifikator.of("11057523044").toJsonNode(),
                    ),
            ),
    ).hentOpplysningsgrunnlag()

    private fun opprettOpplysning(jsonNode: JsonNode) =
        Opplysning.Konstant(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            jsonNode,
        )
}
