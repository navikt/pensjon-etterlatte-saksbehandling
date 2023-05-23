package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.behandling.SakType

data class Sak(val ident: String, val sakType: SakType, val id: Long, val enhet: String)
data class VedtakSak(val ident: String, val sakType: SakType, val id: Long)
data class Saker(val saker: List<Sak>)