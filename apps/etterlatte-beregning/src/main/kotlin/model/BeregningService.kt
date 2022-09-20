package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import model.Grunnbeloep
import model.finnSoeskenperiodeStrategy.FinnSoeskenPeriodeStrategy
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.beregning.erInklusiv
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
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
                    grunnlagVersjon = grunnlag.versjon
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
                            grunnlagVersjon = grunnlag.versjon
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
                            grunnlagVersjon = grunnlag.versjon
                        )
                    }
                }
            }
            BehandlingType.MANUELT_OPPHOER -> {
                val datoFom = foersteVirkFraDoedsdato(grunnlag.grunnlag)
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
                    grunnlagVersjon = grunnlag.versjon
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
        val soeskenPerioder = FinnSoeskenPeriodeStrategy.create(grunnlag, virkFOM).soeskenperioder
        val alleFOM = (grunnbeloep.map { it.dato } + soeskenPerioder.map { it.datoFOM } + virkTOM).map {
            beregnFoersteFom(it, virkFOM)
        }.distinct().sorted().zipWithNext()
            .map { Pair(it.first, it.second.minusMonths(1)) }

        val beregningsperioder = alleFOM.mapIndexed { index, (fom, tom) ->
            val gjeldendeG = Grunnbeloep.hentGjeldendeG(fom)
            val flokkForPeriode = hentFlokkforPeriode(fom, tom, soeskenPerioder)
            val utbetaltBeloep = Soeskenjustering(flokkForPeriode.size, gjeldendeG.grunnbeløp).beloep
            val søkersFødselsdato =
                finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.SOEKER_PDL_V1)?.opplysning?.foedselsdato

            val datoTom = if (index == alleFOM.lastIndex && søkersFødselsdato != null) {
                beregnSisteTom(søkersFødselsdato, tom)
            } else {
                tom
            }

            (
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
                )
        }

        return beregningsperioder
    }

    fun hentFlokkforPeriode(
        datoFOM: YearMonth,
        datoTOM: YearMonth,
        soeskenPeriode: List<SoeskenPeriode>
    ): List<Person> = soeskenPeriode.firstOrNull { it.erInklusiv(datoFOM, datoTOM) }?.soeskenFlokk ?: emptyList()

    private fun beregnFoersteFom(fom: YearMonth, virkFOM: YearMonth): YearMonth =
        if (fom.isBefore(virkFOM)) virkFOM else fom

    private fun foersteVirkFraDoedsdato(grlag: List<Grunnlagsopplysning<JsonNode>>): YearMonth =
        finnOpplysning<Person>(
            grlag,
            Opplysningstyper.AVDOED_PDL_V1
        )?.opplysning?.doedsdato.let {
            YearMonth.from(it).plusMonths(1)
        }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: Grunnlagsopplysning<JsonNode>?): VilkaarOpplysning<T>? {
            return opplysning?.let {
                VilkaarOpplysning(
                    opplysning.id,
                    opplysning.opplysningType,
                    opplysning.kilde,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            }
        }

        inline fun <reified T> finnOpplysning(
            opplysninger: List<Grunnlagsopplysning<JsonNode>>,
            type: Opplysningstyper
        ): VilkaarOpplysning<T>? {
            return setOpplysningType(opplysninger.find { it.opplysningType == type })
        }
    }
}

fun beregnSisteTom(fødselsdato: LocalDate, tom: YearMonth): YearMonth? {
    val fyller18YearMonth = YearMonth.from(fødselsdato).plusYears(18)
    return if (fyller18YearMonth.isAfter(tom)) null else fyller18YearMonth
}