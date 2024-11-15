package no.nav.etterlatte.libs.tidshendelser

import no.nav.etterlatte.libs.common.behandling.SakType

enum class JobbType(
    val beskrivelse: String,
    val kategori: JobbKategori,
    val sakType: SakType?,
) {
    AO_BP20("Aldersovergang barnepensjon ved 20 år", JobbKategori.ALDERSOVERGANG, SakType.BARNEPENSJON),
    AO_BP21("Aldersovergang barnepensjon ved 21 år", JobbKategori.ALDERSOVERGANG, SakType.BARNEPENSJON),
    AO_OMS67("Aldersovergang omstillingsstønad ved 67 år", JobbKategori.ALDERSOVERGANG, SakType.OMSTILLINGSSTOENAD),
    OMS_DOED_3AAR(
        "Omstillingsstønad opphør 3 år etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_5AAR(
        "Omstillingsstønad opphør 5 år etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_4MND(
        "Omstillingsstønad varselbrev om aktivitetsplikt 4 mnd etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_6MND(
        "Omstillingsstønad vurdering av aktivitetsplikt 6 mnd etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_10MND(
        "Infobrev om aktivitetsplikt 10 mnd etter dødsdato",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK(
        "Omstillingsstønad informasjon om aktivitetsplikt ved 6 mnd etter dødsdato - varig unntak",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    REGULERING(
        "Starter regulering",
        JobbKategori.REGULERING,
        sakType = null,
    ),
    FINN_SAKER_TIL_REGULERING(
        "Finner saker som skal reguleres",
        JobbKategori.REGULERING,
        sakType = null,
    ),
}

enum class JobbKategori {
    ALDERSOVERGANG,
    OMS_DOEDSDATO,
    REGULERING,
    AARLIG_INNTEKTSJUSTERING,
}
