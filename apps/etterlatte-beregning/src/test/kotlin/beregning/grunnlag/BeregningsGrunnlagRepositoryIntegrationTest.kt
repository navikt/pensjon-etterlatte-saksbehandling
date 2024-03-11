package no.nav.etterlatte.beregning.grunnlag

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.beregning.regler.toGrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningsGrunnlagRepositoryIntegrationTest(dataSource: DataSource) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val repository = BeregningsGrunnlagRepository(dataSource)

    private val foerstePeriodeFra = LocalDate.of(2022, 8, 1)

    @AfterEach
    fun afterEach() {
        clearAllMocks()
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

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                soeskenMedIBeregning,
                institusjonsoppholdBeregningsgrunnlag,
                beregningsMetode,
            ),
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(soeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(institusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
        assertEquals(beregningsMetode, result?.beregningsMetode)
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

        repository.lagreOMS(
            BeregningsGrunnlagOMS(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                institusjonsoppholdBeregningsgrunnlag,
                beregningsMetode,
            ),
        )

        val result = repository.finnOmstillingstoenadGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(institusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
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

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                initialSoeskenMedIBeregning,
                initialInstitusjonsoppholdBeregningsgrunnlag,
                initialBeregningsMetode,
            ),
        )

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                oppdatertSoeskenMedIBeregning,
                oppdatertInstitusjonsoppholdBeregningsgrunnlag,
                oppdatertBeregningsMetode,
            ),
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(oppdatertSoeskenMedIBeregning, result?.soeskenMedIBeregning)
        assertEquals(oppdatertInstitusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
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

        repository.lagreOMS(
            BeregningsGrunnlagOMS(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z123456",
                    Tidspunkt.now(),
                ),
                initialInstitusjonsoppholdBeregningsgrunnlag,
                initialBeregningsMetode,
            ),
        )

        repository.lagreOMS(
            BeregningsGrunnlagOMS(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                oppdatertInstitusjonsoppholdBeregningsgrunnlag,
                oppdatertBeregningsMetode,
            ),
        )

        val result = repository.finnOmstillingstoenadGrunnlagForBehandling(id)

        assertNotNull(result)

        assertEquals(oppdatertInstitusjonsoppholdBeregningsgrunnlag, result?.institusjonsoppholdBeregningsgrunnlag)
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

        repository.lagre(
            BeregningsGrunnlag(
                id,
                Grunnlagsopplysning.Saksbehandler(
                    ident = "Z654321",
                    Tidspunkt.now(),
                ),
                oppdatertSoeskenMedIBeregning,
                emptyList(),
                BeregningsMetode.BEST.toGrunnlag(),
            ),
        )

        val result = repository.finnBarnepensjonGrunnlagForBehandling(id)

        assertNotNull(result)
    }

    @Test
    fun `skal kunne lagre og hente overstyr beregningsgrunnlag`() {
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
                    trygdetid = 35L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 1",
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
                    trygdetid = 5L,
                    prorataBroekTeller = 10,
                    prorataBroekNevner = 20,
                    sakId = 1L,
                    beskrivelse = "test periode 2",
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
        }
        data.maxBy { it.utbetaltBeloep }.let { grunnlag ->
            grunnlag.utbetaltBeloep shouldBe 321L
            grunnlag.prorataBroekTeller shouldBe 10
            grunnlag.prorataBroekNevner shouldBe 20
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
                    trygdetid = 35L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 1",
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
                    trygdetid = 5L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 2",
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
                    trygdetid = 35L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 3",
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
                    trygdetid = 5L,
                    prorataBroekTeller = null,
                    prorataBroekNevner = null,
                    sakId = 1L,
                    beskrivelse = "test periode 4",
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
    ): List<GrunnlagMedPeriode<List<SoeskenMedIBeregning>>> {
        return listOf(
            GrunnlagMedPeriode(
                fom = periodeFra,
                tom = periodeTil,
                data = this,
            ),
        )
    }
}
