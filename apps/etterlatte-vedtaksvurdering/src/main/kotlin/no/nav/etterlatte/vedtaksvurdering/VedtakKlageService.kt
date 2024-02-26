package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.OpprettVedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakTilstandException
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.slf4j.LoggerFactory

class VedtakKlageService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakKlageService::class.java)

    fun opprettEllerOppdaterVedtakOmAvvisning(klage: Klage): Long {
        logger.info("Oppretter eller oppdaterer vedtak for klage med id=${klage.id}")
        val eksisterendeVedtak = repository.hentVedtak(klage.id)
        val vedtak =
            if (eksisterendeVedtak == null) {
                repository.opprettVedtak(
                    OpprettVedtak(
                        soeker = Folkeregisteridentifikator.of(klage.sak.ident),
                        sakId = klage.sak.id,
                        sakType = klage.sak.sakType,
                        behandlingId = klage.id,
                        type = VedtakType.AVVIST_KLAGE,
                        innhold = VedtakInnhold.Klage(klage.toObjectNode()),
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
                                klage = klage.toObjectNode(),
                            ),
                    ),
                )
            }
        return vedtak.id
    }

    fun fattVedtak(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        logger.info("Fatter vedtak for klage=${klage.id}")
        val eksisterendeVedtak =
            repository.hentVedtak(klage.id)
                ?: throw NotFoundException("Fant ikke vedtak for klage med id=${klage.id}")

        verifiserGyldigVedtakStatus(eksisterendeVedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        repository.oppdaterVedtak(
            eksisterendeVedtak.copy(
                innhold = VedtakInnhold.Klage(klage.toObjectNode()),
            ),
        )

        return repository.fattVedtak(
            klage.id,
            VedtakFattet(
                ansvarligSaksbehandler = brukerTokenInfo.ident(),
                ansvarligEnhet = klage.sak.enhet,
                tidspunkt = Tidspunkt.now(),
            ),
        ).id
    }

    private fun verifiserGyldigVedtakStatus(
        gjeldendeStatus: VedtakStatus,
        forventetStatus: List<VedtakStatus>,
    ) {
        if (gjeldendeStatus !in forventetStatus) throw VedtakTilstandException(gjeldendeStatus, forventetStatus)
    }
}
