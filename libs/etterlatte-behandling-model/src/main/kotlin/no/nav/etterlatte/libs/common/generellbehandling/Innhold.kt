package no.nav.etterlatte.libs.common.generellbehandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class Innhold {
    @JsonTypeName("UTLAND")
    data class Utland(
        val landIsoKode: String,
        val dokumenter: Dokumenter,
        val begrunnelse: String,
        val tilknyttetBehandling: String? = null,
    ) : Innhold()

    @JsonTypeName("ANNEN")
    data class Annen(
        val innhold: String,
    ) : Innhold()
}

data class Dokumenter(
    val P2100: Boolean,
    val P5000: Boolean,
    val P3000: Boolean,
)
