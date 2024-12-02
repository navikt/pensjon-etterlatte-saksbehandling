package no.nav.etterlatte.behandling.aktivitetsplikt.vurdering

data class AktivitetspliktAktivitetsgradOgUnntak(
    val aktivitetsgrad: LagreAktivitetspliktAktivitetsgrad,
    val unntak: LagreAktivitetspliktUnntak? = null,
)
