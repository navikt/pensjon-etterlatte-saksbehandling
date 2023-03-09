package no.nav.etterlatte.fordeler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.fordeler.FordelerResultat.GyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.IkkeGyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.UgyldigHendelse
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.pdltjenester.PersonFinnesIkkeException
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

enum class Vedtaksloesning {
    DOFFEN, PESYS
}
data class Fordeling(val soeknadId: Long, val fordeltTil: Vedtaksloesning, val kriterier: List<FordelerKriterie>)

sealed class FordelerResultat {
    object GyldigForBehandling : FordelerResultat()
    class UgyldigHendelse(val message: String) : FordelerResultat()
    class IkkeGyldigForBehandling(val ikkeOppfylteKriterier: List<FordelerKriterie>) : FordelerResultat()
}

class FordelerService(
    private val fordelerKriterier: FordelerKriterier,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val fordelerRepository: FordelerRepository,
    private val klokke: Clock = utcKlokke(),
    private val maxFordelingTilDoffen: Long
) {
    fun sjekkGyldighetForBehandling(event: FordelerEvent): FordelerResultat = runBlocking {
        if (ugyldigHendelse(event)) {
            UgyldigHendelse(
                "Hendelsen er ikke lenger gyldig (${hendelseGyldigTil(event)})"
            )
        } else {
            try {
                fordelSoeknad(event).let {
                    when (it.fordeltTil) {
                        Vedtaksloesning.DOFFEN -> GyldigForBehandling
                        Vedtaksloesning.PESYS -> IkkeGyldigForBehandling(it.kriterier)
                    }
                }
            } catch (e: PersonFinnesIkkeException) {
                UgyldigHendelse("Person fra søknaden med fnr=${e.fnr} finnes ikke i PDL")
            }
        }
    }

    private fun fordelSoeknad(event: FordelerEvent): Fordeling {
        return finnEksisterendeFordeling(event.soeknadId) ?: nyFordeling(event).apply { lagre() }
    }

    private fun finnEksisterendeFordeling(soeknadId: Long): Fordeling? =
        fordelerRepository
            .finnFordeling(soeknadId)
            ?.let {
                Fordeling(
                    it.soeknadId,
                    Vedtaksloesning.valueOf(it.fordeling),
                    fordelerRepository.finnKriterier(it.soeknadId).map { FordelerKriterie.valueOf(it) }
                )
            }

    private fun Fordeling.lagre() {
        fordelerRepository.lagreFordeling(FordeltTransient(soeknadId, fordeltTil.name, kriterier.map { it.name }))
    }
    private fun nyFordeling(event: FordelerEvent): Fordeling = runBlocking {
        val soeknad: Barnepensjon = event.soeknad

        val barn = pdlTjenesterKlient.hentPerson(hentBarnRequest(soeknad))
        val avdoed = pdlTjenesterKlient.hentPerson(hentAvdoedRequest(soeknad))
        val gjenlevende = pdlTjenesterKlient.hentPerson(hentGjenlevendeRequest(soeknad))

        fordelerKriterier.sjekkMotKriterier(barn, avdoed, gjenlevende, soeknad).let {
            if (it.kandidat &&
                fordelerRepository.antallFordeltTil(Vedtaksloesning.DOFFEN.name) < maxFordelingTilDoffen
            ) {
                Fordeling(event.soeknadId, Vedtaksloesning.DOFFEN, emptyList())
            } else {
                Fordeling(event.soeknadId, Vedtaksloesning.PESYS, it.forklaring)
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