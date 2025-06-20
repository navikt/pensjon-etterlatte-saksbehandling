package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.libs.common.sak.Sak

data class EtteroppgjoerBrevRequestData(
    val redigerbar: BrevRedigerbarInnholdData,
    val innhold: BrevFastInnholdData,
    val vedlegg: List<BrevVedleggRedigerbarNy>,
    val sak: Sak,
)
