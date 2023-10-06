package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
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
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentTilbakekreving(tilbakekrevingId: UUID): Tilbakekreving =
        inTransaction {
            logger.info("Henter tilbakekreving med id=$tilbakekrevingId")
            tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        }

    fun lagreVurdering(
        tilbakekrevingId: UUID,
        vurdering: TilbakekrevingVurdering,
    ): Tilbakekreving =
        inTransaction {
            logger.info("Lagrer vurdering for tilbakekreving=$tilbakekrevingId")
            val eksisterende = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
            if (!eksisterende.underBehandling()) throw TilbakekrevingErIkkeUnderBehandlingException()
            tilbakekrevingDao.lagreTilbakekreving(eksisterende.copy(vurdering = vurdering))
        }

    fun lagrePerioder(
        tilbakekrevingId: UUID,
        perioder: List<TilbakekrevingPeriode>,
    ): Tilbakekreving =
        inTransaction {
            logger.info("Lagrer perioder for tilbakekreving=$tilbakekrevingId")
            val eksisterende = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
            if (!eksisterende.underBehandling()) throw TilbakekrevingErIkkeUnderBehandlingException()
            tilbakekrevingDao.lagreTilbakekreving(eksisterende.copy(perioder = perioder))
        }

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) =
        inTransaction {
            logger.info("Oppretter tilbakekreving=${kravgrunnlag.kravgrunnlagId} på sak=${kravgrunnlag.sakId}")

            val sak = sakDao.hentSak(kravgrunnlag.sakId.value) ?: throw KravgrunnlagHarIkkeEksisterendeSakException()

            val tilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    Tilbakekreving.ny(kravgrunnlag, sak),
                )

            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = tilbakekreving.id.toString(),
                sakId = tilbakekreving.sak.id,
                oppgaveKilde = OppgaveKilde.EKSTERN,
                oppgaveType = OppgaveType.TILBAKEKREVING,
                merknad = null,
            )

            hendelseDao.tilbakekrevingOpprettet(
                tilbakekrevingId = tilbakekreving.id,
                sakId = tilbakekreving.sak.id,
            )
        }

    suspend fun fattVedtak(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        if (!tilbakekreving.underBehandling()) throw TilbakekrevingErIkkeUnderBehandlingException()

        val vedtakId =
            vedtakKlient.fattVedtakTilbakekreving(
                tilbakekreving = tilbakekreving,
                brukerTokenInfo = brukerTokenInfo,
                enhet = tilbakekreving.sak.enhet,
            )

        inTransaction {
            logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingId")

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
    }
}
