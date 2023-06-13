package no.nav.etterlatte

import no.nav.etterlatte.config.AzureGroup
import no.nav.etterlatte.token.Bruker

data class SaksbehandlerMedRoller(val saksbehandler: Bruker) {

    fun harRolle(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>, rolle: AzureGroup) =
        saksbehandler.harRolle(saksbehandlerGroupIdsByKey[rolle])

    fun harRolleStrengtFortrolig(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        harRolle(saksbehandlerGroupIdsByKey, AzureGroup.STRENGT_FORTROLIG)

    fun harRolleFortrolig(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        harRolle(saksbehandlerGroupIdsByKey, AzureGroup.FORTROLIG)

    fun harRolleEgenAnsatt(saksbehandlerGroupIdsByKey: Map<AzureGroup, String>) =
        harRolle(saksbehandlerGroupIdsByKey, AzureGroup.EGEN_ANSATT)
}