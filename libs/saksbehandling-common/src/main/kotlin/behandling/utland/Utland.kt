package no.nav.etterlatte.behandling.utland

import java.time.LocalDate

data class LandMedDokumenter(
    val landIsoKode: String,
    val dokumenter: List<MottattDokument>,
)

data class MottattDokument(
    val dokumenttype: String,
    val dato: LocalDate,
    val kommentar: String?,
)
