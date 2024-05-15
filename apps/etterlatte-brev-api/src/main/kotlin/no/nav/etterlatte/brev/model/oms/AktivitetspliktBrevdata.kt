package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevDataRedigerbar

data class AktivitetspliktBrevdata(
    val aktivitetsgrad: Aktivitetsgrad,
    val utbetaling: Boolean,
) : BrevDataRedigerbar

enum class Aktivitetsgrad { IKKE_I_AKTIVITET, UNDER_50_PROSENT, OVER_50_PROSENT }
