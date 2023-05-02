package no.nav.etterlatte.libs.common.behandling

enum class RevurderingAarsak(val kanBrukes: Boolean) {
    REGULERING(true),
    GRUNNBELOEP(false),
    ANSVARLIGE_FORELDRE(false),
    UTLAND(false),
    BARN(false),
    DOEDSDATO(false),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(false)
}