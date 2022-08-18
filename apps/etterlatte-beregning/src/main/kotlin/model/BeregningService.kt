package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import model.Grunnbeloep
import model.finnSoeskenperiodeStrategy.FinnSoeskenPeriodeStrategy
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

// TODO hvordan håndtere vedtakstidspunkt?

class BeregningService {
    // private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    fun beregnResultat(grunnlag: Grunnlag, virkFOM: YearMonth, virkTOM: YearMonth): BeregningsResultat {
        val beregningsperioder = finnBeregningsperioder(grunnlag, virkFOM, virkTOM)

        return BeregningsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = beregningsperioder,
            beregnetDato = LocalDateTime.now()
        )
    }

    private fun finnBeregningsperioder(
        grunnlag: Grunnlag,
        virkFOM: YearMonth,
        virkTOM: YearMonth
    ): List<Beregningsperiode> {
        val grunnbeloep = Grunnbeloep.hentGforPeriode(virkFOM)
        val soeskenPerioder = FinnSoeskenPeriodeStrategy.create(grunnlag, virkFOM).soeskenperioder
        val alleFOM = (grunnbeloep.map { it.dato } + soeskenPerioder.map { it.datoFOM } + virkTOM).map {
            beregnFoersteFom(
                it,
                virkFOM
            )
        }.distinct().sorted().zipWithNext()
            .map { Pair(it.first, it.second.minusMonths(1)) }

        return alleFOM.map {
            val gjeldendeG = Grunnbeloep.hentGjeldendeG(it.first)
            val flokkForPeriode = hentFlokkforPeriode(it.first, it.second, soeskenPerioder)

            (
                Beregningsperiode(
                    delytelsesId = "BP",
                    type = Beregningstyper.GP,
                    datoFOM = it.first,
                    datoTOM = it.second,
                    grunnbelopMnd = gjeldendeG.grunnbeløpPerMåned,
                    grunnbelop = gjeldendeG.grunnbeløp,
                    soeskenFlokk = flokkForPeriode,
                    utbetaltBeloep = beregnUtbetaling(
                        soeskenFlokk = flokkForPeriode.size,
                        g = gjeldendeG.grunnbeløpPerMåned
                    )
                )
                )
        }
    }

    // TODO finne bedre måte å gjøre dette på
    fun hentFlokkforPeriode(
        datoFOM: YearMonth,
        datoTOM: YearMonth,
        soeskenPeriode: List<SoeskenPeriode>
    ): List<Person> {
        val flokk = soeskenPeriode.filter { !it.datoTOM.isBefore(datoFOM) }
            .filter { it.datoTOM != datoFOM }
            .filter { !it.datoFOM.isAfter(datoTOM) }
            .filter { it.datoFOM != datoTOM }
        return if (flokk.isNotEmpty()) flokk[0].soeskenFlokk!! else emptyList()
    }

    // 40% av G til første barn, 25% til resten. Fordeles likt
    private fun beregnUtbetaling(soeskenFlokk: Int, g: Int): Double {
        if (soeskenFlokk == 0) return 0.0
        val antallSoesken = soeskenFlokk - 1

        return (g * 0.40 + (g * 0.25 * antallSoesken)) / (soeskenFlokk)
    }
    private fun beregnFoersteFom(first: YearMonth, virkFOM: YearMonth): YearMonth {
        return if (first.isBefore(virkFOM)) {
            virkFOM
        } else {
            first
        }
    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: Grunnlagsopplysning<ObjectNode>?): VilkaarOpplysning<T>? {
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
            opplysninger: List<Grunnlagsopplysning<ObjectNode>>,
            type: Opplysningstyper
        ): VilkaarOpplysning<T>? {
            return setOpplysningType(opplysninger.find { it.opplysningType == type })
        }
    }
}