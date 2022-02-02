package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.databind.node.ObjectNode

data class Vilkårsprøving (
    val opplysninger: List<String>,
    val resultat: ObjectNode,
    val ansvarlig: String
)

enum class VilkårsPrøvingResultat{INNVILGET, AVSLAG, OPPHOER}