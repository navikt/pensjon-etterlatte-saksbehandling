package no.nav.etterlatte.brev.navansatt

data class SaksbehandlerInfo(
    val ident: String,
    val navn: String, // active directory displayname
    val fornavn: String,
    val etternavn: String,
    val epost: String,
) {
    val fornavnEtternavn get() = "$fornavn $etternavn"
}
