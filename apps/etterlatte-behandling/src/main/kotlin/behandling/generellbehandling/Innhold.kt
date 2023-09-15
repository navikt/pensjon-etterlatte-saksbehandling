package no.nav.etterlatte.behandling.generellbehandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class Innhold {
    @JsonTypeName("UTLAND")
    data class Utland(
        val sed: String,
        val tilknyttetBehandling: UUID
    ) : Innhold()

    @JsonTypeName("ANNEN")
    data class Annen(
        val innhold: String
    ) : Innhold()
}