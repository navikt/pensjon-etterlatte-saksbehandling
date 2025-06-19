package no.nav.etterlatte.libs.tidshendelser

import no.nav.etterlatte.libs.common.behandling.SakType

enum class JobbType(
    val beskrivelse: String,
    val kategori: JobbKategori,
    val sakType: SakType?,
) {
    OP_BP_FYLT_18("Oppfølging barnepensjon ved fylt 18 år", JobbKategori.OPPFOELGING_BP_FYLT_18, SakType.BARNEPENSJON),
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
        "Infobrev om aktivitetsplikt ved 6 måneder",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_6MND(
        "Vurdering av aktivitetsplikt ved 6 måneder",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_10MND(
        "Infobrev om aktivitetsplikt ved 12 måneder",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_12MND(
        "Vurdering av aktivitetsplikt ved 12 måneder",
        JobbKategori.OMS_DOEDSDATO,
        SakType.OMSTILLINGSSTOENAD,
    ),
    OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK(
        "Infobrev etter 6 måneder - varig unntak",
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
    AARLIG_INNTEKTSJUSTERING(
        "Årlig inntektsjustering - varselbrev og utkast til vedtak",
        JobbKategori.AARLIG_INNTEKTSJUSTERING,
        sakType = SakType.OMSTILLINGSSTOENAD,
    ),
}

enum class JobbKategori {
    ALDERSOVERGANG,
    OMS_DOEDSDATO,
    REGULERING,
    AARLIG_INNTEKTSJUSTERING,
    OPPFOELGING_BP_FYLT_18,
}
