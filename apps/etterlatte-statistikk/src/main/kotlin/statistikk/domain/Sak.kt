package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

enum class BehandlingMetode {
    MANUELL,
    TOTRINN,
    AUTOMATISK,
    AUTOMATISK_REGULERING,
}

enum class SakUtland {
    NASJONAL,
}

enum class SoeknadFormat {
    DIGITAL,
}

enum class BehandlingResultat {
    INNVILGELSE,
    AVBRUTT,
    OPPHOER,
}

enum class SakYtelsesgruppe {
    EN_AVDOED_FORELDER,
    FORELDRELOES,
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
    val sakYtelsesgruppe: SakYtelsesgruppe?,
    val avdoedeForeldre: List<String>?,
    val revurderingAarsak: String?,
    val vedtakLoependeFom: LocalDate?,
    val vedtakLoependeTom: LocalDate?,
    val beregning: Beregning?,
    val avkorting: Avkorting?,
)
