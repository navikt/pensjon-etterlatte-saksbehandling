package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.libs.common.behandling.SakType
import java.time.LocalDate

data class KlageSaksbehandlingstidData(
    val sakType: SakType,
    val borIUtlandet: Boolean,
    val datoMottatKlage: LocalDate,
    val datoForVedtak: LocalDate,
) : BrevDataRedigerbar
