package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

data class BehandlingsBehov(
    val sakId: Long,
    val persongalleri: Persongalleri,
    val mottattDato: String,
)

data class NyBehandlingRequest(
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val mottattDato: String,
    val spraak: String,
    val kilde: Vedtaksloesning?,
    val pesysId: Long?,
    val enhet: String?,
    val foreldreloes: Boolean = false,
    val gradering: AdressebeskyttelseGradering? = AdressebeskyttelseGradering.UGRADERT,
)
