package no.nav.etterlatte.tilgangsstyring

import no.nav.etterlatte.token.Saksbehandler

data class SaksbehandlerMedRoller(
    val saksbehandler: Saksbehandler,
    val saksbehandlerGroupIdsByKey: Map<AzureGroup, String>,
) {
    fun harRolle(rolle: AzureGroup): Boolean {
        val claims = saksbehandler.getClaims()
        return saksbehandlerGroupIdsByKey[rolle]
            ?.let { return claims?.containsClaim("groups", it) ?: false }
            ?: false
    }

    fun harRolleSaksbehandler() = harRolle(AzureGroup.SAKSBEHANDLER)

    fun harRolleAttestant() = harRolle(AzureGroup.ATTESTANT)

    fun harRolleStrengtFortrolig() = harRolle(AzureGroup.STRENGT_FORTROLIG)

    fun harRolleFortrolig() = harRolle(AzureGroup.FORTROLIG)

    fun harRolleEgenAnsatt() = harRolle(AzureGroup.EGEN_ANSATT)

    fun harRolleNasjonalTilgang() = harRolle(AzureGroup.NASJONAL_MED_LOGG) || harRolle(AzureGroup.NASJONAL_UTEN_LOGG)
}
