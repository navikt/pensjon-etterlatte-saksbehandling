package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakTilbakekrevingService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakTilbakekrevingService::class.java)

    fun opprettEllerOppdaterVedtak(dto: TilbakekrevingVedtakDto): Long {
        logger.info("Oppretter eller oppdaterer vedtak for tilbakekreving=$dto.tilbakekrevingId")
        val eksisterendeVedtak = repository.hentVedtak(dto.tilbakekrevingId)
        // TODO sjekk status..
        val vedtak =
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
        return vedtak.id
    }

    fun fattVedtak(dto: TilbakekrevingFattEllerAttesterVedtakDto): Long {
        logger.info("Fatter vedtak for tilbakekreving=$dto.tilbakekrevingId")
        // TODO sjekk status..
        return repository.fattVedtak(
            dto.tilbakekrevingId,
            VedtakFattet(
                ansvarligSaksbehandler = dto.saksbehandler,
                ansvarligEnhet = dto.enhet,
                tidspunkt = Tidspunkt.now(), // Blir ikke brukt fordi egen now() brukes i db..
            ),
        ).id
    }

    fun attesterVedtak(dto: TilbakekrevingFattEllerAttesterVedtakDto): Long {
        logger.info("Attesterer vedtak for tilbakekreving=$dto.tilbakekrevingId")
        // TODO sjekk status..
        return repository.attesterVedtak(
            dto.tilbakekrevingId,
            Attestasjon(
                attestant = dto.saksbehandler,
                attesterendeEnhet = dto.enhet,
                tidspunkt = Tidspunkt.now(), // Blir ikke brukt for egen now() brueks i db..
            ),
        ).id
    }

    fun underkjennVedtak(tilbakekrevingId: UUID): Long {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        // TODO sjekk status..
        return repository.underkjennVedtak(tilbakekrevingId).id
    }
}
