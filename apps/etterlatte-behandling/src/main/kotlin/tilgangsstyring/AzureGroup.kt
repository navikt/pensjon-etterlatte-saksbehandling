package no.nav.etterlatte.tilgangsstyring

import no.nav.etterlatte.libs.common.EnvEnum

enum class AzureGroup(
    val envKey: AzureKey,
) {
    ATTESTANT_GJENNY(AzureKey.AZUREAD_ATTESTANT_GJENNY_GROUPID),
    STRENGT_FORTROLIG(AzureKey.AZUREAD_STRENGT_FORTROLIG_GROUPID),
    FORTROLIG(AzureKey.AZUREAD_FORTROLIG_GROUPID),
    EGEN_ANSATT(AzureKey.AZUREAD_EGEN_ANSATT_GROUPID),
}

enum class AzureKey : EnvEnum {
    AZUREAD_ATTESTANT_GJENNY_GROUPID,
    AZUREAD_STRENGT_FORTROLIG_GROUPID,
    AZUREAD_FORTROLIG_GROUPID,
    AZUREAD_EGEN_ANSATT_GROUPID,
    ;

    override fun key() = name
}
