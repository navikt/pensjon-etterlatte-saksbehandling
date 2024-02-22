package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.vedtak.KlageVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.vedtaksvurdering.OpprettVedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakTilstandException
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.slf4j.LoggerFactory

class VedtakKlageService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakKlageService::class.java)

    fun opprettEllerOppdaterVedtakOmAvvisning(klageVedtakDto: KlageVedtakDto): Long {
        logger.info("Oppretter eller oppdaterer vedtak for klage=${klageVedtakDto.klageId}")
        val eksisterendeVedtak = repository.hentVedtak(klageVedtakDto.klageId)
        val vedtak =
            if (eksisterendeVedtak == null) {
                repository.opprettVedtak(
                    OpprettVedtak(
                        soeker = klageVedtakDto.soeker,
                        sakId = klageVedtakDto.sakId,
                        sakType = klageVedtakDto.sakType,
                        behandlingId = klageVedtakDto.klageId,
                        type = VedtakType.AVVIST_KLAGE,
                        innhold = VedtakInnhold.Klage(klageVedtakDto.klage),
                    ),
                )
            } else {
                // TODO Kandidat for å flytte til felles sjekk for alle vedtakstyper
                // TODO verifiser lovlig type-overgang
                verifiserGyldigVedtakStatus(
                    eksisterendeVedtak.status,
                    listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT),
                )
                repository.oppdaterVedtak(
                    eksisterendeVedtak.copy(
                        innhold =
                            VedtakInnhold.Klage(
                                klage = klageVedtakDto.klage,
                            ),
                    ),
                )
            }
        return vedtak.id
    }

    private fun verifiserGyldigVedtakStatus(
        gjeldendeStatus: VedtakStatus,
        forventetStatus: List<VedtakStatus>,
    ) {
        if (gjeldendeStatus !in forventetStatus) throw VedtakTilstandException(gjeldendeStatus, forventetStatus)
    }
}
