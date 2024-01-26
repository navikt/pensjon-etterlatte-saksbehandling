package no.nav.etterlatte.brev.adresse.navansatt

data class SaksbehandlerInfo(
    val ident: String,
    // active directory displayname
    val navn: String,
    val fornavn: String,
    val etternavn: String,
    val epost: String,
) {
    val fornavnEtternavn get() = "$fornavn $etternavn"
}
