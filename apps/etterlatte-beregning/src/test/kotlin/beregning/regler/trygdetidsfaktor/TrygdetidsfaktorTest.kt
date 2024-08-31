package no.nav.etterlatte.beregning.regler.trygdetidsfaktor

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.beregning.regler.REGEL_PERIODE
import no.nav.etterlatte.beregning.regler.barnepensjon.BarnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.TrygdetidGrunnlag
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.anvendtTrygdetidRegel
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.maksTrygdetid
import no.nav.etterlatte.beregning.regler.barnepensjon.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.beregning.regler.barnepensjonGrunnlag
import no.nav.etterlatte.beregning.regler.samletTrygdetid
import no.nav.etterlatte.beregning.regler.toBeregningstall
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class TrygdetidsfaktorTest {
    val trygdetidIdent = "22511075258"

    private val trygdetid =
        samletTrygdetid(
            BeregningsMetode.NASJONAL,
            samletTrygdetidNorge = Beregningstall(40.0),
            samletTrygdetidTeoretisk = Beregningstall(30.0),
            broek = IntBroek(1, 2),
            ident = trygdetidIdent,
        )

    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid for nasjonal`() {
        val resultat = anvendtTrygdetidRegel.anvend(TrygdetidGrunnlag(trygdetid), REGEL_PERIODE)

        resultat.verdi shouldBe AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40.0), trygdetidIdent)
    }

    @Test
    fun `trygdetidRegel skal returnere 15 aars trygdetid for prorata`() {
        val resultat =
            anvendtTrygdetidRegel.anvend(
                TrygdetidGrunnlag(trygdetid.copy(verdi = trygdetid.verdi.copy(beregningsMetode = BeregningsMetode.PRORATA))),
                REGEL_PERIODE,
            )

        resultat.verdi.trygdetid.setScale(1) shouldBe Beregningstall(15.0).setScale(1)
    }

    @Test
    fun `trygdetidRegel skal returnere 40 aars trygdetid for best`() {
        val resultat =
            anvendtTrygdetidRegel.anvend(
                TrygdetidGrunnlag(trygdetid.copy(verdi = trygdetid.verdi.copy(beregningsMetode = BeregningsMetode.BEST))),
                REGEL_PERIODE,
            )

        resultat.verdi shouldBe AnvendtTrygdetid(BeregningsMetode.NASJONAL, Beregningstall(40.0), trygdetidIdent)
    }

    @Test
    fun `maksTrygdetid skal returnere 40 aars trygdetid`() {
        val resultat = maksTrygdetid.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe Beregningstall(40)
    }

    @Test
    fun `trygdetidsFaktor skal returnere 1 naar trygdetid er 40 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(), REGEL_PERIODE)

        resultat.verdi shouldBe 1.toBeregningstall()
    }

    @Test
    fun `trygdetidsFaktor skal returnere 0,5 naar trygdetid er 20 aar`() {
        val resultat = trygdetidsFaktor.anvend(barnepensjonGrunnlag(trygdeTid = Beregningstall(20.0)), REGEL_PERIODE)

        resultat.verdi shouldBe 0.5.toBeregningstall()
    }

    @ParameterizedTest(
        name = "{0}",
    )
    @MethodSource("flereAvdoedeGrunnlag")
    fun `trygdetidsFaktor med flere avdoede`(
        beskrivelse: String,
        grunnlag: BarnepensjonGrunnlag,
        forventetFaktor: Double,
    ) {
        val resultat = trygdetidsFaktor.anvend(grunnlag, REGEL_PERIODE)

        resultat.verdi shouldBe forventetFaktor.toBeregningstall()
    }

    companion object {
        @JvmStatic
        fun grunnlagForToAvdoede(
            beregningsMetodeAvdoed1: BeregningsMetode,
            trygdetid1: Double,
            beregningsMetodeAvdoed2: BeregningsMetode,
            trygdetid2: Double,
        ) = BarnepensjonGrunnlag(
            soeskenKull = FaktumNode(emptyList(), kilde, "søskenkull"),
            avdoedesTrygdetid =
                FaktumNode(
                    listOf(
                        AnvendtTrygdetid(
                            beregningsMetodeAvdoed1,
                            Beregningstall(trygdetid1),
                            AVDOED_FOEDSELSNUMMER.toString(),
                        ),
                        AnvendtTrygdetid(
                            beregningsMetodeAvdoed2,
                            Beregningstall(trygdetid2),
                            AVDOED2_FOEDSELSNUMMER.toString(),
                        ),
                    ),
                    kilde,
                    "trygdetid",
                ),
            institusjonsopphold = FaktumNode(null, kilde, "institusjonsopphold"),
            kunEnJuridiskForelder = FaktumNode(false, kilde, "kunEnJuridiskForelder"),
        )

        @JvmStatic
        fun flereAvdoedeGrunnlag() =
            listOf(
                Arguments.of(
                    "Begge har NASJONAL",
                    grunnlagForToAvdoede(
                        BeregningsMetode.NASJONAL,
                        40.0,
                        BeregningsMetode.NASJONAL,
                        40.0,
                    ),
                    1.0,
                ),
                Arguments.of(
                    "En med NASJONAL, en med 50% PRORATA",
                    grunnlagForToAvdoede(
                        BeregningsMetode.NASJONAL,
                        40.0,
                        BeregningsMetode.PRORATA,
                        20.0,
                    ),
                    1.0,
                ),
                Arguments.of(
                    "En med lav verdi NASJONAL, en med 50% PRORATA",
                    grunnlagForToAvdoede(
                        BeregningsMetode.NASJONAL,
                        10.0,
                        BeregningsMetode.PRORATA,
                        20.0,
                    ),
                    0.5,
                ),
            )
    }
}
