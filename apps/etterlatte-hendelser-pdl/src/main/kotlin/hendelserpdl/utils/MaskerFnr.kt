package no.nav.etterlatte.hendelserpdl.utils

fun String.maskerFnr(): String = if (this.length >= 6) this.substring(0..5) + "xxxxx" else "xxxxx"