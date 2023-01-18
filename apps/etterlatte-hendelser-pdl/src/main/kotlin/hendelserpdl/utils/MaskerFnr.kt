package no.nav.etterlatte.hendelserpdl.utils

fun String.maskerFnr(): String = this.substring(0..5) + "xxxxx"