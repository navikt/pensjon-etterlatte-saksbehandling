package no.nav.etterlatte.libs.common.person

fun String.maskerFnr(): String = if (this.length >= 6) this.substring(0..5) + "*****" else "*****"
