package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingFattEllerAttesterVedtakDto
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

enum class TilbakekrevingVedtakToggles(
    private val toggle: String,
) : FeatureToggle {
    TILLAT_DOBBEL_ATTESTERING("tillat-dobbel-attestering"),
    ;

    override fun key(): String = toggle
}

class VedtakTilbakekrevingService(
    private val repository: VedtaksvurderingRepository,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettEllerOppdaterVedtak(tilbakekrevingVedtakData: TilbakekrevingVedtakDto): Vedtak {
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
        return vedtak
    }

    fun fattVedtak(
        tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
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
            )
    }

    fun attesterVedtak(
        tilbakekrevingVedtakData: TilbakekrevingFattEllerAttesterVedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
        logger.info("Attesterer vedtak for tilbakekreving=${tilbakekrevingVedtakData.tilbakekrevingId}")
        val tilbakekrevingId = tilbakekrevingVedtakData.tilbakekrevingId
        val vedtak =
            krevIkkeNull(repository.hentVedtak(tilbakekrevingId)) {
                "Vedtak for tilbakekreving $tilbakekrevingId finnes ikke"
            }
        val gyldigeVedtakStatuser =
            if (featureToggleService.isEnabled(TilbakekrevingVedtakToggles.TILLAT_DOBBEL_ATTESTERING, false)) {
                listOf(VedtakStatus.FATTET_VEDTAK, VedtakStatus.ATTESTERT)
            } else {
                listOf(VedtakStatus.FATTET_VEDTAK)
            }

        verifiserGyldigVedtakStatus(tilbakekrevingId, gyldigeVedtakStatuser)
        attestantHarAnnenIdentEnnSaksbehandler(vedtak.vedtakFattet!!.ansvarligSaksbehandler, brukerTokenInfo)

        // Behandling sender ut hendelse om attestert vedtak selv for å unngå at brev blir sendt selv om
        // tilbakekrevingsvedtak feiler.

        return repository.inTransaction { tx ->
            val attestertVedtak =
                repository.attesterVedtak(
                    tilbakekrevingId,
                    Attestasjon(
                        attestant = brukerTokenInfo.ident(),
                        attesterendeEnhet = tilbakekrevingVedtakData.enhet,
                        // Blir ikke brukt for egen now() brukes i db.
                        tidspunkt = Tidspunkt.now(),
                    ),
                    tx,
                )

            attestertVedtak
        }
    }

    fun tilbakeStillAttestert(behandlingId: UUID): Vedtak {
        val vedtak =
            krevIkkeNull(repository.hentVedtak(behandlingId)) {
                "Fant ikke tilbakekrevingsvedtak som skal tilbakestilles"
            }
        verifiserGyldigVedtakStatus(vedtak.behandlingId, listOf(VedtakStatus.ATTESTERT))

        return repository.inTransaction { tx ->
            repository.tilbakestillTilbakekrevingsvedtak(tilbakekrevingId = behandlingId, tx)
            krevIkkeNull(repository.hentVedtak(behandlingId, tx)) {
                "Tilbakekrevingen vi akkurat tilbakestilte fins ikke lengre"
            }
        }
    }

    fun underkjennVedtak(tilbakekrevingId: UUID): Vedtak {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        verifiserGyldigVedtakStatus(tilbakekrevingId, listOf(VedtakStatus.FATTET_VEDTAK))
        return repository.underkjennVedtak(tilbakekrevingId)
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
