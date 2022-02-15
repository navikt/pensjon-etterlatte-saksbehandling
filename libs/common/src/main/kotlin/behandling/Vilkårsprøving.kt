package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.databind.node.ObjectNode

data class Vilkårsprøving (
    val resultat: ObjectNode
    )

enum class VilkårsPrøvingResultat{INNVILGET, AVSLAG, OPPHOER}