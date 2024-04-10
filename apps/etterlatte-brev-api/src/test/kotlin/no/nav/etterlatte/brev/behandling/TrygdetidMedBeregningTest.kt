package no.nav.etterlatte.brev.behandling

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TrygdetidMedBeregningTest {
    @Test
    fun `henter trygdetid nasjonal beregning`() {
        with(
            Trygdetid(
                ident = "id",
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                maanederTrygdetid = 0,
                perioder =
                    listOf(
                        Trygdetidsperiode(
                            datoFOM = LocalDate.of(2024, 1, 1),
                            datoTOM = null,
                            land = "NOR",
                            opptjeningsperiode = null,
                            type = TrygdetidType.FREMTIDIG,
                            prorata = false,
                        ),
                        Trygdetidsperiode(
                            datoFOM = LocalDate.of(2000, 1, 1),
                            datoTOM = LocalDate.of(2023, 12, 31),
                            land = "NOR",
                            opptjeningsperiode = null,
                            type = TrygdetidType.FAKTISK,
                            prorata = false,
                        ),
                        Trygdetidsperiode(
                            datoFOM = LocalDate.of(1990, 1, 1),
                            datoTOM = LocalDate.of(1999, 12, 31),
                            land = "SWE",
                            opptjeningsperiode = null,
                            type = TrygdetidType.FAKTISK,
                            prorata = true,
                        ),
                    ),
                overstyrt = false,
                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
            ).toTrygdetidMedBeregningsmetode(BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL, "Ole"),
        ) {
            beregnetTrygdetidAar shouldBe 40
            prorataBroek shouldBe null
            trygdetidsperioder.size shouldBe 2
            trygdetidsperioder.forEach {
                it.land shouldBe "NOR"
            }
        }
    }

    @Test
    fun `henter trygdetid med prorata`() {
        with(
            Trygdetid(
                ident = "id",
                samletTrygdetidNorge = null,
                samletTrygdetidTeoretisk = 29,
                prorataBroek = null,
                maanederTrygdetid = 0,
                perioder =
                    listOf(
                        Trygdetidsperiode(
                            datoFOM = LocalDate.of(2020, 1, 1),
                            datoTOM = LocalDate.of(2024, 1, 1),
                            land = "NOR",
                            opptjeningsperiode = null,
                            type = TrygdetidType.FAKTISK,
                            prorata = false,
                        ),
                        Trygdetidsperiode(
                            datoFOM = LocalDate.of(1990, 1, 1),
                            datoTOM = LocalDate.of(2019, 12, 31),
                            land = "SWE",
                            opptjeningsperiode = null,
                            type = TrygdetidType.FAKTISK,
                            prorata = true,
                        ),
                    ),
                overstyrt = false,
                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
            ).toTrygdetidMedBeregningsmetode(BeregningsMetode.PRORATA, BeregningsMetode.BEST, "Ole"),
        ) {
            beregnetTrygdetidAar shouldBe 29
            prorataBroek shouldBe null
            trygdetidsperioder.size shouldBe 1
            trygdetidsperioder.forEach {
                it.prorata shouldBe true
            }
        }
    }

    @Test
    fun `trygdetid med beregning utleder navn på relevant avdød`() {
        val avdoede =
            listOf(
                Avdoed(
                    fnr = Foedselsnummer("idTilOle"),
                    navn = "Ole",
                    doedsdato = LocalDate.now(),
                ),
                Avdoed(
                    fnr = Foedselsnummer("idTilNasse"),
                    navn = "Nasse",
                    doedsdato = LocalDate.now(),
                ),
            )
        with(
            Trygdetid(
                ident = "idTilOle",
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                maanederTrygdetid = 0,
                perioder = emptyList(),
                overstyrt = false,
                mindreEnnFireFemtedelerAvOpptjeningstiden = false,
            ).toTrygdetidMedBeregningsmetode(BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL, avdoede),
        ) {
            navnAvdoed shouldBe "Ole"
        }
    }
}
