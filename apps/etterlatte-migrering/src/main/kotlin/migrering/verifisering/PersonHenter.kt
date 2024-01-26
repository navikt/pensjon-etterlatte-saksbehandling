package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle

internal class PersonHenter(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val sikkerlogg = sikkerlogger()

    internal fun hentPerson(
        rolle: PersonRolle,
        folkeregisteridentifikator: Folkeregisteridentifikator,
    ): Result<PersonDTO?> =
        try {
            Result.success(pdlTjenesterKlient.hentPerson(rolle, folkeregisteridentifikator))
        } catch (e: Exception) {
            sikkerlogg.warn("Fant ikke person $folkeregisteridentifikator med rolle $rolle i PDL", e)
            Result.failure(FinsIkkeIPDL(rolle, folkeregisteridentifikator))
        }
}
