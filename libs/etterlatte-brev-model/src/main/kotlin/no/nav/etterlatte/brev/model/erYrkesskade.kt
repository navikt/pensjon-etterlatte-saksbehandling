package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto

fun TrygdetidDto.erYrkesskade() = beregnetTrygdetid?.resultat?.yrkesskade ?: false
