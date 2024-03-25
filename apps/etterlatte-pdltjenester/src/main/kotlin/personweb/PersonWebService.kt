package no.nav.etterlatte.personweb

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.PersonMapper
import no.nav.etterlatte.personweb.dto.Bostedsadresse
import no.nav.etterlatte.personweb.dto.Familierelasjon
import no.nav.etterlatte.personweb.dto.PdlStatsborgerskap
import no.nav.etterlatte.personweb.dto.PersonopplysningPerson
import no.nav.etterlatte.personweb.dto.Personopplysninger
import no.nav.etterlatte.personweb.dto.Sivilstand
import org.slf4j.LoggerFactory
import personweb.dto.PersonNavn

class PdlForesporselFeilet(message: String) : ForespoerselException(
    status = 500,
    code = "UKJENT_FEIL_PDL",
    detail = message,
)

class PersonWebService(
    private val pdlOboKlient: PdlOboKlient,
    private val pdlKlient: PdlKlient,
    private val ppsKlient: ParallelleSannheterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentPersonNavn(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PersonNavn {
        logger.info("Henter navn for ident=${ident.maskerFnr()} fra PDL")

        return pdlOboKlient.hentPersonNavn(ident, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString()

                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke person i PDL")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med ident=${ident.maskerFnr()} fra PDL: $pdlFeil",
                    )
                }
            } else {
                PersonMapper.mapPersonNavn(
                    ppsKlient = ppsKlient,
                    ident = ident,
                    hentPerson = it.data.hentPerson,
                )
            }
        }
    }

    suspend fun hentPersonopplysninger(
        ident: String,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ): Personopplysninger {
        logger.info("Henter persongalleri for ident=${ident.maskerFnr()} fra PDL")

        return when (sakType) {
            SakType.BARNEPENSJON -> hentPersonopplysningerBarnepensjon(ident, bruker)
            SakType.OMSTILLINGSSTOENAD -> hentPersonopplysningerForOmstillingsstoenad(ident, bruker)
        }
    }

    private suspend fun hentPersonopplysningerBarnepensjon(
        ident: String,
        bruker: BrukerTokenInfo,
    ): Personopplysninger {
        val mottaker =
            hentPerson(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.BARN,
                saktyper = listOf(SakType.BARNEPENSJON),
                bruker = bruker,
            )

        val foreldre =
            mottaker.familieRelasjon?.foreldre?.map {
                hentPerson(
                    fnr = it,
                    rolle = PersonRolle.AVDOED,
                    saktyper = listOf(SakType.BARNEPENSJON),
                    bruker,
                )
            } ?: emptyList()

        val (avdoede, gjenlevende) = foreldre.partition { it.doedsdato != null }

        return Personopplysninger(
            soeker = personTilPersonopplysningPerson(mottaker),
            avdoede = avdoede.map { personTilPersonopplysningPerson(it) },
            gjenlevende = gjenlevende.map { personTilPersonopplysningPerson(it) },
        )
    }

    private suspend fun hentPersonopplysningerForOmstillingsstoenad(
        ident: String,
        bruker: BrukerTokenInfo,
    ): Personopplysninger {
        val mottaker =
            hentPerson(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.GJENLEVENDE,
                saktyper = listOf(SakType.OMSTILLINGSSTOENAD),
                bruker,
            )

        val partnerVedSivilstand =
            mottaker.sivilstand?.filter {
                listOf(
                    Sivilstatus.GIFT,
                    Sivilstatus.GJENLEVENDE_PARTNER,
                    Sivilstatus.ENKE_ELLER_ENKEMANN,
                ).contains(it.sivilstatus)
            }?.mapNotNull { it.relatertVedSiviltilstand } ?: emptyList()

        val (avdoede, levende) =
            partnerVedSivilstand.map {
                hentPerson(
                    fnr = it,
                    rolle = PersonRolle.AVDOED,
                    saktyper = listOf(SakType.OMSTILLINGSSTOENAD),
                    bruker,
                )
            }.partition { it.doedsdato != null }

        return Personopplysninger(
            soeker = personTilPersonopplysningPerson(mottaker),
            avdoede = avdoede.map { personTilPersonopplysningPerson(it) },
            gjenlevende = levende.map { personTilPersonopplysningPerson(it) },
        )
    }

    private suspend fun hentPerson(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        saktyper: List<SakType>,
        bruker: BrukerTokenInfo,
    ): Person {
        logger.info("Henter person med fnr=$fnr fra PDL")

        return pdlOboKlient.hentPerson(fnr, rolle, saktyper, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen $fnr")
                } else {
                    throw no.nav.etterlatte.person.PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=$fnr fra PDL: $pdlFeil",
                    )
                }
            } else {
                // TODO: lage egen mapper for person, slik at vi ikke bruker pdlKlient, VERY bad security
                PersonMapper.mapPerson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = fnr,
                    personRolle = rolle,
                    hentPerson = it.data.hentPerson,
                    saktyper = saktyper,
                )
            }
        }
    }

    private fun personTilPersonopplysningPerson(person: Person?): PersonopplysningPerson? {
        if (person != null) {
            return PersonopplysningPerson(
                person.fornavn,
                person.etternavn,
                person.foedselsnummer,
                person.foedselsdato,
                person.doedsdato,
                person.bostedsadresse?.map {
                    Bostedsadresse(it.adresseLinje1, it.postnr, it.gyldigFraOgMed, it.gyldigTilOgMed, it.aktiv)
                },
                person.sivilstand?.map {
                    Sivilstand(it.sivilstatus, it.relatertVedSiviltilstand, it.gyldigFraOgMed)
                },
                person.statsborgerskap,
                person.pdlStatsborgerskap?.map {
                    PdlStatsborgerskap(it.land, it.gyldigFraOgMed, it.gyldigTilOgMed)
                },
                person.utland,
                Familierelasjon(person.familieRelasjon?.ansvarligeForeldre, person.familieRelasjon?.barn),
                person.avdoedesBarn?.map {
                    personTilPersonopplysningPerson(it)
                },
                person.vergemaalEllerFremtidsfullmakt,
            )
        } else {
            return null
        }
    }

    private fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}
