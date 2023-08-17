package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.pdl.ForeldreansvarPeriode
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHistorikkForeldreansvar

object ForeldreansvarHistorikkMapper {

    fun mapForeldreAnsvar(pdlData: PdlHistorikkForeldreansvar): HistorikkForeldreansvar {
        if (pdlData.foreldreansvar.any { it.ansvarligUtenIdentifikator != null }) {
            throw FamilieRelasjonManglerIdent(
                "Har en ansvarlig forelder som mangler ident i historikken for foreldreansvar."
            )
        }

        val foreldreansvar = pdlData.foreldreansvar.filter {
            it.ansvarlig != null
        }
            .map {
                val fraDato = it.folkeregistermetadata?.gyldighetstidspunkt
                val tilDato = it.folkeregistermetadata?.opphoerstidspunkt
                ForeldreansvarPeriode(
                    fraDato = fraDato?.toLocalDate(),
                    tilDato = tilDato?.toLocalDate(),
                    forelder = Folkeregisteridentifikator.of(it.ansvarlig)
                )
            }
        val foreldre = pdlData.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
            .groupBy { it.relatertPersonsIdent }
            .mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
            .map {
                it.value?.relatertPersonsIdent?.let { Folkeregisteridentifikator.of(it) }
                    ?: throw FamilieRelasjonManglerIdent("${it.value} mangler ident")
            }
        return HistorikkForeldreansvar(
            ansvarligeForeldre = foreldreansvar,
            foreldre = foreldre
        )
    }
}