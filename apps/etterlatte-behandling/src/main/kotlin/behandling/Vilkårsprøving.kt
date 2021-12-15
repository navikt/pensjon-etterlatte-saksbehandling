package no.nav.etterlatte.behandling

data class Vilkårsprøving (
    val opplysninger: List<String>,
    val resultat: VilkårsPrøvingResultat,
    val ansvarlig: String
)

enum class VilkårsPrøvingResultat{INNVILGET, AVSLAG, OPPHOER}