package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

data class BehandlingsBehov(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val persongalleri: Persongalleri,
    val mottattDato: String,
)

class PersongalleriFeilException(
    message: String,
) : UgyldigForespoerselException(code = "PERSONGALLERI_MAA_VAERE_GYLDIG", detail = message)

data class NyBehandlingRequest(
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val mottattDato: String,
    val spraak: String,
    val kilde: Vedtaksloesning?,
    val pesysId: Long?,
    val enhet: String?,
    val foreldreloes: Boolean = false,
    val ufoere: Boolean = false,
) {
    fun validerPersongalleri() {
        if (!persongalleri.validerFoedselesnummere()) {
            throw PersongalleriFeilException("Persongalleriet har feil eller mangler i sine fødselsnummere")
        }
    }
}
