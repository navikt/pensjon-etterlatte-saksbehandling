package model.finnSoeskenperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSøskenjustering
import no.nav.etterlatte.libs.common.person.Person
import java.time.YearMonth

// Adressesjekk på halvsøsken på dødsfallstidspunkt i første omgang
data class FinnSoeskenPeriode(
    private val grunnlag: Opplysningsgrunnlag,
    private val virkFOM: YearMonth
) {
    fun hentSoeskenperioder(): List<SoeskenPeriode> {
        val avdoedesBarn = grunnlag.hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn ?: return emptyList()
        val søker = grunnlag.søker
        /* TODO ai: Periodisering */
        val søkersBostedsadresse = søker.hentBostedsadresse()?.perioder?.lastOrNull()?.verdi ?: return emptyList()

        val skalBeregnesOverrides = grunnlag.sak.hentSøskenjustering()?.verdi?.beregningsgrunnlag
            ?.associateBy({ it.foedselsnummer.value }, { it.skalBrukes })
            ?: emptyMap()

        val (søskenOverrides, søskenDefault) = avdoedesBarn.partition {
            skalBeregnesOverrides.containsKey(it.foedselsnummer.value)
        }
        val søsken = finnHelOgHalvsøsken(søker, søskenDefault)

        // first skal være ok, siden PPS allerede har sortert
        val halvsoeskenOppdrattSammen =
            søsken.halvsøsken.filter { it.bostedsadresse?.first() == søkersBostedsadresse } // TODO ai: periodisering

        val kull = søskenOverrides.filter { skalBeregnesOverrides[it.foedselsnummer.value] == true } +
            søsken.helsøsken +
            halvsoeskenOppdrattSammen

        val perioder = beregnSoeskenperioder(kull, virkFOM)
        // TODO håndtere doedsfall
        return perioder.map { SoeskenPeriode(it.first, it.second, beregnGyldigSoeskenForPeriode(kull, it.first)) }
    }

    private fun beregnSoeskenperioder(soesken: List<Person>, virkFOM: YearMonth): List<Pair<YearMonth, YearMonth>> =
        soesken.asSequence().map { YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month) }
            .plus(soesken.map { YearMonth.of(it.foedselsdato!!.year + 18, it.foedselsdato!!.month) })
            .plus(virkFOM)
            .plus(YearMonth.now().plusMonths(3))
            .filter { !it.isBefore(virkFOM) }
            .filter { !it.isAfter(YearMonth.now().plusMonths(3)) }
            .sorted()
            .zipWithNext()
            .mapIndexed { index, (fom, tom) -> if (index > 0) Pair(fom.plusMonths(1), tom) else Pair(fom, tom) }
            .toList()

    private fun beregnGyldigSoeskenForPeriode(soesken: List<Person>, fra: YearMonth): List<Person> {
        return soesken.asSequence().filter { it.foedselsdato != null }
            .map { Pair(YearMonth.of(it.foedselsdato!!.year, it.foedselsdato!!.month), it) }
            .filter { !it.first.isAfter(fra) }
            .filter { ((fra.year - it.first.year) * 12 + (fra.month.value - it.first.month.value)) / 12 < 18 }
            .map { it.second }.toList()
    }
}

data class Søsken(
    val helsøsken: List<Person>,
    val halvsøsken: List<Person>
)

fun finnHelOgHalvsøsken(søker: Grunnlagsdata<JsonNode>, avdoedesBarn: List<Person>): Søsken {
    val søkersForeldre = søker.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
        ?: return Søsken(emptyList(), emptyList())
    val (helsøsken, halvsøsken) = avdoedesBarn
        .filter { it.foedselsnummer != søker.hentFoedselsnummer()?.verdi }
        .partition { it.familieRelasjon?.foreldre == søkersForeldre }
    return Søsken(helsøsken = helsøsken, halvsøsken = halvsøsken)
}