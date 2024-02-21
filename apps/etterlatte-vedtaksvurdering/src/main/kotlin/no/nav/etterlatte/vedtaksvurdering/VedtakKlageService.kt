package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.KlageFattVedtakDto
import no.nav.etterlatte.libs.common.vedtak.KlageVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.OpprettVedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakTilstandException
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakKlageService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtakKlageService::class.java)

    fun opprettEllerOppdaterVedtakOmAvvisning(
        klageId: UUID,
        klageVedtakDto: KlageVedtakDto,
    ): Long {
        logger.info("Oppretter eller oppdaterer vedtak for klage med id=$klageId")
        val eksisterendeVedtak = repository.hentVedtak(klageId)
        val vedtak =
            if (eksisterendeVedtak == null) {
                repository.opprettVedtak(
                    OpprettVedtak(
                        soeker = klageVedtakDto.soeker,
                        sakId = klageVedtakDto.sakId,
                        sakType = klageVedtakDto.sakType,
                        behandlingId = klageId,
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

    fun fattVedtak(
        klageId: UUID,
        klageFattVedtakDto: KlageFattVedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        logger.info("Fatter vedtak for klage=$klageFattVedtakDto.klageId")
        val eksisterendeVedtak =
            repository.hentVedtak(klageId)
                ?: throw NotFoundException("Fant ikke vedtak for klage med id=$klageId")

        verifiserGyldigVedtakStatus(eksisterendeVedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        return repository.fattVedtak(
            klageId,
            VedtakFattet(
                ansvarligSaksbehandler = brukerTokenInfo.ident(),
                ansvarligEnhet = klageFattVedtakDto.enhet,
                // Blir ikke brukt fordi egen now() brukes i db..
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
