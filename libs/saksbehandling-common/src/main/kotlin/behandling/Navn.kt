package no.nav.etterlatte.libs.common.behandling

data class Navn(val fornavn: String, val mellomnavn: String? = null, val etternavn: String) {
    fun formaterNavn() = "$fornavn " + if (mellomnavn != null) { "$mellomnavn " } else { "" } + etternavn
}