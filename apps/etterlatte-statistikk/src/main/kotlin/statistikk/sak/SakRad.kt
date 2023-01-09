package no.nav.etterlatte.statistikk.sak

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.*

enum class BehandlingMetode {
    MANUELL
}

enum class SakUtland {
    NASJONAL
}

enum class SoeknadFormat {
    DIGITAL
}

enum class BehandlingResultat {
    VEDTAK, AVBRUTT, OPPHOER
}

data class SakRad(
    val id: Long,
    val behandlingId: UUID,
    val sakId: Long,
    val mottattTidspunkt: Tidspunkt,
    val registrertTidspunkt: Tidspunkt,
    val ferdigbehandletTidspunkt: Tidspunkt?,
    val vedtakTidspunkt: Tidspunkt?,
    val behandlingType: BehandlingType,
    val behandlingStatus: String?,
    val behandlingResultat: BehandlingResultat?,
    val resultatBegrunnelse: String?,
    val saksbehandler: String?,
    val ansvarligEnhet: String?,
    val soeknadFormat: SoeknadFormat?,
    val sakUtland: SakUtland?,
    val behandlingMetode: BehandlingMetode?,
    val opprettetAv: String?,
    val ansvarligBeslutter: String?,
    val aktorId: String,
    val datoFoersteUtbetaling: LocalDate?,
    val tekniskTid: Tidspunkt,
    val sakYtelse: String,
    val vedtakLoependeFom: LocalDate?,
    val vedtakLoependeTom: LocalDate?
)