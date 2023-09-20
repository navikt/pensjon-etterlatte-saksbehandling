package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient

object BarnekullMapper {
    suspend fun mapBarnekull(
        pdlKlient: PdlKlient,
        ppsKlient: ParallelleSannheterKlient,
        forelder: PdlHentPerson,
        saktype: SakType,
    ): List<Person>? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    (Folkeregisteridentifikator.of(it.value?.relatertPersonsIdent))
                }

        return barnFnr?.let { fnr ->
            pdlKlient.hentPersonBolk(fnr, saktype).data?.hentPersonBolk?.map {
                PersonMapper.mapPerson(
                    ppsKlient,
                    pdlKlient,
                    Folkeregisteridentifikator.of(it.ident),
                    PersonRolle.BARN,
                    it.person!!,
                    saktype,
                )
            }
        }
    }
}
