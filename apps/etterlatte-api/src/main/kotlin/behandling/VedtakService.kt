package no.nav.etterlatte.behandling

import no.nav.etterlatte.kafka.JsonMessage
import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey


data class AttesteringResult(val response: String)

class VedtakService(private val rapid: KafkaProdusent<String, String>) {

    fun fattVedtak(behandlingId: String, saksbehandler: String): AttesteringResult {
        rapid.publiser(behandlingId, JsonMessage.newMessage(
            mapOf(
                eventNameKey to "SAKSBEHANDLER:FATT_VEDTAK",
                "behandlingId" to behandlingId,
                "saksbehandler" to saksbehandler,

                )
        ).toJson())
        return AttesteringResult("Fattet")
    }

    fun attesterVedtak(behandlingId: String, saksbehandler: String): AttesteringResult {
        rapid.publiser(behandlingId, JsonMessage.newMessage(
            mapOf(
                eventNameKey to "SAKSBEHANDLER:ATTESTER_VEDTAK",
                "behandlingId" to behandlingId,
                "saksbehandler" to saksbehandler,

                )
        ).toJson())
        return AttesteringResult("Attestert")
    }

    fun underkjennVedtak(behandlingId: String,
        begrunnelse: String,
        kommentar: String,
        saksbehandler: String): AttesteringResult {
        rapid.publiser(behandlingId, JsonMessage.newMessage(
            mapOf(
                eventNameKey to "SAKSBEHANDLER:UNDERKJENN_VEDTAK",
                "behandlingId" to behandlingId,
                "saksbehandler" to saksbehandler,
                "valgtBegrunnelse" to begrunnelse,
                "kommentar" to kommentar,

                )
        ).toJson())
        return AttesteringResult("Underkjent")
    }

}
