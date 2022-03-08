package no.nav.etterlatte.fordeler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.fordeler.FordelerResultat.GyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.IkkeGyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.UgyldigHendelse
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


sealed class FordelerResultat {
    object GyldigForBehandling : FordelerResultat()
    class UgyldigHendelse(val message: String) : FordelerResultat()
    class IkkeGyldigForBehandling(val ikkeOppfylteKriterier: List<FordelerKriterie>) : FordelerResultat()
}

class FordelerService(
    private val fordelerKriterierService: FordelerKriterierService,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val klokke: Clock = Clock.systemUTC()
) {

    fun sjekkGyldighetForBehandling(event: FordelerEvent): FordelerResultat = runBlocking {
        val soeknad: Barnepensjon = event.soeknad

        when {
            ugyldigHendelse(event) ->
                UgyldigHendelse(
                    "Hendelsen er ikke lenger gyldig (${hendelseGyldigTil(event)})"
                )

            else -> {
                val barn = pdlTjenesterKlient.hentPerson(hentBarnRequest(soeknad))
                val avdoed = pdlTjenesterKlient.hentPerson(hentAvdoedRequest(soeknad))
                val gjenlevende = pdlTjenesterKlient.hentPerson(hentGjenlevendeRequest(soeknad))

                fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, soeknad).let {
                    if (it.kandidat) GyldigForBehandling
                    else IkkeGyldigForBehandling(it.forklaring)
                }
            }
        }
    }

    private fun ugyldigHendelse(event: FordelerEvent) =
        event.hendelseGyldigTil.isBefore(OffsetDateTime.now(klokke))

    private fun hendelseGyldigTil(event: FordelerEvent) =
        event.hendelseGyldigTil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun hentGjenlevendeRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.foreldre.first { it.type == PersonType.GJENLEVENDE_FORELDER }.foedselsnummer.svar,
            rolle = PersonRolle.GJENLEVENDE
        )

    private fun hentAvdoedRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.foreldre.first { it.type == PersonType.AVDOED }.foedselsnummer.svar,
            rolle = PersonRolle.AVDOED
        )

    private fun hentBarnRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.soeker.foedselsnummer.svar,
            rolle = PersonRolle.BARN
        )
}