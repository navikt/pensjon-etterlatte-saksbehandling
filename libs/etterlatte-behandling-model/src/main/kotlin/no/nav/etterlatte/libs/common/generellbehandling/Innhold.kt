package no.nav.etterlatte.libs.common.generellbehandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class Innhold {
    @JsonTypeName("KRAVPAKKE_UTLAND")
    data class KravpakkeUtland(
        val landIsoKode: List<String>,
        val dokumenter: List<DokumentMedSendtDato>,
        val begrunnelse: String,
        val rinanummer: String,
    ) : Innhold()

    @JsonTypeName("ANNEN")
    data class Annen(
        val innhold: String,
    ) : Innhold()
}

data class DokumentMedSendtDato(
    val dokumenttype: String,
    val sendt: Boolean,
    val dato: LocalDate?,
)
