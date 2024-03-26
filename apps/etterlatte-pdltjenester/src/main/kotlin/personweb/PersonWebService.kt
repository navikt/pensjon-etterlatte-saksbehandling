package no.nav.etterlatte.personweb

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.PersonMapper
import no.nav.etterlatte.personweb.dto.PersonopplysningPerson
import no.nav.etterlatte.personweb.dto.Personopplysninger
import org.slf4j.LoggerFactory
import personweb.dto.PersonNavn

class PdlForesporselFeilet(message: String) : ForespoerselException(
    status = 500,
    code = "UKJENT_FEIL_PDL",
    detail = message,
)

class PersonWebService(
    private val pdlOboKlient: PdlOboKlient,
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
            hentPersonopplysningPerson(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.BARN,
                sakType = SakType.BARNEPENSJON,
                bruker = bruker,
            )

        val foreldre =
            mottaker.familierelasjon?.ansvarligeForeldre?.map {
                hentPersonopplysningPerson(
                    fnr = it,
                    rolle = PersonRolle.AVDOED,
                    sakType = SakType.BARNEPENSJON,
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
            hentPersonopplysningPerson(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.GJENLEVENDE,
                sakType = SakType.OMSTILLINGSSTOENAD,
                bruker,
            )

        val partnerVedSivilstand =
            mottaker.sivilstand?.filter {
                listOf(
                    Sivilstatus.GIFT,
                    Sivilstatus.GJENLEVENDE_PARTNER,
                    Sivilstatus.ENKE_ELLER_ENKEMANN,
                ).contains(it.sivilstatus)
            }?.mapNotNull { it.relatertVedSivilstand } ?: emptyList()

        val (avdoede, levende) =
            partnerVedSivilstand.map {
                hentPersonopplysningPerson(
                    fnr = it,
                    rolle = PersonRolle.AVDOED,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    bruker,
                )
            }.partition { it.doedsdato != null }

        return Personopplysninger(
            soeker = mottaker,
            avdoede = avdoede,
            gjenlevende = levende,
        )
    }

    private suspend fun hentPersonopplysningPerson(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ): PersonopplysningPerson {
        logger.info("Henter person med fnr=$fnr fra PDL")

        return pdlOboKlient.hentPersonopplysningPerson(fnr, rolle, sakType, bruker).let {
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
                PersonMapper.mapPersonopplysningPerson(
                    ppsKlient = ppsKlient,
                    pdlOboKlient = pdlOboKlient,
                    ident = fnr,
                    hentPerson = it.data.hentPerson,
                    sakType = sakType,
                    brukerTokenInfo = bruker,
                    personRolle = rolle,
                )
            }
        }
    }

    private fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}
