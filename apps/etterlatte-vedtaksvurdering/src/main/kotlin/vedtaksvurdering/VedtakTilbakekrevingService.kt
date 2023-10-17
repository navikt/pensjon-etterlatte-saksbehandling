package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattetVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakTilbakekrevingService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakTilbakekrevingService::class.java)

    fun lagreVedtak(dto: TilbakekrevingFattetVedtakDto): Long {
        logger.info("Fatter vedtak for tilbakekreving=$dto.tilbakekrevingId")
        val eksisterendeVedtak = repository.hentVedtak(dto.tilbakekrevingId)
        if (eksisterendeVedtak == null) {
            repository.opprettVedtak(
                OpprettVedtak(
                    soeker = dto.soeker,
                    sakId = dto.sakId,
                    sakType = dto.sakType,
                    behandlingId = dto.tilbakekrevingId,
                    type = VedtakType.TILBAKEKREVING,
                    innhold = VedtakTilbakekrevingInnhold(dto.tilbakekreving),
                ),
            )
        } else {
            repository.oppdaterVedtak(
                eksisterendeVedtak.copy(
                    innhold =
                        VedtakTilbakekrevingInnhold(
                            tilbakekreving = dto.tilbakekreving,
                        ),
                ),
            )
        }
        return repository.fattVedtak(
            dto.tilbakekrevingId,
            VedtakFattet(
                ansvarligSaksbehandler = dto.ansvarligSaksbehandler,
                ansvarligEnhet = dto.ansvarligEnhet,
                tidspunkt = Tidspunkt.now(), // Blir ikke brukt fordi egen now() brukes i db..
            ),
        ).id
    }

    fun attesterVedtak(dto: TilbakekrevingAttesterVedtakDto): Long {
        logger.info("Attesterer vedtak for tilbakekreving=$dto.tilbakekrevingId")
        return repository.attesterVedtak(
            dto.tilbakekrevingId,
            Attestasjon(
                attestant = dto.attestant,
                attesterendeEnhet = dto.attesterendeEnhet,
                tidspunkt = Tidspunkt.now(), // Blir ikke brukt for egen now() brueks i db..
            ),
        ).id
    }

    fun underkjennVedtak(tilbakekrevingId: UUID): Long {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        return repository.underkjennVedtak(tilbakekrevingId).id
    }
}
