package no.nav.etterlatte.behandling.utland

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
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

data class SluttbehandlingBehandlinginfo(
    val landMedDokumenter: List<LandMedDokumenter>,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class SluttbehandlingBehandlinginfoRequest(
    val landMedDokumenter: List<LandMedDokumenter>,
) {
    fun toDomain(kilde: Grunnlagsopplysning.Kilde) = SluttbehandlingBehandlinginfo(landMedDokumenter, kilde)
}
