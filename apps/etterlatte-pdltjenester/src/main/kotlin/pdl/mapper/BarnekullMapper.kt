package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient

data class Barnekull(
    val barn: List<Person>,
    val barnUtenIdent: List<PersonUtenIdent>? = null,
)

object BarnekullMapper {
    suspend fun mapBarnekull(
        pdlKlient: PdlKlient,
        ppsKlient: ParallelleSannheterKlient,
        forelder: PdlHentPerson,
        saktype: SakType,
    ): Barnekull? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonsIdent != null
                }
                ?.map { it.relatertPersonsIdent }
                ?.distinct()
                ?.map { Folkeregisteridentifikator.of(it) }

        val personerUtenIdent =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonUtenFolkeregisteridentifikator != null
                }
                ?.map {
                    PersonUtenIdent(
                        RelativPersonrolle.BARN,
                        it.relatertPersonUtenFolkeregisteridentifikator!!.tilRelatertPerson(),
                    )
                }

        val personer =
            barnFnr?.let { fnr ->
                pdlKlient.hentPersonBolk(fnr, saktype).data?.hentPersonBolk?.map {
                    PersonMapper.mapPerson(
                        ppsKlient,
                        pdlKlient,
                        Folkeregisteridentifikator.of(it.ident),
                        PersonRolle.TILKNYTTET_BARN,
                        it.person!!,
                        saktype,
                    )
                }
            }

        return personer?.let { Barnekull(it, personerUtenIdent) }
    }
}
