package no.nav.etterlatte.vedtaksvurdering.outbox

import net.logstash.logback.marker.Markers
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
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
        val markers =
            Markers.appendEntries(
                mapOf(
                    "behandlingId" to vedtak.behandlingId,
                    "outboxId" to item.id,
                ),
            )

        if (vedtak.type.tilgjengeligEksternt) {
            publiserEksternHendelse(
                item.id,
                Vedtakshendelse(
                    ident = vedtak.soeker.value,
                    sakstype = vedtak.sakType.toEksternApi(),
                    type = vedtak.typeToEksternApi(),
                    vedtakId = vedtak.id,
                    vedtaksdato = vedtak.attestasjon?.tidspunkt?.toLocalDate(),
                    virkningFom =
                        (vedtak.innhold as? VedtakInnhold.Behandling)?.virkningstidspunkt?.atDay(1)
                            ?: throw InternfeilException(
                                "Har et vedtak som skal være internt tilgjengelig, " +
                                    "med vedtaksinnhold som ikke er vanlig behandling. Id=${vedtak.id} i sak=${vedtak.sakId}",
                            ),
                ).toJson(),
            )
            logger.info(markers, "Publisert vedtakshendelse for vedtak=${vedtak.id}")
        } else {
            logger.info(markers, "Støtter ikke vedtakshendelse for vedtak=${vedtak.id}, skipper")
        }
    }
}

internal fun VedtakInnhold.Behandling.isRegulering() = Revurderingaarsak.REGULERING == this.revurderingAarsak

internal fun Vedtak.typeToEksternApi(): String {
    if ((this.innhold as VedtakInnhold.Behandling).isRegulering()) {
        return "REGULERING"
    }
    return when (this.type) {
        VedtakType.AVSLAG -> "AVSLAG"
        VedtakType.ENDRING -> "ENDRING"
        VedtakType.INNVILGELSE -> "INNVILGELSE"
        VedtakType.OPPHOER -> "OPPHOER"
        else -> throw IllegalArgumentException("Støtter ikke vedtakstype $this")
    }
}

internal fun SakType.toEksternApi(): String =
    when (this) {
        SakType.BARNEPENSJON -> "BP"
        SakType.OMSTILLINGSSTOENAD -> "OMS"
    }

internal data class Vedtakshendelse(
    val ident: String,
    val sakstype: String,
    val type: String,
    val vedtakId: Long,
    val vedtaksdato: LocalDate?,
    val virkningFom: LocalDate,
)
