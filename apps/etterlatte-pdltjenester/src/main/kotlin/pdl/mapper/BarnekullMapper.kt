package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.personweb.familieOpplysninger.Familiemedlem

data class Barnekull(
    val barn: List<Person>,
    val barnUtenIdent: List<PersonUtenIdent>? = null,
)

data class BarnekullPersonopplysning(
    val barn: List<Familiemedlem?>,
)

object BarnekullMapper {
    suspend fun mapBarnekull(
        pdlKlient: PdlKlient,
        ppsKlient: ParallelleSannheterKlient,
        forelder: PdlHentPerson,
        saktyper: List<SakType>,
    ): Barnekull? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonsIdent != null
                }?.map { it.relatertPersonsIdent }
                ?.distinct()
                ?.map { Folkeregisteridentifikator.of(it) }

        val personerUtenIdent =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonUtenFolkeregisteridentifikator != null
                }?.map {
                    PersonUtenIdent(
                        RelativPersonrolle.BARN,
                        it.relatertPersonUtenFolkeregisteridentifikator!!.tilRelatertPerson(),
                    )
                }

        val personer =
            barnFnr?.let { fnr ->
                pdlKlient.hentPersonBolk(fnr, saktyper).data?.hentPersonBolk?.map {
                    PersonMapper.mapPerson(
                        ppsKlient,
                        pdlKlient,
                        Folkeregisteridentifikator.of(it.ident),
                        PersonRolle.TILKNYTTET_BARN,
                        it.person!!,
                        saktyper,
                    )
                }
            }

        return personer?.let { Barnekull(it, personerUtenIdent) }
    }

    suspend fun mapBarnekullPersonopplysning(
        ppsKlient: ParallelleSannheterKlient,
        pdlOboKlient: PdlOboKlient,
        forelder: PdlHentPerson,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): BarnekullPersonopplysning? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonsIdent != null
                }?.map { it.relatertPersonsIdent }
                ?.distinct()
                ?.map { Folkeregisteridentifikator.of(it) }

        val personer =
            barnFnr?.let { fnr ->
                fnr.map { ident ->
                    pdlOboKlient
                        .hentPerson(
                            ident,
                            PersonRolle.TILKNYTTET_BARN,
                            bruker = brukerTokenInfo,
                            sakType = sakType,
                        ).data
                        ?.hentPerson
                        ?.let {
                            PersonMapper.mapFamiliemedlem(
                                ppsKlient,
                                pdlOboKlient,
                                it,
                                ident,
                                sakType,
                                brukerTokenInfo = brukerTokenInfo,
                                PersonRolle.TILKNYTTET_BARN,
                            )
                        }
                }
            }

        return personer?.let { BarnekullPersonopplysning(it) }
    }
}
