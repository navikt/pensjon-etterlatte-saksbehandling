package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.VedtakSak
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class VedtakTilJournalfoering(
    val vedtakId: Long,
    val sak: VedtakSak,
    val behandlingId: UUID,
    val ansvarligEnhet: Enhetsnummer,
    val saksbehandler: String,
)
