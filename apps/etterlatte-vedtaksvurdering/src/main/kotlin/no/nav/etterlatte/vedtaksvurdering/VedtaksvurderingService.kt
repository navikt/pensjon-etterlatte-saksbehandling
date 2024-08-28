package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVedtak(vedtakId: Long): Vedtak? {
        logger.info("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)
    }

    fun hentVedtakMedBehandlingId(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }

    fun hentVedtakISak(sakId: SakId): List<Vedtak> = repository.hentVedtakForSak(sakId)
}
