package no.nav.etterlatte.behandling.tilbakekreving

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.FattetVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriodeVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilbakekrevingService(
    private val tilbakekrevingDao: TilbakekrevingDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val vedtakKlient: VedtakKlient,
    private val brevApiKlient: BrevApiKlient,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevinghendelser: TilbakekrevingHendelserService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentTilbakekrevinger(sakId: Long) =
        inTransaction {
            logger.info("Henter tilbakekrevinger sak=$sakId")
            tilbakekrevingDao.hentTilbakekrevinger(sakId)
        }

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Oppretter tilbakekreving=${kravgrunnlag.kravgrunnlagId} på sak=${kravgrunnlag.sakId}")

            val sak =
                sakDao.hentSak(kravgrunnlag.sakId.value)
                    ?: throw TilbakekrevingHarMangelException("Tilbakekreving mangler sak")

            val tilbakekrevingBehandling =
                tilbakekrevingDao.lagreTilbakekreving(
                    TilbakekrevingBehandling.ny(kravgrunnlag, sak),
                )

            oppgaveService.hentOppgaverForReferanse(kravgrunnlag.sisteUtbetalingslinjeId.value)
                .filter { it.type == OppgaveType.TILBAKEKREVING }.let {
                    if (it.isEmpty()) {
                        logger.info("Fant ingen tilbakekrevingsoppgave, oppretter ny")
                        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                            referanse = tilbakekrevingBehandling.id.toString(),
                            sakId = tilbakekrevingBehandling.sak.id,
                            oppgaveKilde = OppgaveKilde.TILBAKEKREVING,
                            oppgaveType = OppgaveType.TILBAKEKREVING,
                            merknad = "Kravgrunnlag mottatt",
                        )
                    } else {
                        val eksisterendeOppgave = it.single()

                        logger.info("Kobler nytt kravgrunnlag med eksisterende oppgave ${eksisterendeOppgave.id}")
                        oppgaveService.oppdaterReferanseOgMerknad(
                            oppgaveId = eksisterendeOppgave.id,
                            referanse = tilbakekrevingBehandling.id.toString(),
                            merknad = "Kravgrunnlag mottatt",
                        )
                    }
                }

            hendelseDao.tilbakekrevingOpprettet(
                tilbakekrevingId = tilbakekrevingBehandling.id,
                sakId = tilbakekrevingBehandling.sak.id,
            )

            val statistikkTilbakekrevingDto =
                StatistikkTilbakekrevingDto(
                    tilbakekrevingBehandling.id,
                    tilbakekrevingBehandling,
                    Tidspunkt.now(),
                )
            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekrevingDto,
                TilbakekrevingHendelseType.OPPRETTET,
            )

            tilbakekrevingBehandling
        }

    fun hentTilbakekreving(tilbakekrevingId: UUID): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Henter tilbakekreving med id=$tilbakekrevingId")
            tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        }

    fun lagreVurdering(
        tilbakekrevingId: UUID,
        vurdering: TilbakekrevingVurdering,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer vurdering for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(
                    status = TilbakekrevingStatus.UNDER_ARBEID,
                    tilbakekreving =
                        tilbakekreving.tilbakekreving.copy(
                            vurdering = vurdering,
                        ),
                ),
            )
        }

    fun lagrePerioder(
        tilbakekrevingId: UUID,
        perioder: List<TilbakekrevingPeriode>,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer perioder for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(
                    status = TilbakekrevingStatus.UNDER_ARBEID,
                    tilbakekreving =
                        tilbakekreving.tilbakekreving.copy(
                            perioder = perioder,
                        ),
                ),
            )
        }

    fun lagreSkalSendeBrev(
        tilbakekrevingId: UUID,
        sendeBrev: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer om brev skal sendes for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(
                    sendeBrev = sendeBrev,
                ),
            )
        }

    fun validerVurderingOgPerioder(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Sjekker at vurdering og perioder er gyldig for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            tilbakekreving.validerVurderingOgPerioder()

            val lagretTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    tilbakekreving.copy(
                        status = TilbakekrevingStatus.VALIDERT,
                    ),
                )

            runBlocking {
                logger.info("Oppretter eller oppdaterer vedtak for tilbakekreving=$tilbakekrevingId")
                val vedtakId =
                    vedtakKlient.lagreVedtakTilbakekreving(
                        tilbakekrevingBehandling = lagretTilbakekreving,
                        brukerTokenInfo = brukerTokenInfo,
                        enhet = lagretTilbakekreving.sak.enhet,
                    )

                logger.info("Lagret vedtak med vedtakId $vedtakId for tilbakekreving $tilbakekrevingId")
            }

            lagretTilbakekreving
        }

    suspend fun fattVedtak(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = inTransaction {
        logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingId")
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

        sjekkAtTilbakekrevingErGyldigForVedtak(tilbakekreving)
        sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

        if (!tilbakekreving.gyldigForVedtak()) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har ikke gyldig status for vedtak (${tilbakekreving.status}",
            )
        }

        runBlocking {
            val vedtaksbrev = brevApiKlient.hentVedtaksbrev(tilbakekrevingId, brukerTokenInfo)
            if (tilbakekreving.sendeBrev && vedtaksbrev == null) {
                throw TilbakekrevingManglerBrevException(
                    "Kan ikke fatte tilbakekrevingsvedtak uten vedtaksbrev når dette er spesifisert",
                )
            }
            if (!tilbakekreving.sendeBrev && vedtaksbrev != null) {
                logger.info("Sletter ubrukt vedtaksbrev med id ${vedtaksbrev.id}")
                brevApiKlient.slettVedtaksbrev(tilbakekrevingId, brukerTokenInfo)
            }
        }

        val vedtakId =
            runBlocking {
                vedtakKlient.fattVedtakTilbakekreving(
                    tilbakekrevingId = tilbakekreving.id,
                    brukerTokenInfo = brukerTokenInfo,
                    enhet = tilbakekreving.sak.enhet,
                )
            }

        val oppdatertTilbakekreving =
            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(status = TilbakekrevingStatus.FATTET_VEDTAK),
            )

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

        val statistikkTilbakekrevingDto =
            StatistikkTilbakekrevingDto(
                tilbakekreving.id,
                tilbakekreving,
                Tidspunkt.now(),
            )
        tilbakekrevinghendelser.sendTilbakekreving(
            statistikkTilbakekrevingDto,
            TilbakekrevingHendelseType.FATTET_VEDTAK,
        )

        oppgaveService.tilAttestering(
            referanse = tilbakekreving.id.toString(),
            type = OppgaveType.TILBAKEKREVING,
            merknad = "Tilbakekreving kan attesteres",
        )

        oppdatertTilbakekreving
    }

    suspend fun attesterVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkForventetStatus(tilbakekreving, TilbakekrevingStatus.FATTET_VEDTAK)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            if (tilbakekreving.sendeBrev) {
                logger.info("Sender vedtaksbrev for tilbakekreving=$tilbakekrevingId")
                runBlocking { brevApiKlient.ferdigstillVedtaksbrev(tilbakekrevingId, tilbakekreving.sak.id, brukerTokenInfo) }
            } else {
                logger.info("Skal ikke sende vedtaksbrev for tilbakekreving=$tilbakekrevingId")
            }

            val vedtak =
                runBlocking {
                    vedtakKlient.attesterVedtakTilbakekreving(
                        tilbakekrevingId = tilbakekreving.id,
                        brukerTokenInfo = brukerTokenInfo,
                        enhet = tilbakekreving.sak.enhet,
                    )
                }

            val oppdatertTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    tilbakekreving.copy(status = TilbakekrevingStatus.ATTESTERT),
                )

            hendelseDao.vedtakHendelse(
                behandlingId = tilbakekreving.id,
                sakId = tilbakekreving.sak.id,
                vedtakId = vedtak.id,
                hendelse = HendelseType.ATTESTERT,
                inntruffet = Tidspunkt.now(),
                saksbehandler = brukerTokenInfo.ident(),
                kommentar = kommentar,
                begrunnelse = null,
            )

            oppgaveService.ferdigStillOppgaveUnderBehandling(
                referanse = tilbakekreving.id.toString(),
                type = OppgaveType.TILBAKEKREVING,
                saksbehandler = brukerTokenInfo,
            )

            val statistikkTilbakekrevingDto =
                StatistikkTilbakekrevingDto(
                    tilbakekreving.id,
                    tilbakekreving,
                    Tidspunkt.now(),
                )

            tilbakekrevinghendelser.sendTilbakekreving(statistikkTilbakekrevingDto, TilbakekrevingHendelseType.ATTESTERT)

            runBlocking {
                tilbakekrevingKlient.sendTilbakekrevingsvedtak(
                    brukerTokenInfo,
                    TilbakekrevingVedtak(
                        vedtakId = tilbakekreving.tilbakekreving.kravgrunnlag.vedtakId.value,
                        fattetVedtak =
                            FattetVedtak(
                                saksbehandler = vedtak.fattetAv,
                                enhet = vedtak.enhet,
                                dato = vedtak.dato,
                            ),
                        aarsak = requireNotNull(tilbakekreving.tilbakekreving.vurdering?.aarsak),
                        hjemmel = requireNotNull(tilbakekreving.tilbakekreving.vurdering?.hjemmel),
                        kravgrunnlagId = tilbakekreving.tilbakekreving.kravgrunnlag.kravgrunnlagId.value.toString(),
                        kontrollfelt = tilbakekreving.tilbakekreving.kravgrunnlag.kontrollFelt.value,
                        perioder =
                            tilbakekreving.tilbakekreving.perioder.map {
                                TilbakekrevingPeriodeVedtak(
                                    maaned = it.maaned,
                                    ytelse = it.ytelse.toYtelseVedtak(),
                                    feilkonto = it.feilkonto.toFeilkontoVedtak(),
                                )
                            },
                    ),
                )
            }

            oppdatertTilbakekreving
        }

    suspend fun underkjennVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkForventetStatus(tilbakekreving, TilbakekrevingStatus.FATTET_VEDTAK)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, brukerTokenInfo)

            val vedtakId =
                runBlocking {
                    vedtakKlient.underkjennVedtakTilbakekreving(
                        tilbakekrevingId = tilbakekreving.id,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }

            val oppdatertTilbakekreving =
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

            oppgaveService.tilUnderkjent(
                referanse = tilbakekreving.id.toString(),
                type = OppgaveType.TILBAKEKREVING,
                merknad = listOfNotNull(valgtBegrunnelse, kommentar).joinToString(separator = ": "),
            )

            oppdatertTilbakekreving
        }

    private fun sjekkForventetStatus(
        tilbakekreving: TilbakekrevingBehandling,
        forventetStatus: TilbakekrevingStatus,
    ) {
        if (tilbakekreving.status != forventetStatus) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har status ${tilbakekreving.status} men forventet $forventetStatus",
            )
        }
    }

    private fun sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving: TilbakekrevingBehandling) {
        if (!tilbakekreving.underBehandling()) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har status ${tilbakekreving.status} og er ikke under behandling",
            )
        }
    }

    private fun sjekkAtTilbakekrevingErGyldigForVedtak(tilbakekreving: TilbakekrevingBehandling) {
        if (!tilbakekreving.gyldigForVedtak()) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har status ${tilbakekreving.status} og er ikke gyldig for vedtak",
            )
        }
    }

    private fun sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(
        tilbakekrevingId: UUID,
        saksbehandler: BrukerTokenInfo,
    ) {
        val oppgaveUnderBehandling =
            oppgaveService.hentOppgaveUnderBehandling(tilbakekrevingId.toString())
                ?: throw TilbakekrevingFeilTilstandException(
                    "Oppgaven tilknyttet tilbakekreving $tilbakekrevingId er ikke under behandling",
                )

        if (oppgaveUnderBehandling.saksbehandler?.ident != saksbehandler.ident()) {
            throw TilbakekrevingFeilTilstandException(
                "Saksbehandler ${saksbehandler.ident()} er ikke tilknyttet oppgave ${oppgaveUnderBehandling.id}",
            )
        }
    }
}
