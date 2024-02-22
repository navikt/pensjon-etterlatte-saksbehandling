package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.logging.samleExceptions
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import org.slf4j.LoggerFactory

internal class GjenlevendeForelderPatcher(val pdlTjenesterKlient: PdlTjenesterKlient, private val personHenter: PersonHenter) {
    private val sikkerlogg = sikkerlogger()
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal fun patchGjenlevendeHvisIkkeOppgitt(request: MigreringRequest): Result<MigreringRequest> {
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
            val pdlresultat = sjekkAvdoedForelderMotPDL(request, avdodeIPDL, avdoede, persongalleri)
            if (pdlresultat != null) {
                logger.error("Fant ikke avdød forelder i PDL. Se sikkerlogg for detaljer")
                sikkerlogg.error("Fant ikke avdød forelder i PDL", pdlresultat)
                return Result.failure(pdlresultat)
            }
        }
        if (persongalleri.gjenlevende.size == 1) {
            return Result.success(request.copy(gjenlevendeForelder = Folkeregisteridentifikator.of(persongalleri.gjenlevende.single())))
        }
        logger.warn("Fant ${persongalleri.gjenlevende.size} gjenlevende i PDL, patcher ikke request")
        return Result.success(request)
    }

    private fun sjekkAvdoedForelderMotPDL(
        request: MigreringRequest,
        avdodeIPDL: Set<String>,
        avdoede: Set<String>,
        persongalleri: Persongalleri?,
    ): Exception? {
        logger.warn(
            "Migreringrequest med pesysid=${request.pesysId} har forskjellige avdøde enn det vi finner " +
                "i PDL.",
        )
        sikkerlogg.warn("Fikk $avdodeIPDL fra PDL, forventa $avdoede. Hele persongalleriet: $persongalleri")

        val exceptions =
            request.avdoedForelder.map { personHenter.hentPerson(PersonRolle.AVDOED, it.ident) }
                .filter { it.isFailure }
                .mapNotNull { it.exceptionOrNull() }
                .map { RuntimeException(it) }

        return if (exceptions.isNotEmpty()) {
            samleExceptions(exceptions)
        } else {
            null
        }
    }

    private fun hentPersongalleri(soeker: Folkeregisteridentifikator): Persongalleri? {
        return try {
            pdlTjenesterKlient.hentPersongalleri(soeker)
        } catch (e: Exception) {
            logger.info("Persongalleriet ble hentet med feil, returnerer null i stedet")
            null
        }
    }
}
