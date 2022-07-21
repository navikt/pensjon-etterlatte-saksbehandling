package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import model.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.*
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

//TODO hvordan håndtere vedtakstidspunkt?

class BeregningService {
    //private val logger = LoggerFactory.getLogger(BeregningService::class.java)


    fun beregnResultat(grunnlag: Grunnlag, virkFOM: YearMonth, virkTOM: YearMonth): BeregningsResultat {

        val beregningsperioder = finnBeregningsperioder(grunnlag,virkFOM, virkTOM)


        return BeregningsResultat(
            id = UUID.randomUUID(),
            type = Beregningstyper.GP,
            endringskode = Endringskode.NY,
            resultat = BeregningsResultatType.BEREGNET,
            beregningsperioder = beregningsperioder,
            beregnetDato = LocalDateTime.now()
        )
    }

    private fun finnBeregningsperioder(grunnlag: Grunnlag, virkFOM: YearMonth, virkTOM: YearMonth): List<Beregningsperiode> {
        val grunnbeloep = Grunnbeloep.hentGforPeriode(virkFOM)
        val soeskenPeriode = finnSoeskenperioder(grunnlag, virkFOM)
        val alleFOM = (grunnbeloep.map {it.dato} + soeskenPeriode.map { it.datoFOM } + virkTOM).map { beregnFoersteFom(it, virkFOM) }.distinct().sorted().zipWithNext()
            .map{ Pair(it.first, it.second.minusMonths(1))}

        return alleFOM.map {
            val gjeldendeG = Grunnbeloep.hentGjeldendeG(it.first)
            val flokkForPeriode = hentFlokkforPeriode(it.first,it.second,soeskenPeriode )
            (Beregningsperiode(
                delytelsesId = "BP",
                type = Beregningstyper.GP,
                datoFOM = it.first,
                datoTOM = it.second,
                grunnbelopMnd = gjeldendeG.grunnbeløpPerMåned,
                grunnbelop = gjeldendeG.grunnbeløp,
                soeskenFlokk = flokkForPeriode,
                utbetaltBeloep = beregnUtbetaling(flokkForPeriode.size, gjeldendeG.grunnbeløpPerMåned)

            ))
        }
    }

    // Adressesjekk på halvsøsken på dødsfallstidspunkt i første omgang
    private fun finnSoeskenperioder(grunnlag: Grunnlag, virkFOM: YearMonth): List<SoeskenPeriode> {

        val avdoedPdl = finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.AVDOED_PDL_V1)?.opplysning
        val bruker = finnOpplysning<Person>(grunnlag.grunnlag, Opplysningstyper.SOEKER_PDL_V1)?.opplysning
        //List compare?
        val helsoesken = avdoedPdl?.avdoedesBarn?.filter { it.familieRelasjon?.foreldre == bruker?.familieRelasjon?.foreldre }
        val halvsoesken = avdoedPdl?.avdoedesBarn?.filter { avdoedbarn ->
            avdoedbarn.foedselsnummer !in (helsoesken?.map { helsoesken -> helsoesken.foedselsnummer } ?: emptyList())
        }
        //first skal være ok, siden PPS allerede har sortert
        val halvsoeskenOppdrattSammen = halvsoesken?.filter { it.bostedsadresse?.first() == bruker?.bostedsadresse?.first() }
        val kull: MutableList<Person> = ArrayList()
        helsoesken?.let { kull.addAll(it) }
        halvsoeskenOppdrattSammen?.let { kull.addAll(it) }

        val perioder = beregnSoeskenperioder(kull,virkFOM)
        //TODO håndtere doedsfall
        return perioder.map {
            SoeskenPeriode(
                it.first,
                it.second,
                beregnGyldigSoeskenForPeriode(kull,it.first))
        }
    }

    private fun beregnGyldigSoeskenForPeriode (soesken: List<Person>, fra: YearMonth): List<Person> {
        return soesken.filter {it.foedselsdato != null }
            .map { Pair(YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month ), it)}
            .filter { !it.first.isAfter(fra) }
            .filter{((fra.year - it.first.year) * 12 + (fra.month.value - it.first.month.value))/12 < 18}
            .map { it.second }
    }


    private fun beregnSoeskenperioder (soesken: List<Person>, virkFOM: YearMonth):  List<Pair<YearMonth, YearMonth>> {
        return soesken.map { YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month) }
            .plus(soesken.map { YearMonth.of(it.foedselsdato!!.year+18, it.foedselsdato!!.month) })
            .plus(virkFOM)
            .plus(YearMonth.now().plusMonths(3))
            .filter { !it.isBefore(virkFOM) }
            .filter { !it.isAfter(YearMonth.now().plusMonths(3)) }
            .sorted()
            .zipWithNext()
    }


    //TODO finne bedre måte å gjøre dette på
    fun hentFlokkforPeriode(datoFOM: YearMonth, datoTOM: YearMonth, soeskenPeriode: List<SoeskenPeriode>): List<Person> {
        val flokk = soeskenPeriode.filter { !it.datoTOM.isBefore(datoFOM)}
            .filter{it.datoTOM != datoFOM}
            .filter{!it.datoFOM.isAfter(datoTOM) }
            .filter{it.datoFOM != datoTOM}
        return if (flokk.isNotEmpty()) flokk[0].soeskenFlokk!! else emptyList()

    }
    // 40% av G til første barn, 25% til resten. Fordeles likt
    fun beregnUtbetaling(flokkStoerrelse: Int, g: Int): Double {
        return if (flokkStoerrelse == 0) g * 0.40
        else (g * 0.40 + ( g *0.25)*flokkStoerrelse) / (flokkStoerrelse +1)
    }
    private fun beregnFoersteFom (first: YearMonth, virkFOM: YearMonth ): YearMonth {
        return if(first.isBefore(virkFOM)) {
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

