package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import org.slf4j.LoggerFactory

internal class Verifiserer(
    private val pdlKlient: PDLKlient,
    private val repository: PesysRepository,
    private val featureToggleService: FeatureToggleService,
    private val gjenlevendeForelderPatcher: GjenlevendeForelderPatcher,
) {
    private val sikkerlogg = sikkerlogger()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest): MigreringRequest {
        val patchedRequest = gjenlevendeForelderPatcher.patchGjenlevendeHvisIkkeOppgitt(request)
        val feil = mutableListOf<Exception>()
        patchedRequest.onFailure { feilen ->
            feil.add(PDLException(feilen).also { it.addSuppressed(feilen) })
        }
        patchedRequest.onSuccess {
            feil.addAll(sjekkAtPersonerFinsIPDL(it))
        }
        if (feil.isNotEmpty()) {
            haandterFeil(request, feil)
        }
        return patchedRequest.getOrThrow()
    }

    private fun haandterFeil(
        request: MigreringRequest,
        feil: MutableList<Exception>,
    ) {
        logger.warn(
            "Sak ${request.pesysId} har ufullstendige data i PDL, kan ikke migrere. Se sikkerlogg for detaljer",
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
        if (request.gjenlevendeForelder == null) {
            return listOf(GjenlevendeForelderMangler)
        }
        if (request.enhet.nr in listOf("0001")) {
            return listOf(EnhetUtland(request.enhet.nr))
        }
        if (request.enhet.nr == "2103") {
            return listOf(StrengtFortrolig)
        }
        request.gjenlevendeForelder!!.let { personer.add(Pair(PersonRolle.GJENLEVENDE, it)) }

        return personer
            .map {
                val person = hentPerson(it.first, it.second)

                if (it.first == PersonRolle.BARN) {
                    person.getOrNull()?.let { pdlPerson ->
                        pdlPerson.vergemaalEllerFremtidsfullmakt?.let { vergemaal ->
                            if (vergemaal.isNotEmpty()) {
                                logger.warn("Barn har vergemaal eller fremtidsfullmakt, kan ikke migrere")
                                return listOf(BarnetHarVerge)
                            }
                        }
                    }
                }

                person
            }
            .filter { it.isFailure }
            .map { it.exceptionOrNull() }
            .filterIsInstance<Verifiseringsfeil>()
    }

    private fun hentPerson(
        rolle: PersonRolle,
        folkeregisteridentifikator: Folkeregisteridentifikator,
    ): Result<PersonDTO?> =
        try {
            Result.success(pdlKlient.hentPerson(rolle, folkeregisteridentifikator))
        } catch (e: Exception) {
            sikkerlogg.warn("Fant ikke person $folkeregisteridentifikator med rolle $rolle i PDL", e)
            if (featureToggleService.isEnabled(MigreringFeatureToggle.VerifiserFoerMigrering, true)) {
                Result.failure(FinsIkkeIPDL(rolle, folkeregisteridentifikator))
            } else {
                logger.warn("Har skrudd av at vi feiler migreringa hvis verifisering feiler. Fortsetter.")
                Result.success(null)
            }
        }
}

sealed class Verifiseringsfeil : Exception()

data class FinsIkkeIPDL(val rolle: PersonRolle, val id: Folkeregisteridentifikator) : Verifiseringsfeil() {
    override val message: String
        get() = toString()
}

object GjenlevendeForelderMangler : Verifiseringsfeil() {
    override val message: String
        get() = "Gjenlevende forelder er null i det vi får fra Pesys"
}

object BarnetHarVerge : Verifiseringsfeil() {
    override val message: String
        get() = "Barn har vergemaal eller fremtidsfullmakt, kan ikke migrere"
}

data class EnhetUtland(val enhet: String) : Verifiseringsfeil() {
    override val message: String
        get() = "Vi har ikke adresse for enhet utland $enhet. Må følges opp snart"
}

object StrengtFortrolig : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere strengt fortrolig sak"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
}
