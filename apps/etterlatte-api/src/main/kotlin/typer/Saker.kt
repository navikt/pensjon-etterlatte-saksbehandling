package no.nav.etterlatte.typer

data class Sak(val ident: String, val sakType: String, val id: Long)

data class Saker(val saker: List<Sak>)
