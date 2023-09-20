package no.nav.etterlatte.libs.common.logging

fun samleExceptions(exceptions: List<Exception>): Exception {
    val siste = exceptions.last()
    exceptions.dropLast(1).reversed().forEach { siste.addSuppressed(it) }
    return siste
}
