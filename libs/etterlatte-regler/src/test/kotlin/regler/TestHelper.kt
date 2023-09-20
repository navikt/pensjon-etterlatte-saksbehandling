package no.nav.etterlatte.libs.regler

import java.time.Instant

data class KildeSaksbehandler(
    val ident: String,
    val tidspunkt: Instant = Instant.now(),
    val type: String = "saksbehandler",
)

val saksbehandler = KildeSaksbehandler("Z12345")
val regelReferanse = RegelReferanse("REGEL-ID-1")
