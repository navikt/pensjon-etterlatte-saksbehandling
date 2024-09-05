package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakTilbakekrevingService(
    private val repository: VedtaksvurderingRepository,
    private val rapidService: VedtaksvurderingRapidService,
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettEllerOppdaterVedtak(tilbakekrevingVedtakData: TilbakekrevingVedtakDto): Long {
        logger.info("Oppretter eller oppdaterer vedtak for tilbakekreving=${tilbakekrevingVedtakData.tilbakekrevingId}")
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
                        innhold = VedtakInnhold.Tilbakekreving(tilbakekrevingVedtakData.tilbakekreving),
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
                            VedtakInnhold.Tilbakekreving(
                                tilbakekreving = tilbakekrevingVedtakData.tilbakekreving,
                            ),
                    ),
                )
            }
        return vedtak.id
    }

    fun fattVedtak(
        tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        logger.info("Fatter vedtak for tilbakekreving=${tilbakekrevingVedtakData.tilbakekrevingId}")
        verifiserGyldigVedtakStatus(tilbakekrevingVedtakData.tilbakekrevingId, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        return repository
            .fattVedtak(
                tilbakekrevingVedtakData.tilbakekrevingId,
                VedtakFattet(
                    ansvarligSaksbehandler = brukerTokenInfo.ident(),
                    ansvarligEnhet = tilbakekrevingVedtakData.enhet,
                    // Blir ikke brukt fordi egen now() brukes i db..
                    tidspunkt = Tidspunkt.now(),
                ),
            ).id
    }

    fun attesterVedtak(
        tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingVedtakLagretDto {
        logger.info("Attesterer vedtak for tilbakekreving=${tilbakekrevingVedtakData.tilbakekrevingId}")
        val tilbakekrevingId = tilbakekrevingVedtakData.tilbakekrevingId
        val vedtak =
            requireNotNull(repository.hentVedtak(tilbakekrevingId)) {
                "Vedtak for tilbakekreving $tilbakekrevingId finnes ikke"
            }
        verifiserGyldigVedtakStatus(tilbakekrevingId, listOf(VedtakStatus.FATTET_VEDTAK))
        attestantHarAnnenIdentEnnSaksbehandler(vedtak.vedtakFattet!!.ansvarligSaksbehandler, brukerTokenInfo)

        val attestertVedtak =
            repository.attesterVedtak(
                tilbakekrevingId,
                Attestasjon(
                    attestant = brukerTokenInfo.ident(),
                    attesterendeEnhet = tilbakekrevingVedtakData.enhet,
                    // Blir ikke brukt for egen now() brukes i db.
                    tidspunkt = Tidspunkt.now(),
                ),
            )

        val skalSendeBrev =
            runBlocking {
                behandlingKlient.hentTilbakekrevingBehandling(tilbakekrevingId, brukerTokenInfo).sendeBrev
            }

        rapidService.sendToRapid(
            VedtakOgRapid(
                attestertVedtak.toDto(),
                RapidInfo(
                    vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                    vedtak = attestertVedtak.toDto(),
                    tekniskTid = attestertVedtak.attestasjon!!.tidspunkt,
                    behandlingId = attestertVedtak.behandlingId,
                    extraParams =
                        mapOf(SKAL_SENDE_BREV to skalSendeBrev),
                ),
            ),
        )

        return requireNotNull(attestertVedtak.vedtakFattet).let {
            TilbakekrevingVedtakLagretDto(
                id = attestertVedtak.id,
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

    private fun attestantHarAnnenIdentEnnSaksbehandler(
        ansvarligSaksbehandler: String,
        innloggetBrukerTokenInfo: BrukerTokenInfo,
    ) {
        if (innloggetBrukerTokenInfo.erSammePerson(ansvarligSaksbehandler)) {
            throw UgyldigAttestantException(innloggetBrukerTokenInfo.ident())
        }
    }
}
