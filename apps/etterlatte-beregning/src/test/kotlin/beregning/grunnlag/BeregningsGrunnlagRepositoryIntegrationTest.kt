package no.nav.etterlatte.beregning.grunnlag

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRepositoryIntegrationTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        @JvmStatic
        fun overstyrtAarsaker() =
            listOf(
                Arguments.of("ANNET"),
                Arguments.of("AVKORTET_UFOERETRYGD"),
                Arguments.of("AVKORTET_FENGSEL"),
            )
    }

    private val repository = BeregningsGrunnlagRepository(dataSource)

    private val foerstePeriodeFra = LocalDate.of(2022, 8, 1)

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `Opprettelse fungerer`() {
        val id = UUID.randomUUID()

        val soeskenMedIBeregning =
            listOf(SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true)).somPeriodisertGrunnlag()
        val institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )
        val beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag()
        val kunEnJuridiskForelder = GrunnlagMedPeriode(TomVerdi, LocalDate.of(2022, 8, 1), null)

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                institusjonsoppholdBeregningsgrunnlag,
                beregningsMetode,
                listOf(
                    GrunnlagMedPeriode(
                        fom = LocalDate.of(2022, 8, 1),
                        tom = null,
                        data = BeregningsmetodeForAvdoed(AVDOED_FOEDSELSNUMMER.value, beregningsMetode),
                    ),
                ),
                soeskenMedIBeregning,
                kunEnJuridiskForelder,
            ),
        )

        val result = repository.finnBeregningsGrunnlag(id)

        assertNotNull(result)

        assertEquals(soeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(institusjonsoppholdBeregningsgrunnlag, result?.institusjonsopphold)
        assertEquals(beregningsMetode, result?.beregningsMetode)
        assertEquals(kunEnJuridiskForelder, result?.kunEnJuridiskForelder)
    }

    @Test
    fun `Opprettelse fungerer for OMS`() {
        val id = UUID.randomUUID()

        val institusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )
        val beregningsMetode = BeregningsMetode.NASJONAL.toGrunnlag()

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                institusjonsoppholdBeregningsgrunnlag,
                beregningsMetode,
            ),
        )

        val result = repository.finnBeregningsGrunnlag(id)

        assertNotNull(result)

        assertEquals(institusjonsoppholdBeregningsgrunnlag, result?.institusjonsopphold)
        assertEquals(beregningsMetode, result?.beregningsMetode)
    }

    @Test
    fun `Oppdatering fungerer`() {
        val id = UUID.randomUUID()

        val initialSoeskenMedIBeregning =
            listOf(SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true)).somPeriodisertGrunnlag()
        val oppdatertSoeskenMedIBeregning =
            listOf(
                SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true),
                SoeskenMedIBeregning(HELSOESKEN2_FOEDSELSNUMMER, true),
            ).somPeriodisertGrunnlag()

        val initialInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )
        val oppdatertInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                ),
            )
        val initialBeregningsMetode = BeregningsMetode.BEST.toGrunnlag()
        val oppdatertBeregningsMetode = BeregningsMetode.PRORATA.toGrunnlag()

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                initialInstitusjonsoppholdBeregningsgrunnlag,
                initialBeregningsMetode,
                listOf(
                    GrunnlagMedPeriode(
                        fom = LocalDate.of(2022, 8, 1),
                        tom = null,
                        data = BeregningsmetodeForAvdoed(AVDOED_FOEDSELSNUMMER.value, initialBeregningsMetode),
                    ),
                ),
                initialSoeskenMedIBeregning,
            ),
        )

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                oppdatertInstitusjonsoppholdBeregningsgrunnlag,
                oppdatertBeregningsMetode,
                listOf(
                    GrunnlagMedPeriode(
                        fom = LocalDate.of(2022, 8, 1),
                        tom = null,
                        data = BeregningsmetodeForAvdoed(AVDOED_FOEDSELSNUMMER.value, oppdatertBeregningsMetode),
                    ),
                ),
                oppdatertSoeskenMedIBeregning,
            ),
        )

        val result = repository.finnBeregningsGrunnlag(id)

        assertNotNull(result)

        assertEquals(oppdatertSoeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(oppdatertInstitusjonsoppholdBeregningsgrunnlag, result?.institusjonsopphold)
        assertEquals("Z654321", result?.kilde?.ident)
        assertEquals(oppdatertBeregningsMetode, result?.beregningsMetode)
    }

    @Test
    fun `Oppdatering fungerer for OMS`() {
        val id = UUID.randomUUID()

        val initialInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.NEI_KORT_OPPHOLD),
                ),
            )
        val oppdatertInstitusjonsoppholdBeregningsgrunnlag =
            listOf(
                GrunnlagMedPeriode(
                    fom = LocalDate.of(2022, 8, 1),
                    tom = null,
                    data = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                ),
            )
        val initialBeregningsMetode = BeregningsMetode.BEST.toGrunnlag()
        val oppdatertBeregningsMetode = BeregningsMetode.PRORATA.toGrunnlag()

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                initialInstitusjonsoppholdBeregningsgrunnlag,
                initialBeregningsMetode,
            ),
        )

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                oppdatertInstitusjonsoppholdBeregningsgrunnlag,
                oppdatertBeregningsMetode,
            ),
        )

        val result = repository.finnBeregningsGrunnlag(id)

        assertNotNull(result)

        assertEquals(oppdatertInstitusjonsoppholdBeregningsgrunnlag, result?.institusjonsopphold)
        assertEquals("Z654321", result?.kilde?.ident)
        assertEquals(oppdatertBeregningsMetode, result?.beregningsMetode)
    }

    @Test
    fun `skal haandtere at institusjonsopphold er null`() {
        val id = UUID.randomUUID()

        val oppdatertSoeskenMedIBeregning =
            listOf(
                SoeskenMedIBeregning(HELSOESKEN_FOEDSELSNUMMER, true),
                SoeskenMedIBeregning(HELSOESKEN2_FOEDSELSNUMMER, true),
            ).somPeriodisertGrunnlag()

        repository.lagreBeregningsGrunnlag(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                emptyList(),
                BeregningsMetode.BEST.toGrunnlag(),
                listOf(
                    GrunnlagMedPeriode(
                        fom = LocalDate.of(2022, 8, 1),
                        tom = null,
                        data = BeregningsmetodeForAvdoed(AVDOED_FOEDSELSNUMMER.value, BeregningsMetode.BEST.toGrunnlag()),
                    ),
                ),
                oppdatertSoeskenMedIBeregning,
            ),
        )

        val result = repository.finnBeregningsGrunnlag(id)

        assertNotNull(result)
    }

    @ParameterizedTest
    @MethodSource("overstyrtAarsaker")
    fun `skal kunne lagre og hente overstyr beregningsgrunnlag`(aarsak: String) {
        val behandlingId = UUID.randomUUID()

        repository.lagreOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12),
                    datoTOM = LocalDate.now().minusYears(6),
                    utbetaltBeloep = 123L,
                    foreldreloessats = false,
                    trygdetid = 35L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 1",
                    aarsak = aarsak,
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6).plusDays(1),
                    datoTOM = LocalDate.now(),
                    utbetaltBeloep = 321L,
                    foreldreloessats = false,
                    trygdetid = 5L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = 10,
                    prorataBroekNevner = 20,
                    sakId = sakId1,
                    beskrivelse = "test periode 2",
                    aarsak = aarsak,
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            ),
        )

        val data = repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId)

        data.size shouldBe 2
        data.first().behandlingId shouldBe behandlingId
        data.minBy { it.utbetaltBeloep }.let { grunnlag ->
            grunnlag.utbetaltBeloep shouldBe 123L
            grunnlag.prorataBroekNevner shouldBe null
            grunnlag.prorataBroekTeller shouldBe null
            grunnlag.aarsak shouldBe aarsak
        }
        data.maxBy { it.utbetaltBeloep }.let { grunnlag ->
            grunnlag.utbetaltBeloep shouldBe 321L
            grunnlag.prorataBroekTeller shouldBe 10
            grunnlag.prorataBroekNevner shouldBe 20
            grunnlag.aarsak shouldBe aarsak
        }
    }

    @Test
    fun `skal kunne lagre og hente overstyr beregningsgrunnlag som erstatter eksisterende`() {
        val behandlingId = UUID.randomUUID()

        repository.lagreOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12),
                    datoTOM = LocalDate.now().minusYears(6),
                    utbetaltBeloep = 123L,
                    foreldreloessats = false,
                    trygdetid = 35L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 1",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6).plusDays(1),
                    datoTOM = LocalDate.now(),
                    utbetaltBeloep = 321L,
                    foreldreloessats = false,
                    trygdetid = 5L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 2",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            ),
        )

        repository.lagreOverstyrBeregningGrunnlagForBehandling(
            behandlingId,
            listOf(
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(12),
                    datoTOM = LocalDate.now().minusYears(6),
                    utbetaltBeloep = 223L,
                    foreldreloessats = false,
                    trygdetid = 35L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 3",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
                OverstyrBeregningGrunnlagDao(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    datoFOM = LocalDate.now().minusYears(6).plusDays(1),
                    datoTOM = LocalDate.now(),
                    utbetaltBeloep = 322L,
                    foreldreloessats = false,
                    trygdetid = 5L,
                    trygdetidForIdent = null,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = sakId1,
                    beskrivelse = "test periode 4",
                    aarsak = "ANNET",
                    kilde =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "Z123456",
                            Tidspunkt.now(),
                        ),
                ),
            ),
        )

        val data = repository.finnOverstyrBeregningGrunnlagForBehandling(behandlingId)

        data.size shouldBe 2
        data.first().behandlingId shouldBe behandlingId
        data.minBy { it.utbetaltBeloep }.let {
            it.utbetaltBeloep shouldBe 223L
            it.trygdetid shouldBe 35L
            it.beskrivelse shouldBe "test periode 3"
        }
        data.maxBy { it.utbetaltBeloep }.let {
            it.utbetaltBeloep shouldBe 322L
            it.trygdetid shouldBe 5L
            it.beskrivelse shouldBe "test periode 4"
        }
    }

    private fun List<SoeskenMedIBeregning>.somPeriodisertGrunnlag(
        periodeFra: LocalDate = foerstePeriodeFra,
        periodeTil: LocalDate? = null,
    ): List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> =
        listOf(
            GrunnlagMedPeriode(
                fom = periodeFra,
                tom = periodeTil,
                data = this,
            ),
        )
}
