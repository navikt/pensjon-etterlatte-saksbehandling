package no.nav.etterlatte.vedtaksvurdering.outbox

import net.logstash.logback.marker.Markers
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val publiserEksternHendelse: (UUID, String) -> Unit,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal fun run() {
        outboxRepository.hentUpubliserte().forEach {
            vedtaksvurderingService.hentVedtak(it.vedtakId)?.let { vedtak ->
                publiserVedtakshendelse(it, vedtak)
                outboxRepository.merkSomPublisert(it.id)
            }
        }
    }

    private fun publiserVedtakshendelse(
        item: OutboxItem,
        vedtak: Vedtak,
    ) {
        if (vedtak.innhold is VedtakInnhold.Behandling) {
            publiserEksternHendelse(
                item.id,
                Vedtakshendelse(
                    ident = vedtak.soeker.value,
                    sakstype = vedtak.sakType.toEksternApi(),
                    type = vedtak.type.toEksternApi(),
                    vedtakId = vedtak.id,
                    vedtaksdato = vedtak.attestasjon?.tidspunkt?.toLocalDate(),
                    virkningFom = vedtak.innhold.virkningstidspunkt.atDay(1),
                ).toJson(),
            )
        } else {
            val markers =
                Markers.appendEntries(
                    mapOf(
                        "behandlingId" to vedtak.behandlingId,
                        "outboxId" to item.id,
                    ),
                )

            logger.warn(markers, "Støtter ikke vedtakshendelse for vedtak=${vedtak.id}, skipper")
        }
    }
}

internal fun VedtakType.toEksternApi(): String {
    return when (this) {
        VedtakType.AVSLAG -> "AVSLAG"
        VedtakType.ENDRING -> "ENDRING"
        VedtakType.INNVILGELSE -> "INNVILGELSE"
        VedtakType.OPPHOER -> "OPPHOER"
        else -> throw IllegalArgumentException("Støtter ikke vedtakstype $this")
    }
}

internal fun SakType.toEksternApi(): String {
    return when (this) {
        SakType.BARNEPENSJON -> "BP"
        SakType.OMSTILLINGSSTOENAD -> "OMS"
    }
}

internal data class Vedtakshendelse(
    val ident: String,
    val sakstype: String,
    val type: String,
    val vedtakId: Long,
    val vedtaksdato: LocalDate?,
    val virkningFom: LocalDate,
)
