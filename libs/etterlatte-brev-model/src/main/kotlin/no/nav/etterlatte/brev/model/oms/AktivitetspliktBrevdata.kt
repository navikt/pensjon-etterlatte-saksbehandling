package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class AktivitetspliktInformasjon4MndBrevdataData(
    val aktivitetsgrad: Aktivitetsgrad,
    val utbetaling: Boolean,
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
    val halvtGrunnbeloep: Int,
)

data class AktivitetspliktInformasjon4MndBrevdata(
    override val data: AktivitetspliktInformasjon4MndBrevdataData,
) : BrevDataRedigerbar

data class AktivitetspliktInformasjon10mndBrevdataData(
    val aktivitetsgrad: Aktivitetsgrad,
    val utbetaling: Boolean,
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
    val halvtGrunnbeloep: Int,
)

data class AktivitetspliktInformasjon10mndBrevdata(
    override val data: AktivitetspliktInformasjon10mndBrevdataData,
) : BrevDataRedigerbar

data class AktivitetspliktInformasjon6MndBrevdataData(
    val redusertEtterInntekt: Boolean,
    val nasjonalEllerUtland: NasjonalEllerUtland,
    val halvtGrunnbeloep: Int,
)

data class AktivitetspliktInformasjon6MndBrevdata(
    override val data: AktivitetspliktInformasjon6MndBrevdataData,
) : BrevDataRedigerbar

enum class Aktivitetsgrad { IKKE_I_AKTIVITET, UNDER_50_PROSENT, OVER_50_PROSENT, AKKURAT_100_PROSENT }

enum class NasjonalEllerUtland { NASJONAL, UTLAND }
