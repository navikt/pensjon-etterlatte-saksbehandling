package no.nav.etterlatte.model.finnSoeskenperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.beregning.SoeskenPeriode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentFamilierelasjon
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoeskenjustering
import no.nav.etterlatte.libs.common.person.Person
import java.time.YearMonth

// Adressesjekk på halvsøsken på dødsfallstidspunkt i første omgang
data class FinnSoeskenPeriode(
    private val grunnlag: Grunnlag,
    private val virkFOM: YearMonth
) {
    fun hentSoeskenperioder(): List<SoeskenPeriode> {
        val avdoedesBarn = grunnlag.hentAvdoed().hentAvdoedesbarn()?.verdi?.avdoedesBarn ?: return emptyList()
        val soeker = grunnlag.soeker
        val soekersBostedsadresse = soeker.hentBostedsadresse()?.hentSenest()?.verdi ?: return emptyList()

        val skalBeregnesOverrides = grunnlag.sak.hentSoeskenjustering()?.verdi?.beregningsgrunnlag
            ?.associateBy({ it.foedselsnummer.value }, { it.skalBrukes })
            ?: emptyMap()

        val (soeskenOverrides, soeskenDefault) = avdoedesBarn.partition {
            skalBeregnesOverrides.containsKey(it.foedselsnummer.value)
        }
        val soesken = finnHelOgHalvsoesken(soeker, soeskenDefault)

        // first skal være ok, siden PPS allerede har sortert
        val halvsoeskenOppdrattSammen =
            soesken.halvsoesken.filter { it.bostedsadresse?.first() == soekersBostedsadresse }

        val kull = soeskenOverrides.filter { skalBeregnesOverrides[it.foedselsnummer.value] == true } +
            soesken.helsoesken +
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

data class Soesken(
    val helsoesken: List<Person>,
    val halvsoesken: List<Person>
)

fun finnHelOgHalvsoesken(soeker: Grunnlagsdata<JsonNode>, avdoedesBarn: List<Person>): Soesken {
    val soekersForeldre = soeker.hentFamilierelasjon()?.verdi?.ansvarligeForeldre
        ?: return Soesken(emptyList(), emptyList())
    val (helsoesken, halvsoesken) = avdoedesBarn
        .filter { it.foedselsnummer != soeker.hentFoedselsnummer()?.verdi }
        .partition { it.familieRelasjon?.foreldre == soekersForeldre }
    return Soesken(helsoesken = helsoesken, halvsoesken = halvsoesken)
}