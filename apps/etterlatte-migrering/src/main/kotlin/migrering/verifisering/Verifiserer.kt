package no.nav.etterlatte.migrering.verifisering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.grunnlag.GrunnlagKlient
import no.nav.etterlatte.migrering.grunnlag.Utenlandstilknytningsjekker
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month

internal class Verifiserer(
    private val repository: PesysRepository,
    private val gjenlevendeForelderPatcher: GjenlevendeForelderPatcher,
    private val utenlandstilknytningsjekker: Utenlandstilknytningsjekker,
    private val personHenter: PersonHenter,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest): MigreringRequest {
        val patchedRequest = gjenlevendeForelderPatcher.patchGjenlevendeHvisIkkeOppgitt(request)
        val feil = mutableListOf<Exception>()
        patchedRequest.onFailure { feilen ->
            feil.add(PDLException(feilen).also { it.addSuppressed(feilen) })
        }
        patchedRequest.onSuccess {
            if (request.enhet.nr == "2103") {
                feil.add(StrengtFortrolig)
            }
            feil.addAll(sjekkAtPersonerFinsIPDL(it))
            feil.addAll(sjekkAtSoekerHarRelevantVerge(it))
        }

        if (feil.isNotEmpty()) {
            haandterFeil(request, feil)
        }
        return patchedRequest.getOrThrow().copy(
            utlandstilknytningType = utenlandstilknytningsjekker.finnUtenlandstilknytning(request),
        )
    }

    private fun haandterFeil(
        request: MigreringRequest,
        feil: MutableList<Exception>,
    ) {
        logger.warn(
            "Sak ${request.pesysId} har ufullstendige data i PDL, eller feiler verifisering av andre grunner. " +
                "Kan ikke migrere. Se sikkerlogg for detaljer",
        )
        repository.lagreFeilkjoering(
            request.toJson(),
            feilendeSteg = Migreringshendelser.VERIFISER,
            feil = feil.map { it.message }.toJson(),
            pesysId = request.pesysId,
        )
        repository.oppdaterStatus(request.pesysId, Migreringsstatus.VERIFISERING_FEILA)
        throw samleExceptions(feil)
    }

    private fun sjekkAtPersonerFinsIPDL(request: MigreringRequest): List<Verifiseringsfeil> {
        val personer = mutableListOf(Pair(PersonRolle.BARN, request.soeker))
        request.avdoedForelder.forEach { personer.add(Pair(PersonRolle.AVDOED, it.ident)) }
        request.gjenlevendeForelder?.let { personer.add(Pair(PersonRolle.GJENLEVENDE, it)) }

        return personer
            .map {
                val person = personHenter.hentPerson(it.first, it.second)

                if (it.first == PersonRolle.BARN) {
                    val foedselsdato: LocalDate =
                        person.getOrNull()?.foedselsdato?.verdi
                            ?: request.soeker.getBirthDate()
                    if (foedselsdato.isBefore(LocalDate.of(2006, Month.JANUARY, 1)) && !request.dodAvYrkesskade) {
                        logger.warn("Søker er over 18 år og det er ikke yrkesskade")
                        return listOf(SoekerErOver18)
                    }
                }
                person
            }
            .filter { it.isFailure }
            .map { it.exceptionOrNull() }
            .filterIsInstance<Verifiseringsfeil>()
    }

    private fun sjekkAtSoekerHarRelevantVerge(request: MigreringRequest): List<Verifiseringsfeil> {
        val person =
            personHenter.hentPerson(PersonRolle.BARN, request.soeker).getOrNull()
                ?: return listOf(FinsIkkeIPDL(PersonRolle.BARN, request.soeker))

        if ((person.vergemaalEllerFremtidsfullmakt?.size ?: 0) > 1) {
            return listOf(BarnetHarFlereVerger)
        }
        if (person.vergemaalEllerFremtidsfullmakt.isNullOrEmpty()) {
            return listOf()
        }

        if (!featureToggleService.isEnabled(MigreringFeatureToggle.MigrerNaarSoekerHarVerge, false)) {
            return listOf(BarnetHarVergemaal)
        } else {
            try {
                return person.vergemaalEllerFremtidsfullmakt!!
                    .map { it.verdi }
                    .map { it.vergeEllerFullmektig }
                    .map { it.motpartsPersonident }
                    .map { it!!.value }
                    .mapNotNull {
                        runBlocking {
                            val vergesAdresse = grunnlagKlient.hentVergesAdresse(it)
                            return@runBlocking if (vergesAdresse == null) {
                                VergeManglerAdresseFraPDL
                            } else {
                                null
                            }
                        }
                    }
            } catch (e: Exception) {
                logger.error("Feil under henting av verges adresse", e)
                return listOf(FeilUnderHentingAvVergesAdresse)
            }
        }
    }
}

sealed class Verifiseringsfeil : Exception()

data class FinsIkkeIPDL(val rolle: PersonRolle, val id: Folkeregisteridentifikator) : Verifiseringsfeil() {
    override val message: String
        get() = toString()
}

object BarnetHarFlereVerger : Verifiseringsfeil() {
    override val message: String
        get() = "Barnet har flere verger"
}

object BarnetHarVergemaal : Verifiseringsfeil() {
    override val message: String
        get() = "Barn har vergemål eller framtidsfullmakt, støtte for det er deaktivert"
}

object StrengtFortrolig : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere strengt fortrolig sak"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
}

object SoekerErOver18 : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke per nå migrere søker der søker er over 18"
}

object VergeManglerAdresseFraPDL : Verifiseringsfeil() {
    override val message: String
        get() = "Verge mangler adresse i PDL"
}

object FeilUnderHentingAvVergesAdresse : Verifiseringsfeil() {
    override val message: String
        get() = "Noe feil skjedde under henting av verges adresse, se detaljer i logg"
}
