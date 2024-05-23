package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
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
    UTLANDSTILSNITT,
    BOSATT_UTLAND,
    ;

    companion object {
        fun fraUtlandstilknytning(utlandstilknytning: Utlandstilknytning?): SakUtland? {
            if (utlandstilknytning == null) {
                return null
            }

            return fraUtlandstilknytningType(utlandstilknytning.type)
        }

        fun fraUtlandstilknytningType(utlandstilknytningType: UtlandstilknytningType): SakUtland {
            return when (utlandstilknytningType) {
                UtlandstilknytningType.NASJONAL -> NASJONAL
                UtlandstilknytningType.BOSATT_UTLAND -> BOSATT_UTLAND
                UtlandstilknytningType.UTLANDSTILSNITT -> UTLANDSTILSNITT
            }
        }
    }
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
    val referanseId: UUID,
    val sakId: Long,
    val mottattTidspunkt: Tidspunkt,
    val registrertTidspunkt: Tidspunkt,
    val ferdigbehandletTidspunkt: Tidspunkt?,
    val vedtakTidspunkt: Tidspunkt?,
    val type: String,
    val status: String?,
    val resultat: String?,
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
    val kilde: Vedtaksloesning,
    val pesysId: Long?,
    val relatertTil: String?,
    val paaVentAarsak: PaaVentAarsak? = null,
)
