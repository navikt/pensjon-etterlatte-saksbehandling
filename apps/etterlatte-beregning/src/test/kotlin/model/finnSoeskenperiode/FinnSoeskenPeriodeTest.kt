package no.nav.etterlatte.model.finnSoeskenperiode

import model.finnSoeskenperiode.FinnSoeskenPeriode
import model.finnSoeskenperiode.Soesken
import model.finnSoeskenperiode.finnHelOgHalvsoesken
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID.randomUUID

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
                listOf(testData.soesken, testData.halvsoesken)
            )
        )

        assertEquals(expected, søskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal splitte når søsken blir 18`() {
        val foedselsdagSoesken = LocalDate.of(2003, 2, 10)
        val `18aarsdag` = YearMonth.of(foedselsdagSoesken.year + 18, foedselsdagSoesken.monthValue)
        val testData = GrunnlagTestData(
            opplysningsmapSoeskenOverrides = mapOf(
                FOEDSELSDATO to Opplysning.Konstant(randomUUID(), kilde, foedselsdagSoesken.toJsonNode())
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val soeskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, `18aarsdag`, listOf(testData.soesken, testData.halvsoesken)),
            SoeskenPeriode(`18aarsdag`.plusMonths(1), tom, listOf(testData.halvsoesken))
        )

        assertEquals(expected, soeskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal ikke regne med halvsøsken som bor på en annen adresse`() {
        val testData = GrunnlagTestData(
            opplysningsmapHalvsoeskenOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    ADRESSE_DEFAULT.map {
                        PeriodisertOpplysning(
                            randomUUID(),
                            kilde,
                            it.copy(adresseLinje1 = "Andebygata 69").toJsonNode(),
                            fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                            tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
                        )
                    }
                )
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val soeskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, listOf(testData.soesken))
        )

        assertEquals(expected, soeskenperiode)
    }

    @Test
    fun `hentSoeskenperioder saksoverrides skal overstyre alt`() {
        val testData = GrunnlagTestData(
            opplysningsmapHalvsoeskenOverrides = mapOf(
                BOSTEDSADRESSE to Opplysning.Periodisert(
                    ADRESSE_DEFAULT.map {
                        PeriodisertOpplysning(
                            randomUUID(),
                            kilde,
                            it.toJsonNode(),
                            fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                            tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
                        )
                    }
                )
            ),
            opplysningsmapSakOverrides = mapOf(
                Opplysningstype.SOESKEN_I_BEREGNINGEN to Opplysning.Konstant(
                    randomUUID(),
                    kilde,
                    Beregningsgrunnlag(
                        listOf(
                            SoeskenMedIBeregning(HALVSOESKEN_FOEDSELSNUMMER, true),
                            SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, false)
                        )
                    ).toJsonNode()
                )
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val soeskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, listOf(testData.halvsoesken))
        )

        assertEquals(expected, soeskenperiode)
    }

    @Test
    fun `hentSoeskenperioder skal returnere tom liste ved ingen søsken`() {
        val soeker = GrunnlagTestData().soeker
        val testData = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                AVDOEDESBARN to Opplysning.Konstant(randomUUID(), kilde, AvdoedesBarn(listOf(soeker)).toJsonNode())
            )
        )
        val fom = YearMonth.of(2020, 2)
        val tom = YearMonth.now().plusMonths(3)
        val soeskenperiode = FinnSoeskenPeriode(testData.hentOpplysningsgrunnlag(), fom).hentSoeskenperioder()
        val expected = listOf(
            SoeskenPeriode(fom, tom, emptyList())
        )

        assertEquals(expected, soeskenperiode)
    }

    @Test
    fun `finnHelOgHalvsøsken skal splitte søsken basert på felles foreldre`() {
        val testData = GrunnlagTestData()
        val helsoesken = testData.soesken
        val halvsoesken = testData.halvsoesken

        assertEquals(
            Soesken(helsoesken = listOf(helsoesken), halvsoesken = listOf(halvsoesken)),
            finnHelOgHalvsoesken(
                soeker = testData.hentOpplysningsgrunnlag().soeker,
                avdoedesBarn = testData.hentOpplysningsgrunnlag().hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn!!
            )
        )
    }

    @Test
    fun `finnHelOgHalvsøsken skal returnere tomme lister om avdøde kun har søker som barn`() {
        val soeker = GrunnlagTestData().soeker

        val testData = GrunnlagTestData(
            opplysningsmapAvdoedOverrides = mapOf(
                AVDOEDESBARN to Opplysning.Konstant(randomUUID(), kilde, AvdoedesBarn(listOf(soeker)).toJsonNode())
            )
        )

        assertEquals(
            Soesken(helsoesken = emptyList(), halvsoesken = emptyList()),
            finnHelOgHalvsoesken(
                soeker = testData.hentOpplysningsgrunnlag().soeker,
                avdoedesBarn = testData.hentOpplysningsgrunnlag().hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn!!
            )
        )
    }
}