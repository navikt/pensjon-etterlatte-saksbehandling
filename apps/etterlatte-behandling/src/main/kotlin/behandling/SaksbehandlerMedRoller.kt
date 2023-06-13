package no.nav.etterlatte

import no.nav.etterlatte.config.AzureGroup
import no.nav.etterlatte.token.Bruker

data class SaksbehandlerMedRoller(val saksbehandler: Bruker) {
    fun harRolleStrengtFortrolig(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        saksbehandler.harRolle(saksbehandlerGroupIdsByKey, AzureGroup.STRENGT_FORTROLIG)

    fun harRolleFortrolig(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        saksbehandler.harRolle(saksbehandlerGroupIdsByKey, AzureGroup.FORTROLIG)

    fun harRolleEgenAnsatt(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        saksbehandler.harRolle(saksbehandlerGroupIdsByKey, AzureGroup.EGEN_ANSATT)
}