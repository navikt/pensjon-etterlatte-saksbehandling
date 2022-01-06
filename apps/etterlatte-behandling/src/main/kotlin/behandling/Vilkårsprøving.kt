package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.node.ObjectNode

data class Vilkårsprøving (
    val opplysninger: List<String>,
    val resultat: ObjectNode,
    val ansvarlig: String
)

enum class VilkårsPrøvingResultat{INNVILGET, AVSLAG, OPPHOER}