package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.pdl.ForeldreansvarPeriode
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHistorikkForeldreansvar

object ForeldreansvarHistorikkMapper {
    fun mapForeldreAnsvar(pdlData: PdlHistorikkForeldreansvar): HistorikkForeldreansvar {
        val foreldreansvar =
            pdlData.foreldreansvar
                .filter {
                    it.ansvarlig != null
                }.map {
                    val fraDato = it.folkeregistermetadata?.gyldighetstidspunkt
                    val tilDato = it.folkeregistermetadata?.opphoerstidspunkt
                    ForeldreansvarPeriode(
                        fraDato = fraDato?.toLocalDate(),
                        tilDato = tilDato?.toLocalDate(),
                        forelder = Folkeregisteridentifikator.of(it.ansvarlig),
                    )
                }
        val foreldre =
            pdlData.forelderBarnRelasjon
                .filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
                .groupBy { it.relatertPersonsIdent }
                .mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                .map { it.value }
                .mapNotNull {
                    it?.relatertPersonsIdent?.let { ident ->
                        Folkeregisteridentifikator.of(ident)
                    }
                }
        return HistorikkForeldreansvar(
            ansvarligeForeldre = foreldreansvar,
            foreldre = foreldre,
        )
    }
}
