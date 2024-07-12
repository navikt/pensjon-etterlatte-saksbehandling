package no.nav.etterlatte.tilgangsstyring

import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Saksbehandler

data class SaksbehandlerMedRoller(
    val saksbehandler: Saksbehandler,
    val saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
) {
    fun harRolle(rolle: AzureGroup): Boolean {
        val claims = saksbehandler.getClaims()
        return saksbehandlerGroupIdsByKey[rolle]
            ?.let { return claims?.containsClaim(Claims.groups.name, it) ?: false }
            ?: false
    }

    fun harRolleSaksbehandler() = harRolle(AzureGroup.SAKSBEHANDLER)

    fun harRolleAttestant() = (harRolle(AzureGroup.ATTESTANT) || harRolle(AzureGroup.ATTESTANT_GJENNY))

    fun harRolleStrengtFortrolig() = harRolle(AzureGroup.STRENGT_FORTROLIG)

    fun harRolleFortrolig() = harRolle(AzureGroup.FORTROLIG)

    fun harRolleEgenAnsatt() = harRolle(AzureGroup.EGEN_ANSATT)
}
