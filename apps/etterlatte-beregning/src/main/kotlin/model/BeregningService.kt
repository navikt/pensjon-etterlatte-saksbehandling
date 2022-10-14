package no.nav.etterlatte.model

import model.Grunnbeloep
import model.finnSoeskenperiode.FinnSoeskenPeriode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.beregning.erInklusiv
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

// TODO hvordan håndtere vedtakstidspunkt?
class BeregningService {
    fun beregnResultat(
        grunnlag: Grunnlag,
        virkFOM: YearMonth,
        virkTOM: YearMonth,
        vilkaarsvurdering: VilkaarResultat,
        behandlingType: BehandlingType
    ): BeregningsResultat {
        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                val beregningsperioder = finnBeregningsperioder(grunnlag, virkFOM, virkTOM)
                BeregningsResultat(
                    id = UUID.randomUUID(),
                    type = Beregningstyper.GP,
                    endringskode = Endringskode.NY,
                    resultat = BeregningsResultatType.BEREGNET,
                    beregningsperioder = beregningsperioder,
                    beregnetDato = LocalDateTime.now(),
                    grunnlagVersjon = grunnlag.hentVersjon()
                )
            }

            BehandlingType.REVURDERING -> {
                when (vilkaarsvurdering.resultat) {
                    VurderingsResultat.IKKE_OPPFYLT -> {
                        BeregningsResultat(
                            id = UUID.randomUUID(),
                            type = Beregningstyper.GP,
                            endringskode = Endringskode.REVURDERING,
                            resultat = BeregningsResultatType.BEREGNET,
                            beregningsperioder = listOf(
                                Beregningsperiode(
                                    delytelsesId = "BP",
                                    type = Beregningstyper.GP,
                                    datoFOM = virkFOM,
                                    datoTOM = null,
                                    utbetaltBeloep = 0,
                                    soeskenFlokk = listOf(),
                                    grunnbelopMnd = Grunnbeloep.hentGjeldendeG(virkFOM).grunnbeløpPerMåned,
                                    grunnbelop = Grunnbeloep.hentGjeldendeG(virkFOM).grunnbeløp
                                )
                            ),
                            beregnetDato = LocalDateTime.now(),
                            grunnlagVersjon = grunnlag.hentVersjon()
                        )
                    }

                    else -> {
                        val beregningsperioder = finnBeregningsperioder(grunnlag, virkFOM, virkTOM)
                        BeregningsResultat(
                            id = UUID.randomUUID(),
                            type = Beregningstyper.GP,
                            endringskode = Endringskode.REVURDERING,
                            resultat = BeregningsResultatType.BEREGNET,
                            beregningsperioder = beregningsperioder,
                            beregnetDato = LocalDateTime.now(),
                            grunnlagVersjon = grunnlag.hentVersjon()
                        )
                    }
                }
            }
            BehandlingType.MANUELT_OPPHOER -> {
                val datoFom = foersteVirkFraDoedsdato(grunnlag.hentAvdoed().hentDoedsdato()?.verdi!!)
                return BeregningsResultat(
                    id = UUID.randomUUID(),
                    type = Beregningstyper.GP,
                    endringskode = Endringskode.REVURDERING,
                    resultat = BeregningsResultatType.BEREGNET,
                    beregningsperioder = listOf(
                        Beregningsperiode(
                            delytelsesId = "BP",
                            type = Beregningstyper.GP,
                            datoFOM = datoFom,
                            datoTOM = null,
                            utbetaltBeloep = 0,
                            soeskenFlokk = listOf(),
                            grunnbelopMnd = Grunnbeloep.hentGjeldendeG(datoFom).grunnbeløpPerMåned,
                            grunnbelop = Grunnbeloep.hentGjeldendeG(datoFom).grunnbeløp
                        )
                    ),
                    beregnetDato = LocalDateTime.now(),
                    grunnlagVersjon = grunnlag.hentVersjon()
                )
            }
        }
    }

    private fun finnBeregningsperioder(
        grunnlag: Grunnlag,
        virkFOM: YearMonth,
        virkTOM: YearMonth
    ): List<Beregningsperiode> {
        val grunnbeloep = Grunnbeloep.hentGforPeriode(virkFOM)
        val soeskenPerioder = FinnSoeskenPeriode(grunnlag, virkFOM).hentSoeskenperioder()
        val alleFOM = (grunnbeloep.map { it.dato } + soeskenPerioder.map { it.datoFOM } + virkTOM).map {
            beregnFoersteFom(it, virkFOM)
        }.distinct().sorted().zipWithNext()
            .map { Pair(it.first, it.second.minusMonths(1)) }

        val beregningsperioder = alleFOM.mapIndexed { index, (fom, tom) ->
            val gjeldendeG = Grunnbeloep.hentGjeldendeG(fom)
            val flokkForPeriode = hentFlokkforPeriode(fom, tom, soeskenPerioder)
            val utbetaltBeloep = Soeskenjustering(flokkForPeriode.size, gjeldendeG.grunnbeløp).beloep
            val søkersFødselsdato = grunnlag.soeker.hentFoedselsdato()?.verdi

            val datoTom = if (index == alleFOM.lastIndex && søkersFødselsdato != null) {
                beregnSisteTom(søkersFødselsdato, tom)
            } else {
                tom
            }

            Beregningsperiode(
                delytelsesId = "BP",
                type = Beregningstyper.GP,
                datoFOM = fom,
                datoTOM = datoTom,
                grunnbelopMnd = gjeldendeG.grunnbeløpPerMåned,
                grunnbelop = gjeldendeG.grunnbeløp,
                soeskenFlokk = flokkForPeriode,
                utbetaltBeloep = utbetaltBeloep
            )
        }

        return beregningsperioder
    }

    private fun hentFlokkforPeriode(
        datoFOM: YearMonth,
        datoTOM: YearMonth,
        soeskenPeriode: List<SoeskenPeriode>
    ): List<Person> = soeskenPeriode.firstOrNull { it.erInklusiv(datoFOM, datoTOM) }?.soeskenFlokk ?: emptyList()

    private fun beregnFoersteFom(fom: YearMonth, virkFOM: YearMonth): YearMonth =
        if (fom.isBefore(virkFOM)) virkFOM else fom

    private fun foersteVirkFraDoedsdato(dødsdato: LocalDate): YearMonth = YearMonth.from(dødsdato).plusMonths(1)
}

fun beregnSisteTom(fødselsdato: LocalDate, tom: YearMonth): YearMonth? {
    val fyller18YearMonth = YearMonth.from(fødselsdato).plusYears(18)
    return if (fyller18YearMonth.isAfter(tom)) null else fyller18YearMonth
}