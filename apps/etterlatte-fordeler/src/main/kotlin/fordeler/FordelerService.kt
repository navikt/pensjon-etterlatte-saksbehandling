package no.nav.etterlatte.fordeler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.fordeler.FordelerResultat.GyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.IkkeGyldigForBehandling
import no.nav.etterlatte.fordeler.FordelerResultat.UgyldigHendelse
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.person.FamilieRelasjonManglerIdent
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.etterlatte.pdltjenester.PersonFinnesIkkeException
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.skjerming.SkjermingKlient
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
    private val behandlingKlient: BehandlingKlient,
    private val skjermingKlient: SkjermingKlient,
    private val skalBrukeSkjermingsklient: Boolean,
    private val klokke: Clock = utcKlokke(),
    private val maxFordelingTilDoffen: Long
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun sjekkGyldighetForBehandling(event: FordelerEvent): FordelerResultat {
        if (ugyldigHendelse(event)) {
            return UgyldigHendelse(
                "Hendelsen er ikke lenger gyldig (${hendelseGyldigTil(event)})"
            )
        }
        return try {
            fordelSoeknad(event).let {
                when (it.fordeltTil) {
                    Vedtaksloesning.DOFFEN -> GyldigForBehandling
                    Vedtaksloesning.PESYS -> IkkeGyldigForBehandling(it.kriterier)
                }
            }
        } catch (e: FamilieRelasjonManglerIdent) {
            logger.warn(
                "Fikk en familierelasjon som mangler ident fra PDL. Disse tilfellene støtter vi ikke per nå." +
                    " Se sikkerlogg for detaljer"
            )
            sikkerLogg.info("Fikk en søknad med en familierelasjon som manglet ident", e)
            IkkeGyldigForBehandling(listOf(FordelerKriterie.FAMILIERELASJON_MANGLER_IDENT))
        } catch (e: PersonFinnesIkkeException) {
            UgyldigHendelse("Person fra søknaden med fnr=${e.fnr} finnes ikke i PDL")
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

        val barnetErSkjermet = if (skalBrukeSkjermingsklient) {
            skjermingKlient.personErSkjermet(barn.foedselsnummer.value)
        } else {
            false
        }

        fordelerKriterier.sjekkMotKriterier(barn, avdoed, gjenlevende, soeknad, barnetErSkjermet).let {
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
            foedselsnummer = soeknad.foreldre.first {
                it.type == PersonType.GJENLEVENDE_FORELDER
            }.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.GJENLEVENDE
        )

    private fun hentAvdoedRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.foreldre.first {
                it.type == PersonType.AVDOED
            }.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.AVDOED
        )

    private fun hentBarnRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.soeker.foedselsnummer.svar.toFolkeregisteridentifikator(),
            rolle = PersonRolle.BARN
        )

    fun hentSakId(fnr: String, barnepensjon: SakType): Long {
        return runBlocking {
            behandlingKlient.hentSak(fnr, barnepensjon)
        }
    }
}

fun Foedselsnummer.toFolkeregisteridentifikator(): Folkeregisteridentifikator {
    return Folkeregisteridentifikator.of(this.value)
}