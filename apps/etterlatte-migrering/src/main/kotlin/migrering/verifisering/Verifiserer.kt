package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import org.slf4j.LoggerFactory

internal class Verifiserer(private val pdlKlient: PDLKlient, private val repository: PesysRepository) {
    private val sikkerlogg = sikkerlogger()
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun verifiserRequest(request: MigreringRequest) {
        val feil = sjekkAtPersonerFinsIPDL(request)
        if (feil.isNotEmpty()) {
            logger.warn(
                "Sak ${request.pesysId} har ufullstendige data i PDL, kan ikke migrere. Se sikkerlogg for detaljer"
            )
            repository.lagreFeilkjoering(request, feil)
            repository.oppdaterStatus(request.pesysId, Migreringsstatus.VERIFISERING_FEILA)
            throw samleExceptions(feil)
        }
    }

    private fun sjekkAtPersonerFinsIPDL(request: MigreringRequest): List<Verifiseringsfeil> {
        val personer = mutableListOf(Pair(PersonRolle.BARN, request.soeker))
        request.avdoedForelder.forEach { personer.add(Pair(PersonRolle.AVDOED, it.ident)) }
        request.gjenlevendeForelder?.let { personer.add(Pair(PersonRolle.GJENLEVENDE, it)) }

        return personer.map { hentPerson(it.first, it.second) }
            .filter { it.isFailure }
            .map { it.exceptionOrNull() }
            .filterIsInstance<Verifiseringsfeil>()
    }

    private fun hentPerson(
        rolle: PersonRolle,
        folkeregisteridentifikator: Folkeregisteridentifikator
    ): Result<PersonDTO> =
        try {
            Result.success(pdlKlient.hentPerson(rolle, folkeregisteridentifikator))
        } catch (e: Exception) {
            sikkerlogg.warn("Fant ikke person $folkeregisteridentifikator med rolle $rolle i PDL", e)
            Result.failure(FinsIkkeIPDL(rolle, folkeregisteridentifikator))
        }
}

sealed class Verifiseringsfeil : Exception()

data class FinsIkkeIPDL(val rolle: PersonRolle, val id: Folkeregisteridentifikator) : Verifiseringsfeil() {
    override val message: String
        get() = toString()
}
