package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakTilbakekrevingService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakTilbakekrevingService::class.java)

    fun opprettEllerOppdaterVedtak(tilbakekrevingVedtakData: TilbakekrevingVedtakDto): Long {
        logger.info("Oppretter eller oppdaterer vedtak for tilbakekreving=$tilbakekrevingVedtakData.tilbakekrevingId")
        val eksisterendeVedtak = repository.hentVedtak(tilbakekrevingVedtakData.tilbakekrevingId)
        val vedtak =
            if (eksisterendeVedtak == null) {
                repository.opprettVedtak(
                    OpprettVedtak(
                        soeker = tilbakekrevingVedtakData.soeker,
                        sakId = tilbakekrevingVedtakData.sakId,
                        sakType = tilbakekrevingVedtakData.sakType,
                        behandlingId = tilbakekrevingVedtakData.tilbakekrevingId,
                        type = VedtakType.TILBAKEKREVING,
                        innhold = VedtakTilbakekrevingInnhold(tilbakekrevingVedtakData.tilbakekreving),
                    ),
                )
            } else {
                verifiserGyldigVedtakStatus(
                    eksisterendeVedtak.status,
                    listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT),
                )
                repository.oppdaterVedtak(
                    eksisterendeVedtak.copy(
                        innhold =
                            VedtakTilbakekrevingInnhold(
                                tilbakekreving = tilbakekrevingVedtakData.tilbakekreving,
                            ),
                    ),
                )
            }
        return vedtak.id
    }

    fun fattVedtak(tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto): Long {
        logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingVedtakData.tilbakekrevingId")
        verifiserGyldigVedtakStatus(tilbakekrevingVedtakData.tilbakekrevingId, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        return repository.fattVedtak(
            tilbakekrevingVedtakData.tilbakekrevingId,
            VedtakFattet(
                ansvarligSaksbehandler = tilbakekrevingVedtakData.saksbehandler,
                ansvarligEnhet = tilbakekrevingVedtakData.enhet,
                // Blir ikke brukt fordi egen now() brukes i db..
                tidspunkt = Tidspunkt.now(),
            ),
        ).id
    }

    fun attesterVedtak(tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto): TilbakekrevingVedtakLagretDto {
        logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingVedtakData.tilbakekrevingId")
        verifiserGyldigVedtakStatus(tilbakekrevingVedtakData.tilbakekrevingId, listOf(VedtakStatus.FATTET_VEDTAK))
        val vedtak =
            repository.attesterVedtak(
                tilbakekrevingVedtakData.tilbakekrevingId,
                Attestasjon(
                    attestant = tilbakekrevingVedtakData.saksbehandler,
                    attesterendeEnhet = tilbakekrevingVedtakData.enhet,
                    // Blir ikke brukt for egen now() brukes i db.
                    tidspunkt = Tidspunkt.now(),
                ),
            )
        return requireNotNull(vedtak.vedtakFattet).let {
            TilbakekrevingVedtakLagretDto(
                id = vedtak.id,
                fattetAv = it.ansvarligSaksbehandler,
                enhet = it.ansvarligEnhet,
                dato = it.tidspunkt.toLocalDate(),
            )
        }
    }

    fun underkjennVedtak(tilbakekrevingId: UUID): Long {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        verifiserGyldigVedtakStatus(tilbakekrevingId, listOf(VedtakStatus.FATTET_VEDTAK))
        return repository.underkjennVedtak(tilbakekrevingId).id
    }

    private fun verifiserGyldigVedtakStatus(
        tilbakekrevingId: UUID,
        forventetStatus: List<VedtakStatus>,
    ) {
        repository.hentVedtak(tilbakekrevingId)?.let {
            verifiserGyldigVedtakStatus(it.status, forventetStatus)
        } ?: throw NotFoundException("Fant ikke vedtak med tilbakekrevingsId=$tilbakekrevingId")
    }

    private fun verifiserGyldigVedtakStatus(
        gjeldendeStatus: VedtakStatus,
        forventetStatus: List<VedtakStatus>,
    ) {
        if (gjeldendeStatus !in forventetStatus) throw VedtakTilstandException(gjeldendeStatus, forventetStatus)
    }
}
