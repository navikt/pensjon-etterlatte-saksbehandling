package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
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
) {
    private val sikkerlogg = sikkerlogger()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest): MigreringRequest {
        val patchedRequest = patchGjenlevendeHvisIkkeOppgitt(request)
        val feil = mutableListOf<Exception>()
        patchedRequest.onFailure { feilen ->
            feil.add(PDLException(feilen).also { it.addSuppressed(feilen) })
        }
        patchedRequest.onSuccess {
            feil.addAll(sjekkAtPersonerFinsIPDL(it))
        }
        verifiserFolketrygdBeregning(request)?.let { feil.add(it) }

        if (feil.isNotEmpty()) {
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
        return patchedRequest.getOrThrow()
    }

    private fun patchGjenlevendeHvisIkkeOppgitt(request: MigreringRequest): Result<MigreringRequest> {
        if (request.gjenlevendeForelder != null) {
            return Result.success(request)
        }
        val persongalleri = hentPersongalleri(request.soeker)
        if (persongalleri == null) {
            logger.warn(
                "Kunne ikke hente persongalleriet fra PDL for migrering av pesysid=${request.pesysId}, " +
                    "sannsynligvis på grunn av personer som mangler identer. Gjør ingen patching av persongalleriet",
            )
            return Result.success(request)
        }
        val avdoede = request.avdoedForelder.map { it.ident.value }.toSet()
        val avdodeIPDL = persongalleri.avdoed.toSet()
        if (avdoede != avdodeIPDL) {
            logger.error(
                "Migreringrequest med pesysid=${request.pesysId} har forskjellige avdøde enn det vi finner " +
                    "i PDL.",
            )
            sikkerlogg.error("Fikk $avdodeIPDL fra PDL, forventa $avdoede. Hele persongalleriet: $persongalleri")
            return Result.failure(IllegalStateException("Migreringsrequest har forskjellig sett med avdøde enn det vi har i følge PDL"))
        }
        if (persongalleri.gjenlevende.size == 1) {
            return Result.success(request.copy(gjenlevendeForelder = Folkeregisteridentifikator.of(persongalleri.gjenlevende.single())))
        }
        logger.warn("Fant ${persongalleri.gjenlevende.size} gjenlevende i PDL, patcher ikke request")
        return Result.success(request)
    }

    private fun hentPersongalleri(soeker: Folkeregisteridentifikator): Persongalleri? {
        return try {
            pdlKlient.hentPersongalleri(soeker)
        } catch (e: Exception) {
            logger.info("Persongalleriet ble hentet med feil, returnerer null i stedet")
            null
        }
    }

    private fun verifiserFolketrygdBeregning(request: MigreringRequest): Verifiseringsfeil? {
        val beregningsMetode = request.beregning.meta?.beregningsMetodeType
        if (beregningsMetode != "FOLKETRYGD" || request.beregning.prorataBroek != null) {
            return SakHarIkkeFolketrygdBeregning
        }
        return null
    }

    private fun sjekkAtPersonerFinsIPDL(request: MigreringRequest): List<Verifiseringsfeil> {
        val personer = mutableListOf(Pair(PersonRolle.BARN, request.soeker))
        request.avdoedForelder.forEach { personer.add(Pair(PersonRolle.AVDOED, it.ident)) }
        if (request.gjenlevendeForelder == null) {
            return listOf(GjenlevendeForelderMangler)
        }
        if (request.enhet.nr in listOf("0001", "4862")) {
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

object SakHarIkkeFolketrygdBeregning : Verifiseringsfeil() {
    override val message: String
        get() = "Skal ikke migrere saker med EØS beregning eller proratabrøk"
}

data class PDLException(val kilde: Throwable) : Verifiseringsfeil() {
    override val message: String?
        get() = kilde.message
}
