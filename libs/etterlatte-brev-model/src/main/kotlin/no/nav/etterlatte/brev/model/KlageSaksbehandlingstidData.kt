package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

data class KlageSaksbehandlingstidDataData(
    val sakType: SakType,
    val borIUtlandet: Boolean,
    val datoMottatKlage: LocalDate,
    val datoForVedtak: LocalDate,
)

data class KlageSaksbehandlingstidData(
    override val data: KlageSaksbehandlingstidDataData,
) : BrevDataRedigerbar {
    companion object {
        fun fra (
            sakType: SakType,
            borIUtlandet: Boolean,
            datoMottatKlage: LocalDate,
            datoForVedtak: LocalDate,
        ) =
            KlageSaksbehandlingstidData(
                data = KlageSaksbehandlingstidDataData(
                    sakType = sakType,
                    borIUtlandet = borIUtlandet,
                    datoMottatKlage = datoMottatKlage,
                    datoForVedtak = datoForVedtak,
                )
            )

    }
}
