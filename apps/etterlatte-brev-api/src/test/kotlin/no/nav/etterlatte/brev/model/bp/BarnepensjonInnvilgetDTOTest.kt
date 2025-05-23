package no.nav.etterlatte.brev.model.bp

import io.mockk.mockk
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.kodeverk.BeskrivelseDto
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

internal class BarnepensjonInnvilgetDTOTest {
    private val grunnbeloep = 118_620
    private val grunnbeloepPerMaaned = 9885

    @Test
    fun `setter skille for etterbetaling`() {
        val barnepensjonInnvilgelse =
            BarnepensjonInnvilgelse.fra(
                utbetalingsinfo =
                    Utbetalingsinfo(
                        false,
                        antallBarn = 1,
                        beloep = Kroner(1234),
                        virkningsdato = LocalDate.of(2023, Month.JANUARY, 1),
                        soeskenjustering = false,
                        beregningsperioder =
                            listOf(
                                beregningsperiodeJanuar2022(),
                                beregningsperiodeFebruar2022(),
                                beregningsperiodeMarsApril2022(),
                                beregningsperiodeAprilDesember2022(),
                                beregningsperiodeHele2023(),
                                beregningsperiode2024OgUtover(),
                            ),
                    ),
                etterbetaling =
                    EtterbetalingDTO(
                        datoFom = LocalDate.of(2022, Month.JANUARY, 1),
                        datoTom = LocalDate.of(2022, Month.MARCH, 31),
                    ),
                trygdetid =
                    listOf(
                        TrygdetidDto(
                            id = UUID.randomUUID(),
                            ident = "123",
                            behandlingId = UUID.randomUUID(),
                            beregnetTrygdetid =
                                DetaljertBeregnetTrygdetidDto(
                                    resultat =
                                        DetaljertBeregnetTrygdetidResultat(
                                            samletTrygdetidNorge = 40,
                                            samletTrygdetidTeoretisk = null,
                                            faktiskTrygdetidNorge = null,
                                            fremtidigTrygdetidNorge = null,
                                            faktiskTrygdetidTeoretisk = null,
                                            fremtidigTrygdetidTeoretisk = null,
                                            beregnetSamletTrygdetidNorge = null,
                                            prorataBroek = null,
                                            overstyrt = false,
                                            yrkesskade = false,
                                        ),
                                    tidspunkt = Tidspunkt.now(),
                                ),
                            trygdetidGrunnlag = emptyList(),
                            overstyrtNorskPoengaar = null,
                            opplysningerDifferanse = mockk(),
                            opplysninger = mockk(),
                            begrunnelse = null,
                        ),
                    ),
                grunnbeloep =
                    Grunnbeloep(
                        dato = YearMonth.of(2023, Month.JANUARY),
                        grunnbeloep = grunnbeloep,
                        grunnbeloepPerMaaned = grunnbeloepPerMaaned,
                        omregningsfaktor = null,
                    ),
                innhold = lagInnholdMedVedlegg(),
                utlandstilknytning = null,
                brevutfall =
                    BrevutfallDto(
                        UUID.randomUUID(),
                        Aldersgruppe.UNDER_18,
                        Feilutbetaling(
                            FeilutbetalingValg.NEI,
                            null,
                        ),
                        frivilligSkattetrekk = true,
                        null,
                    ),
                erGjenoppretting = false,
                avdoede =
                    listOf(
                        Avdoed(
                            fnr = Foedselsnummer("123"),
                            navn = "HubbaBubba",
                            doedsdato = LocalDate.now(),
                        ),
                    ),
                erMigrertYrkesskade = false,
                erSluttbehandling = false,
                landKodeverk = landKodeverk(),
                klage = null,
            )

        Assertions.assertEquals(
            listOf(
                beregningsperiodeJanuar2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeFebruar2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeMarsApril2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeAprilDesember2022().toBarnepensjonBeregningsperiode(),
                beregningsperiodeHele2023().toBarnepensjonBeregningsperiode(),
                beregningsperiode2024OgUtover().toBarnepensjonBeregningsperiode(),
            ),
            barnepensjonInnvilgelse.beregning.beregningsperioder,
        )
    }

    private fun beregningsperiodeJanuar2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.JANUARY, 1),
            LocalDate.of(2022, Month.JANUARY, 31),
        )

    private fun beregningsperiodeFebruar2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.FEBRUARY, 1),
            LocalDate.of(2022, Month.FEBRUARY, 28),
        )

    private fun beregningsperiodeMarsApril2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.MARCH, 1),
            LocalDate.of(2022, Month.APRIL, 30),
        )

    private fun beregningsperiodeAprilDesember2022() =
        beregningsperiode(
            LocalDate.of(2022, Month.APRIL, 1),
            LocalDate.of(2022, Month.DECEMBER, 31),
        )

    private fun beregningsperiodeHele2023() =
        beregningsperiode(
            datoFOM = LocalDate.of(2023, Month.JANUARY, 1),
            datoTOM = LocalDate.of(2023, Month.DECEMBER, 31),
        )

    private fun beregningsperiode2024OgUtover() =
        beregningsperiode(
            datoFOM = LocalDate.of(2024, Month.JANUARY, 1),
            datoTOM = null,
        )

    private fun beregningsperiode(
        datoFOM: LocalDate,
        datoTOM: LocalDate?,
    ) = Beregningsperiode(
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        grunnbeloep = Kroner(grunnbeloep),
        antallBarn = 1,
        utbetaltBeloep = Kroner(grunnbeloepPerMaaned),
        trygdetid = 10,
        prorataBroek = null,
        institusjon = false,
        beregningsMetodeAnvendt = BeregningsMetode.NASJONAL,
        beregningsMetodeFraGrunnlag = BeregningsMetode.BEST,
        trygdetidForIdent = "123",
        avdoedeForeldre = if (datoFOM < BarnepensjonInnvilgelse.tidspunktNyttRegelverk) null else listOf("123"),
        harForeldreloessats = false,
    )

    private fun lagInnholdMedVedlegg() =
        InnholdMedVedlegg({ emptyList() }, {
            listOf(
                BrevInnholdVedlegg(
                    tittel = "Vedlegget",
                    key = BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                    payload = Slate(),
                ),
            )
        })

    private fun Beregningsperiode.toBarnepensjonBeregningsperiode() =
        BarnepensjonBeregningsperiode.fra(
            this,
            false,
        )

    private fun landKodeverk() =
        listOf(
            LandDto("SWE", "2020-01-01", "2999-01-01", BeskrivelseDto("SVERIGE", "Sverige")),
            LandDto("NOR", "2020-01-01", "2999-01-01", BeskrivelseDto("NORGE", "Norge")),
        )
}
