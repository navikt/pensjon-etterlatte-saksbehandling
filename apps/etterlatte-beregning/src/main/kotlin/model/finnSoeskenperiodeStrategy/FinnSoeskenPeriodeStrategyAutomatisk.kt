package model.finnSoeskenperiodeStrategy

import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.person.Person
import java.time.YearMonth

// Adressesjekk på halvsøsken på dødsfallstidspunkt i første omgang
data class FinnSoeskenPeriodeStrategyAutomatisk(val avdoedesBarn: List<Person>?, val bruker: Person?, private val virkFOM: YearMonth) : FinnSoeskenPeriodeStrategy() {
    override val soeskenperioder: List<SoeskenPeriode>
        get() = finnSoeskenperioder(avdoedesBarn, bruker, virkFOM)

    private fun finnSoeskenperioder(avdoedesBarn: List<Person>?, bruker: Person?, virkFOM: YearMonth): List<SoeskenPeriode> {
        //List compare?
        val helsoesken = avdoedesBarn?.filter { it.familieRelasjon?.foreldre == bruker?.familieRelasjon?.foreldre }
        val halvsoesken = avdoedesBarn?.filter { avdoedbarn ->
            avdoedbarn.foedselsnummer !in (helsoesken?.map { helsoesken -> helsoesken.foedselsnummer } ?: emptyList())
        }
        //first skal være ok, siden PPS allerede har sortert
        val halvsoeskenOppdrattSammen =
            halvsoesken?.filter { it.bostedsadresse?.first() == bruker?.bostedsadresse?.first() }
        val kull: MutableList<Person> = ArrayList()
        helsoesken?.let { kull.addAll(it) }
        halvsoeskenOppdrattSammen?.let { kull.addAll(it) }

        val perioder = beregnSoeskenperioder(kull, virkFOM)
        //TODO håndtere doedsfall
        return perioder.map {
            SoeskenPeriode(
                it.first,
                it.second,
                beregnGyldigSoeskenForPeriode(kull, it.first)
            )
        }
    }

    private fun beregnSoeskenperioder(soesken: List<Person>, virkFOM: YearMonth): List<Pair<YearMonth, YearMonth>> {
        return soesken.map { YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month) }
            .plus(soesken.map { YearMonth.of(it.foedselsdato!!.year + 18, it.foedselsdato!!.month) })
            .plus(virkFOM)
            .plus(YearMonth.now().plusMonths(3))
            .filter { !it.isBefore(virkFOM) }
            .filter { !it.isAfter(YearMonth.now().plusMonths(3)) }
            .sorted()
            .zipWithNext()
    }

    private fun beregnGyldigSoeskenForPeriode(soesken: List<Person>, fra: YearMonth): List<Person> {
        return soesken.filter { it.foedselsdato != null }
            .map { Pair(YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month), it) }
            .filter { !it.first.isAfter(fra) }
            .filter { ((fra.year - it.first.year) * 12 + (fra.month.value - it.first.month.value)) / 12 < 18 }
            .map { it.second }
    }

}
