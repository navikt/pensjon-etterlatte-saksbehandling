package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class AktivitetspliktInformasjon4MndBrevdata(
    val aktivitetsgrad: Aktivitetsgrad,
    val utbetaling: Boolean,
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
) : BrevDataRedigerbar

data class AktivitetspliktInformasjon10mndBrevdata(
    val aktivitetsgrad: Aktivitetsgrad,
    val utbetaling: Boolean,
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
) : BrevDataRedigerbar

data class AktivitetspliktInformasjon6MndBrevdata(
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
) : BrevDataRedigerbar

enum class Aktivitetsgrad { IKKE_I_AKTIVITET, UNDER_50_PROSENT, OVER_50_PROSENT, UNDER_100_PROSENT, AKKURAT_100_PROSENT }

enum class NasjonalEllerUtland { NASJONAL, UTLAND }
