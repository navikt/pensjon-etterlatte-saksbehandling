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

    // TODO: Hent persongalleri -> Hent personer utifra ny datastruktur, se på PersonService, lag dette til å funke med pdlOboKlient

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
                    rolle = PersonRolle.GJENLEVENDE,
                    saktyper = listOf(SakType.BARNEPENSJON),
                    bruker,
                )
            } ?: emptyList()

        val (avdoede, gjenlevende) = foreldre.partition { it.doedsdato != null }

        return Personopplysninger(
            soeker = mottaker,
            avdoede = avdoede,
            gjenlevende = gjenlevende,
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
                    rolle = PersonRolle.GJENLEVENDE,
                    saktyper = listOf(SakType.OMSTILLINGSSTOENAD),
                    bruker,
                )
            }.partition { it.doedsdato != null }

        return Personopplysninger(
            soeker = mottaker,
            avdoede = avdoede,
            gjenlevende = levende,
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

    data class Personopplysninger(
        val soeker: Person?,
        val avdoede: List<Person>,
        val gjenlevende: List<Person>,
    )

    private fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}
