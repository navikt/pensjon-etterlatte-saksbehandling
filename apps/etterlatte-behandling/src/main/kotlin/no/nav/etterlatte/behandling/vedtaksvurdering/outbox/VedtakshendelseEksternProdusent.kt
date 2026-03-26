package no.nav.etterlatte.behandling.vedtaksvurdering.outbox

import no.nav.etterlatte.kafka.KafkaProdusent
import no.nav.etterlatte.libs.common.toJson
import java.util.UUID

class VedtakshendelseEksternProdusent(
    private val rapid: KafkaProdusent<String, String>,
) {
    internal fun publiserHendelse(
        id: UUID,
        vedtakshendelse: Vedtakshendelse,
    ) {
        rapid.publiser(
            noekkel = id.toString(),
            verdi = vedtakshendelse.toJson(),
        )
    }
}
