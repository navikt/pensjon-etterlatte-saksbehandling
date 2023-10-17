package no.nav.etterlatte.libs.common.generellbehandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class Innhold {
    @JsonTypeName("KRAVPAKKE_UTLAND")
    data class KravpakkeUtland(
        val landIsoKode: List<String>,
        val dokumenter: Dokumenter,
        val begrunnelse: String,
        val rinanummer: String,
    ) : Innhold()

    @JsonTypeName("ANNEN")
    data class Annen(
        val innhold: String,
    ) : Innhold()
}

data class Dokumenter(
    val p2100: DokumentMedSendtDato,
    val p5000: DokumentMedSendtDato,
    val p3000: DokumentMedSendtDato,
    val p4000: DokumentMedSendtDato,
    val p6000: DokumentMedSendtDato,
)

data class DokumentMedSendtDato(
    val sendt: Boolean,
    val dato: LocalDate?,
)
