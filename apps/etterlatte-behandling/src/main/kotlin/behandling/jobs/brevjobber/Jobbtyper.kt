package no.nav.etterlatte.behandling.jobs.brevjobber

import no.nav.etterlatte.libs.common.behandling.SakType

enum class JobbType(
    val beskrivelse: String,
    val kategori: JobbKategori,
    val sakType: SakType?,
) {
    TREKKPLIKT_2025("Trekkplikt 2025, du taper penger på dette", JobbKategori.TREKKPLIKT, SakType.OMSTILLINGSSTOENAD),
}

enum class JobbKategori {
    TREKKPLIKT,
}
