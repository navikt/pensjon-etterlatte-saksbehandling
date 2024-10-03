package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.libs.common.person.Verge

data class PersonerISak(
    val innsender: Innsender?,
    val soeker: Soeker,
    val avdoede: List<Avdoed>,
    val verge: Verge?,
) {
    fun soekerOgEventuellVerge(): SoekerOgEventuellVerge = SoekerOgEventuellVerge(soeker, verge)
}
