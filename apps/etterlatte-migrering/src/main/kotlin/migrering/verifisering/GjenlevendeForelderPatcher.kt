package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import org.slf4j.LoggerFactory

internal class GjenlevendeForelderPatcher(val pdlKlient: PDLKlient) {
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
}
