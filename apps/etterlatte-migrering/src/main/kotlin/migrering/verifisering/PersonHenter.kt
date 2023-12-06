package no.nav.etterlatte.migrering.verifisering

import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.migrering.start.MigreringFeatureToggle
import org.slf4j.LoggerFactory

internal class PersonHenter(
    private val pdlKlient: PDLKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val sikkerlogg = sikkerlogger()
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal fun hentPerson(
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
