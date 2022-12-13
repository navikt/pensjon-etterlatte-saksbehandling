package no.nav.etterlatte

import no.nav.etterlatte.vilkaarsvurdering.config.ApplicationBuilder

fun main() {
    val application = ApplicationBuilder()
    application.migrerVilkaarsvurdering()
    application.start()
}