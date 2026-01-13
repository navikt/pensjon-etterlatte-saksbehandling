package no.nav.etterlatte.libs.common.behandling.etteroppgjoer

enum class FilterVerdi(
    val filterEn: Boolean,
    val filterTo: Boolean,
) {
    FALSE(false, false),
    TRUE(true, true),
    DONT_CARE(false, true),
}

enum class EtteroppgjoerFilter(
    val harSanksjon: FilterVerdi,
    val harInstitusjonsopphold: FilterVerdi,
    val harOpphoer: FilterVerdi,
    val harAdressebeskyttelseEllerSkjermet: FilterVerdi,
    val harAktivitetskrav: FilterVerdi,
    val harBosattUtland: FilterVerdi,
    val harUtlandstilsnitt: FilterVerdi,
    val harOverstyrtBeregning: FilterVerdi,
) {
    ENKEL(
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
        FilterVerdi.FALSE,
    ),
    MED_AKTIVITET_OG_SKJERMET(
        harSanksjon = FilterVerdi.FALSE,
        harInstitusjonsopphold = FilterVerdi.FALSE,
        harOpphoer = FilterVerdi.FALSE,
        harAdressebeskyttelseEllerSkjermet = FilterVerdi.DONT_CARE,
        harAktivitetskrav = FilterVerdi.DONT_CARE,
        harBosattUtland = FilterVerdi.FALSE,
        harUtlandstilsnitt = FilterVerdi.FALSE,
        harOverstyrtBeregning = FilterVerdi.FALSE,
    ),
    MED_AKTIVITET_SKJERMET_OG_UTLANDSTILSNITT(
        harSanksjon = FilterVerdi.FALSE,
        harInstitusjonsopphold = FilterVerdi.FALSE,
        harOpphoer = FilterVerdi.FALSE,
        harAdressebeskyttelseEllerSkjermet = FilterVerdi.DONT_CARE,
        harAktivitetskrav = FilterVerdi.DONT_CARE,
        harBosattUtland = FilterVerdi.FALSE,
        harUtlandstilsnitt = FilterVerdi.DONT_CARE,
        harOverstyrtBeregning = FilterVerdi.FALSE,
    ),
    MED_AKTIVITET_SKJERMET_OG_UTLANDSTILSNITT_OG_OPPHOER(
        harSanksjon = FilterVerdi.FALSE,
        harInstitusjonsopphold = FilterVerdi.FALSE,
        harOpphoer = FilterVerdi.DONT_CARE,
        harAdressebeskyttelseEllerSkjermet = FilterVerdi.DONT_CARE,
        harAktivitetskrav = FilterVerdi.DONT_CARE,
        harBosattUtland = FilterVerdi.FALSE,
        harUtlandstilsnitt = FilterVerdi.DONT_CARE,
        harOverstyrtBeregning = FilterVerdi.FALSE,
    ),

    MED_AKTIVITET_SKJERMET_OG_UTLANDSTILSNITT_OG_OPPHOER_OG_BOSATT_UTLAND(
        harSanksjon = FilterVerdi.FALSE,
        harInstitusjonsopphold = FilterVerdi.FALSE,
        harOpphoer = FilterVerdi.DONT_CARE,
        harAdressebeskyttelseEllerSkjermet = FilterVerdi.DONT_CARE,
        harAktivitetskrav = FilterVerdi.DONT_CARE,
        harBosattUtland = FilterVerdi.DONT_CARE,
        harUtlandstilsnitt = FilterVerdi.DONT_CARE,
        harOverstyrtBeregning = FilterVerdi.FALSE,
    ),
}
