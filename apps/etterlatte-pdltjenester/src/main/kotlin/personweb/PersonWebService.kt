package no.nav.etterlatte.personweb

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.SoekPerson
import no.nav.etterlatte.pdl.mapper.PersonMapper
import no.nav.etterlatte.personweb.dto.PersonNavnFoedselsaar
import no.nav.etterlatte.personweb.dto.PersonSoekSvar
import no.nav.etterlatte.personweb.familieOpplysninger.FamilieOpplysninger
import no.nav.etterlatte.personweb.familieOpplysninger.Familiemedlem
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(
    message: String,
) : ForespoerselException(
        status = 500,
        code = "UKJENT_FEIL_PDL",
        detail = message,
    )

class PersonWebService(
    private val pdlOboKlient: PdlOboKlient,
    private val ppsKlient: ParallelleSannheterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentPersonNavnOgFoedsel(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PersonNavnFoedselsaar {
        logger.info("Henter navn, fødselsdato og fødselsnummer for ident=${ident.maskerFnr()} fra PDL")

        return pdlOboKlient.hentPersonNavnOgFoedsel(ident, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString()

                if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke person i PDL")
                } else {
                    sikkerLogg.warn("Kunne ikke hente person med fnr=$ident fra PDL: $pdlFeil")
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med ident=${ident.maskerFnr()} se sikkerlogg for pdlfeil",
                    )
                }
            } else {
                PersonMapper.mapPersonNavnFoedsel(
                    ppsKlient = ppsKlient,
                    ident = ident,
                    hentPerson = it.data.hentPerson,
                )
            }
        }
    }

    suspend fun soekPerson(
        soekPerson: SoekPerson,
        bruker: BrukerTokenInfo,
    ): List<PersonSoekSvar> {
        logger.info("Søker etter person fra PDL")

        return pdlOboKlient.soekPerson(soekPerson, bruker).let { personSoekSvar ->
            if (personSoekSvar.errors?.isNotEmpty() != null) {
                sikkerLogg.warn("Fikk feil i PDL søk feil: {}", personSoekSvar.errors.toJson())
                throw PdlForesporselFeilet("Kunne ikke søke mot pdl se sikkerlogg for feil")
            } else {
                if (personSoekSvar.data != null) {
                    if (personSoekSvar.data.sokPerson?.hits != null) {
                        return coroutineScope {
                            val mappedePersoner =
                                personSoekSvar.data.sokPerson.hits.map {
                                    async {
                                        PersonMapper.mapPersonSoek(
                                            ppsKlient = ppsKlient,
                                            ident =
                                                it.person.folkeregisteridentifikator
                                                    .first { !it.metadata.historisk }
                                                    .identifikasjonsnummer,
                                            soekPerson = it.person,
                                        )
                                    }
                                }
                            mappedePersoner.map {
                                it.await()
                            }
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    suspend fun hentFamilieOpplysninger(
        ident: String,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ): FamilieOpplysninger {
        logger.info("Henter persongalleri for ident=${ident.maskerFnr()} fra PDL")

        return when (sakType) {
            SakType.BARNEPENSJON -> hentFamilieOpplysningerBarnepensjon(ident, bruker)
            SakType.OMSTILLINGSSTOENAD -> hentFamilieOpplysningerOmstillingsstoenad(ident, bruker)
        }
    }

    private suspend fun hentFamilieOpplysningerBarnepensjon(
        ident: String,
        bruker: BrukerTokenInfo,
    ): FamilieOpplysninger {
        val mottaker =
            hentFamiliemedlem(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.BARN,
                sakType = SakType.BARNEPENSJON,
                bruker = bruker,
            )

        val foreldre =
            mottaker.familierelasjon?.foreldre?.map {
                hentFamiliemedlem(
                    fnr = it,
                    rolle = PersonRolle.AVDOED,
                    sakType = SakType.BARNEPENSJON,
                    bruker,
                )
            } ?: emptyList()

        val (avdoede, gjenlevende) = foreldre.partition { it.doedsdato != null }

        return FamilieOpplysninger(
            soeker = mottaker,
            avdoede = avdoede,
            gjenlevende = gjenlevende,
        )
    }

    private suspend fun hentFamilieOpplysningerOmstillingsstoenad(
        ident: String,
        bruker: BrukerTokenInfo,
    ): FamilieOpplysninger {
        val mottaker =
            hentFamiliemedlem(
                fnr = Folkeregisteridentifikator.of(ident),
                rolle = PersonRolle.GJENLEVENDE,
                sakType = SakType.OMSTILLINGSSTOENAD,
                bruker,
            )

        val partnerVedSivilstand =
            mottaker.sivilstand
                ?.filter {
                    listOf(
                        Sivilstatus.GIFT,
                        Sivilstatus.GJENLEVENDE_PARTNER,
                        Sivilstatus.ENKE_ELLER_ENKEMANN,
                    ).contains(it.sivilstatus)
                }?.mapNotNull { it.relatertVedSivilstand } ?: emptyList()

        val (avdoede, levende) =
            partnerVedSivilstand
                .map {
                    hentFamiliemedlem(
                        fnr = it,
                        rolle = PersonRolle.AVDOED,
                        sakType = SakType.OMSTILLINGSSTOENAD,
                        bruker,
                    )
                }.partition { it.doedsdato != null }

        return FamilieOpplysninger(
            soeker = mottaker,
            avdoede = avdoede,
            gjenlevende = levende,
        )
    }

    private suspend fun hentFamiliemedlem(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ): Familiemedlem {
        logger.info("Henter person med fnr=$fnr fra PDL")

        return pdlOboKlient.hentPerson(fnr, rolle, sakType, bruker).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen $fnr")
                } else {
                    sikkerLogg.warn("Kunne ikke hente person med fnr=$fnr fra PDL: $pdlFeil")
                    throw no.nav.etterlatte.person.PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=$fnr se sikkerlogg for feilmelding",
                    )
                }
            } else {
                PersonMapper.mapFamiliemedlem(
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

    private fun List<PdlResponseError>.harAdressebeskyttelse() =
        any { error ->
            error.extensions?.code == "unauthorized" &&
                error.extensions
                    .details
                    ?.policy
                    ?.let { policy ->
                        policy.contains("adressebeskyttelse_fortrolig_adresse") ||
                            policy.contains("adressebeskyttelse_strengt_fortrolig_adresse")
                    } == true
        }

    private fun pdlForesporselFeiletForAdressebeskyttelse(): Throwable =
        throw PdlForesporselFeilet(
            "Denne personen har adressebeskyttelse. Behandlingen skal derfor sendes til enhet Vikafossen som vil behandle saken videre.",
        )
}
