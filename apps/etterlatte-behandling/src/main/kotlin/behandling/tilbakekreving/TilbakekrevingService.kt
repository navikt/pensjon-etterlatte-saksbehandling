package no.nav.etterlatte.behandling.tilbakekreving

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.FattetVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriodeVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilbakekrevingService(
    private val tilbakekrevingDao: TilbakekrevingDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val vedtakKlient: VedtakKlient,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) =
        inTransaction {
            logger.info("Oppretter tilbakekreving=${kravgrunnlag.kravgrunnlagId} på sak=${kravgrunnlag.sakId}")

            val sak =
                sakDao.hentSak(kravgrunnlag.sakId.value)
                    ?: throw TilbakekrevingHarMangelException("Tilbakekreving mangler sak")

            val tilbakekrevingBehandling =
                tilbakekrevingDao.lagreTilbakekreving(
                    TilbakekrevingBehandling.ny(kravgrunnlag, sak),
                )

            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = tilbakekrevingBehandling.id.toString(),
                sakId = tilbakekrevingBehandling.sak.id,
                oppgaveKilde = OppgaveKilde.TILBAKEKREVING,
                oppgaveType = OppgaveType.TILBAKEKREVING,
                merknad = null,
            )

            hendelseDao.tilbakekrevingOpprettet(
                tilbakekrevingId = tilbakekrevingBehandling.id,
                sakId = tilbakekrevingBehandling.sak.id,
            )
        }

    fun hentTilbakekreving(tilbakekrevingId: UUID): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Henter tilbakekreving med id=$tilbakekrevingId")
            tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        }

    fun lagreVurdering(
        tilbakekrevingId: UUID,
        vurdering: TilbakekrevingVurdering,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer vurdering for tilbakekreving=$tilbakekrevingId")
            val eksisterende = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
            if (!eksisterende.underBehandling()) {
                throw TilbakekrevingFeilTilstandException("Tilbakekreving er ikke under behandling")
            }
            tilbakekrevingDao.lagreTilbakekreving(
                eksisterende.copy(
                    tilbakekreving =
                        eksisterende.tilbakekreving.copy(
                            vurdering = vurdering,
                        ),
                ),
            )
        }

    fun lagrePerioder(
        tilbakekrevingId: UUID,
        perioder: List<TilbakekrevingPeriode>,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer perioder for tilbakekreving=$tilbakekrevingId")
            val eksisterende = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
            if (!eksisterende.underBehandling()) {
                throw TilbakekrevingFeilTilstandException("Tilbakekreving er ikke under behandling")
            }
            tilbakekrevingDao.lagreTilbakekreving(
                eksisterende.copy(
                    tilbakekreving =
                        eksisterende.tilbakekreving.copy(
                            perioder = perioder,
                        ),
                ),
            )
        }

    suspend fun opprettVedtak(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = inTransaction {
        logger.info("Oppretter vedtak for tilbakekreving=$tilbakekrevingId")
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        if (!tilbakekreving.underBehandling()) {
            throw TilbakekrevingFeilTilstandException("Tilbakekreving er ikke under behandling")
        }
        runBlocking {
            vedtakKlient.lagreVedtakTilbakekreving(
                tilbakekrevingBehandling = tilbakekreving,
                brukerTokenInfo = brukerTokenInfo,
                enhet = tilbakekreving.sak.enhet,
            )
        }
    }

    suspend fun fattVedtak(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = inTransaction {
        logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingId")
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        if (!tilbakekreving.underBehandling()) {
            throw TilbakekrevingFeilTilstandException("Tilbakekreving er ikke under behandling")
        }

        val vedtakId =
            runBlocking {
                vedtakKlient.fattVedtakTilbakekreving(
                    tilbakekrevingId = tilbakekreving.id,
                    brukerTokenInfo = brukerTokenInfo,
                    enhet = tilbakekreving.sak.enhet,
                )
            }

        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving.copy(status = TilbakekrevingStatus.FATTET_VEDTAK))

        hendelseDao.vedtakHendelse(
            behandlingId = tilbakekreving.id,
            sakId = tilbakekreving.sak.id,
            vedtakId = vedtakId,
            hendelse = HendelseType.FATTET,
            inntruffet = Tidspunkt.now(),
            saksbehandler = brukerTokenInfo.ident(),
            kommentar = null,
            begrunnelse = null,
        )

        oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
            fattetoppgave =
                VedtakOppgaveDTO(
                    sakId = tilbakekreving.sak.id,
                    referanse = tilbakekreving.id.toString(),
                ),
            oppgaveType = OppgaveType.ATTESTERING,
            merknad = null,
            saksbehandler = brukerTokenInfo,
        )
    }

    suspend fun attesterVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) = inTransaction {
        logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingId")
        val behandling = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        if (behandling.status != TilbakekrevingStatus.FATTET_VEDTAK) {
            throw TilbakekrevingFeilTilstandException("Tilbakekreving kan ikke attesteres fordi vedtak ikke er fattet")
        }

        val vedtak =
            runBlocking {
                vedtakKlient.attesterVedtakTilbakekreving(
                    tilbakekrevingId = behandling.id,
                    brukerTokenInfo = brukerTokenInfo,
                    enhet = behandling.sak.enhet,
                )
            }
        tilbakekrevingDao.lagreTilbakekreving(behandling.copy(status = TilbakekrevingStatus.ATTESTERT))

        hendelseDao.vedtakHendelse(
            behandlingId = behandling.id,
            sakId = behandling.sak.id,
            vedtakId = vedtak.id,
            hendelse = HendelseType.ATTESTERT,
            inntruffet = Tidspunkt.now(),
            saksbehandler = brukerTokenInfo.ident(),
            kommentar = kommentar,
            begrunnelse = null,
        )

        oppgaveService.ferdigStillOppgaveUnderBehandling(
            referanse = behandling.id.toString(),
            saksbehandler = brukerTokenInfo,
        )

        runBlocking {
            tilbakekrevingKlient.sendTilbakekrevingsvedtak(
                brukerTokenInfo,
                TilbakekrevingVedtak(
                    // TODO sjekk opp dette - her sender vi vedtaksId fra tilbakekrevingskomponenten
                    vedtakId = behandling.tilbakekreving.kravgrunnlag.vedtakId.value,
                    fattetVedtak =
                        FattetVedtak(
                            saksbehandler = vedtak.fattetAv,
                            enhet = vedtak.enhet,
                            dato = vedtak.dato,
                        ),
                    aarsak = requireNotNull(behandling.tilbakekreving.vurdering.aarsak),
                    hjemmel = requireNotNull(behandling.tilbakekreving.vurdering.hjemmel),
                    kravgrunnlagId = behandling.tilbakekreving.kravgrunnlag.kravgrunnlagId.value.toString(),
                    kontrollfelt = behandling.tilbakekreving.kravgrunnlag.kontrollFelt.value,
                    perioder =
                        behandling.tilbakekreving.perioder.map {
                            TilbakekrevingPeriodeVedtak(
                                maaned = it.maaned,
                                ytelse = it.ytelse.toVedtak(),
                                feilkonto = it.feilkonto.toVedtak(),
                            )
                        },
                ),
            )
        }
    }

    suspend fun underkjennVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) = inTransaction {
        logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        if (tilbakekreving.status != TilbakekrevingStatus.FATTET_VEDTAK) {
            throw TilbakekrevingFeilTilstandException("Tilbakekreving kan ikke underkjennes fordi vedtak ikke er fattet")
        }

        val vedtakId =
            runBlocking {
                vedtakKlient.underkjennVedtakTilbakekreving(
                    tilbakekrevingId = tilbakekreving.id,
                    brukerTokenInfo = brukerTokenInfo,
                )
            }

        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving.copy(status = TilbakekrevingStatus.UNDERKJENT))

        hendelseDao.vedtakHendelse(
            behandlingId = tilbakekreving.id,
            sakId = tilbakekreving.sak.id,
            vedtakId = vedtakId,
            hendelse = HendelseType.UNDERKJENT,
            inntruffet = Tidspunkt.now(),
            saksbehandler = brukerTokenInfo.ident(),
            kommentar = kommentar,
            begrunnelse = valgtBegrunnelse,
        )

        oppgaveService.ferdigstillOppgaveUnderbehandlingOgLagNyMedType(
            fattetoppgave =
                VedtakOppgaveDTO(
                    sakId = tilbakekreving.sak.id,
                    referanse = tilbakekreving.id.toString(),
                ),
            oppgaveType = OppgaveType.UNDERKJENT,
            merknad = listOfNotNull(valgtBegrunnelse, kommentar).joinToString(separator = ": "),
            saksbehandler = brukerTokenInfo,
        )
    }
}
