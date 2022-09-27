package model.finnSoeskenperiode

import GrunnlagTestData
import grunnlag.ADRESSE_DEFAULT
import grunnlag.HALVSØSKEN_FØDSELSNUMMER
import grunnlag.HELSØSKEN_FØDSELSNUMMER
import grunnlag.kilde
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class FinnSoeskenPeriodeTest {

    @Test
    fun `hentSoeskenperioder skal regnes for helsøsken, og halvsøsken på samme adresse`() {
        val testData = GrunnlagTestData()
        val fom = YearMonth.of(2020, 2)
        val søskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(
                fom,
                YearMonth.now().plusMonths(3),
                listOf(testData.søsken, testData.halvsøsken)
            )
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal splitte når søsken blir 18`() {
        val fødselsdagSøsken = LocalDate.of(2003, 2, 10)
        val `18årsdag` = YearMonth.of(fødselsdagSøsken.year + 18, fødselsdagSøsken.monthValue)
        val testData = GrunnlagTestData(
            opplysningsmapSøskenOverrides = mapOf(
                FOEDSELSDATO to Opplysning.Konstant(kilde, fødselsdagSøsken.toJsonNode())
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val søskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, `18årsdag`, listOf(testData.søsken, testData.halvsøsken)),
            SoeskenPeriode(`18årsdag`.plusMonths(1), tom, listOf(testData.halvsøsken))
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal ikke regne med halvsøsken som bor på en annen adresse`() {
        val testData = GrunnlagTestData(
            opplysningsmapHalvsøskenOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Konstant(
                    kilde,
                    ADRESSE_DEFAULT.copy(adresseLinje1 = "Andebygata 69").toJsonNode()
                )
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val søskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, listOf(testData.søsken))
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `hentSoeskenperioder saksoverrides skal overstyre alt`() {
        val testData = GrunnlagTestData(
            opplysningsmapHalvsøskenOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Konstant(
                    kilde,
                    ADRESSE_DEFAULT.copy(adresseLinje1 = "Andebygata 69").toJsonNode()
                )
            ),
            opplysningsmapSakOverrides = mapOf(
                Opplysningstyper.SOESKEN_I_BEREGNINGEN to Opplysning.Konstant(
                    kilde,
                    Beregningsgrunnlag(
                        listOf(
                            SoeskenMedIBeregning(HALVSØSKEN_FØDSELSNUMMER, true),
                            SoeskenMedIBeregning(HELSØSKEN_FØDSELSNUMMER, false)
                        )
                    ).toJsonNode()
                )
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val søskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, listOf(testData.halvsøsken))
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal returnere tom liste ved ingen søsken`() {
        val søker = GrunnlagTestData().søker
        val testData = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                AVDOEDESBARN to Opplysning.Konstant(kilde, AvdoedesBarn(listOf(søker)).toJsonNode())
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val søskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, emptyList())
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `finnHelOgHalvsøsken skal splitte søsken basert på felles foreldre`() {
        val testData = GrunnlagTestData()
        val helsøsken = testData.søsken
        val halvsøsken = testData.halvsøsken

        assertEquals(
            Søsken(helsøsken = listOf(helsøsken), halvsøsken = listOf(halvsøsken)),
            finnHelOgHalvsøsken(
                søker = testData.hentOpplysningsgrunnlag().søker,
                avdoedesBarn = testData.hentOpplysningsgrunnlag().hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn!!
            )
        )
    }

    @Test
    fun `finnHelOgHalvsøsken skal returnere tomme lister om avdøde kun har søker som barn`() {
        val søker = GrunnlagTestData().søker

        val testData = GrunnlagTestData(
            opplysningsmapAvdødOverrides = mapOf(
                AVDOEDESBARN to Opplysning.Konstant(kilde, AvdoedesBarn(listOf(søker)).toJsonNode())
            )
        )

        assertEquals(
            Søsken(helsøsken = emptyList(), halvsøsken = emptyList()),
            finnHelOgHalvsøsken(
                søker = testData.hentOpplysningsgrunnlag().søker,
                avdoedesBarn = testData.hentOpplysningsgrunnlag().hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn!!
            )
        )
    }
}