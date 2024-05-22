package no.nav.etterlatte.behandling.tilbakekreving

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriodeVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVilkaar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.oppgave.PaaVent
import no.nav.etterlatte.oppgave.PaaVentAarsak
import no.nav.etterlatte.sak.SakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilbakekrevingService(
    private val tilbakekrevingDao: TilbakekrevingDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
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

            tilbakekrevingDao.hentTilbakekrevinger(sak.id)
                .find { it.underBehandlingEllerFattetVedtak() }
                ?.let {
                    throw TilbakekrevingUnderBehandlingFinnesAlleredeException(
                        "Det finnes allerede en tilbakekreving under behandling i denne saken. Denne må ferdigstilles " +
                            "eller avbrytes før det kan opprettes en ny tilbakekrevingsbehandling",
                    )
                }

            val tilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    TilbakekrevingBehandling.ny(kravgrunnlag, sak),
                )

            val oppgaveFraBehandlingMedFeilutbetaling =
                oppgaveService.hentOppgaverForReferanse(kravgrunnlag.sakId.value.toString())
                    .filter { it.type == OppgaveType.TILBAKEKREVING }
                    .maxByOrNull { it.opprettet }

            if (oppgaveFraBehandlingMedFeilutbetaling != null) {
                logger.info("Kobler nytt kravgrunnlag med eksisterende oppgave ${oppgaveFraBehandlingMedFeilutbetaling.id}")
                oppgaveService.oppdaterReferanseOgMerknad(
                    oppgaveId = oppgaveFraBehandlingMedFeilutbetaling.id,
                    referanse = tilbakekreving.id.toString(),
                    merknad = "Kravgrunnlag mottatt",
                )
            } else {
                logger.info("Fant ingen tilbakekrevingsoppgave, oppretter ny")
                oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                    referanse = tilbakekreving.id.toString(),
                    sakId = tilbakekreving.sak.id,
                    oppgaveKilde = OppgaveKilde.TILBAKEKREVING,
                    oppgaveType = OppgaveType.TILBAKEKREVING,
                    merknad = "Kravgrunnlag mottatt",
                )
            }

            tilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.OPPRETTET)

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                type = TilbakekrevingHendelseType.OPPRETTET,
            )

            tilbakekreving
        }

    fun endreTilbakekrevingOppgaveStatus(
        sakId: Long,
        paaVent: Boolean,
    ) {
        inTransaction {
            logger.info("Endrer oppgavestatus for oppgave på tilbakekreving for sakId=$sakId")
            val tilbakekreving = tilbakekrevingDao.hentNyesteTilbakekreving(sakId)

            sjekkAtTilbakekrevingIkkeErFerdigstiltEllerAvbrutt(tilbakekreving)

            val oppgave =
                oppgaveService.hentOppgaverForReferanse(tilbakekreving.id.toString())
                    .firstOrNull { it.type == OppgaveType.TILBAKEKREVING } ?: throw TilbakekrevingFeilTilstandException(
                    "Fant ingen oppgave tilknyttet ${tilbakekreving.id}",
                )

            val merknad = if (paaVent) "Kravgrunnlag er sperret" else "Sperre på kravgrunnlag opphevet"
            val aarsak = if (paaVent) PaaVentAarsak.KRAVGRUNNLAG_SPERRET else null
            oppgaveService.endrePaaVent(PaaVent(oppgaveId = oppgave.id, merknad = merknad, paavent = paaVent, aarsak = aarsak))
        }
    }

    fun avbrytTilbakekreving(sakId: Long): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Avbryter tilbakekreving for sakId=$sakId")

            val tilbakekreving = tilbakekrevingDao.hentNyesteTilbakekreving(sakId)

            sjekkAtTilbakekrevingKanAvbrytes(tilbakekreving)

            val avbruttTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    tilbakekreving.copy(
                        status = TilbakekrevingStatus.AVBRUTT,
                    ),
                )

            oppgaveService.avbrytAapneOppgaverMedReferanse(tilbakekreving.id.toString())

            tilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.AVBRUTT)

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                type = TilbakekrevingHendelseType.AVBRUTT,
            )

            avbruttTilbakekreving
        }

    fun hentTilbakekreving(tilbakekrevingId: UUID): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Henter tilbakekreving med id=$tilbakekrevingId")
            tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
        }

    fun lagreVurdering(
        tilbakekrevingId: UUID,
        vurdering: TilbakekrevingVurdering,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer vurdering for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

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
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer perioder for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

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
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Lagrer om brev skal sendes for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(
                    sendeBrev = sendeBrev,
                ),
            )
        }

    fun validerVurderingOgPerioder(
        tilbakekrevingId: UUID,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Sjekker at vurdering og perioder er gyldig for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)
            sjekkAtTilbakekrevingErGyldig(tilbakekreving)

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
                        brukerTokenInfo = saksbehandler,
                        enhet = lagretTilbakekreving.sak.enhet,
                    )

                logger.info("Lagret vedtak med vedtakId $vedtakId for tilbakekreving $tilbakekrevingId")
            }

            lagretTilbakekreving
        }

    suspend fun fattVedtak(
        tilbakekrevingId: UUID,
        saksbehandler: Saksbehandler,
    ) = inTransaction {
        logger.info("Fatter vedtak for tilbakekreving=$tilbakekrevingId")
        val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

        sjekkAtTilbakekrevingErGyldigForVedtak(tilbakekreving)
        sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

        runBlocking {
            val vedtaksbrev = brevApiKlient.hentVedtaksbrev(tilbakekrevingId, saksbehandler)
            if (tilbakekreving.sendeBrev && vedtaksbrev == null) {
                throw TilbakekrevingManglerBrevException(
                    "Kan ikke fatte tilbakekrevingsvedtak uten vedtaksbrev når dette er spesifisert",
                )
            }
            if (!tilbakekreving.sendeBrev && vedtaksbrev != null) {
                logger.info("Sletter ubrukt vedtaksbrev med id ${vedtaksbrev.id}")
                brevApiKlient.slettVedtaksbrev(tilbakekrevingId, saksbehandler)
            }
        }

        val vedtakId =
            runBlocking {
                vedtakKlient.fattVedtakTilbakekreving(
                    tilbakekrevingId = tilbakekreving.id,
                    brukerTokenInfo = saksbehandler,
                    enhet = tilbakekreving.sak.enhet,
                )
            }

        val oppdatertTilbakekreving =
            tilbakekrevingDao.lagreTilbakekreving(
                tilbakekreving.copy(status = TilbakekrevingStatus.FATTET_VEDTAK),
            )

        tilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.FATTET_VEDTAK, vedtakId, saksbehandler)

        tilbakekrevinghendelser.sendTilbakekreving(
            statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
            type = TilbakekrevingHendelseType.FATTET_VEDTAK,
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
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkForventetStatus(tilbakekreving, TilbakekrevingStatus.FATTET_VEDTAK)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

            if (tilbakekreving.sendeBrev) {
                logger.info("Ferdigstiller vedtaksbrev for tilbakekreving=$tilbakekrevingId")
                runBlocking { brevApiKlient.ferdigstillVedtaksbrev(tilbakekrevingId, tilbakekreving.sak.id, saksbehandler) }
            } else {
                logger.info("Skal ikke sende vedtaksbrev for tilbakekreving=$tilbakekrevingId")
            }

            val vedtak =
                runBlocking {
                    vedtakKlient.attesterVedtakTilbakekreving(
                        tilbakekrevingId = tilbakekreving.id,
                        brukerTokenInfo = saksbehandler,
                        enhet = tilbakekreving.sak.enhet,
                    )
                }

            val oppdatertTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    tilbakekreving.copy(status = TilbakekrevingStatus.ATTESTERT),
                )

            tilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.ATTESTERT, vedtak.id, saksbehandler, kommentar)

            oppgaveService.ferdigStillOppgaveUnderBehandling(
                referanse = tilbakekreving.id.toString(),
                type = OppgaveType.TILBAKEKREVING,
                saksbehandler = saksbehandler,
            )

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                type = TilbakekrevingHendelseType.ATTESTERT,
            )

            runBlocking {
                tilbakekrevingKlient.sendTilbakekrevingsvedtak(
                    saksbehandler,
                    tilbakekrevingVedtak(tilbakekreving, vedtak),
                )
            }

            oppdatertTilbakekreving
        }

    suspend fun underkjennVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Underkjenner vedtak for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkForventetStatus(tilbakekreving, TilbakekrevingStatus.FATTET_VEDTAK)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

            val vedtakId =
                runBlocking {
                    vedtakKlient.underkjennVedtakTilbakekreving(
                        tilbakekrevingId = tilbakekreving.id,
                        brukerTokenInfo = saksbehandler,
                    )
                }

            val oppdatertTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(tilbakekreving.copy(status = TilbakekrevingStatus.UNDERKJENT))

            tilbakekrevingHendelse(
                tilbakekreving = tilbakekreving,
                vedtakId = vedtakId,
                saksbehandler = saksbehandler,
                hendelseType = TilbakekrevingHendelseType.UNDERKJENT,
                kommentar = kommentar,
                begrunnelse = valgtBegrunnelse,
            )

            oppgaveService.tilUnderkjent(
                referanse = tilbakekreving.id.toString(),
                type = OppgaveType.TILBAKEKREVING,
                merknad = listOfNotNull(valgtBegrunnelse, kommentar).joinToString(separator = ": "),
            )

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                type = TilbakekrevingHendelseType.UNDERKJENT,
            )

            oppdatertTilbakekreving
        }

    private fun tilbakekrevingHendelse(
        tilbakekreving: TilbakekrevingBehandling,
        hendelseType: TilbakekrevingHendelseType,
        vedtakId: Long? = null,
        saksbehandler: Saksbehandler? = null,
        kommentar: String? = null,
        begrunnelse: String? = null,
    ) {
        hendelseDao.tilbakekrevingHendelse(
            tilbakekrevingId = tilbakekreving.id,
            sakId = tilbakekreving.sak.id,
            hendelse = hendelseType,
            vedtakId = vedtakId,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler?.ident,
            kommentar = kommentar,
            begrunnelse = begrunnelse,
        )
    }

    private fun sjekkAtTilbakekrevingErGyldig(tilbakekreving: TilbakekrevingBehandling) {
        tilbakekreving.validerVurderingOgPerioder()
    }

    private fun tilbakekrevingVedtak(
        tilbakekreving: TilbakekrevingBehandling,
        vedtak: TilbakekrevingVedtakLagretDto,
    ) = TilbakekrevingVedtak(
        vedtakId = tilbakekreving.tilbakekreving.kravgrunnlag.vedtakId.value,
        fattetVedtak =
            FattetVedtak(
                saksbehandler = vedtak.fattetAv,
                enhet = vedtak.enhet,
                dato = vedtak.dato,
            ),
        aarsak = requireNotNull(tilbakekreving.tilbakekreving.vurdering?.aarsak),
        hjemmel = hjemmelFraVurdering(requireNotNull(tilbakekreving.tilbakekreving.vurdering)),
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
    )

    private fun hjemmelFraVurdering(vurdering: TilbakekrevingVurdering): TilbakekrevingHjemmel {
        return if (vurdering.vilkaarsresultat == TilbakekrevingVilkaar.IKKE_OPPFYLT) {
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD
        } else {
            requireNotNull(vurdering.rettsligGrunnlag)
        }
    }

    private fun tilbakekrevingForStatistikk(tilbakekreving: TilbakekrevingBehandling): StatistikkTilbakekrevingDto {
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(tilbakekreving.sak.id)?.type
        return StatistikkTilbakekrevingDto(
            tilbakekreving.id,
            tilbakekreving,
            Tidspunkt.now(),
            utlandstilknytningType,
        )
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

    private fun sjekkAtTilbakekrevingKanAvbrytes(tilbakekreving: TilbakekrevingBehandling) {
        if (!tilbakekreving.underBehandlingEllerFattetVedtak()) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har status ${tilbakekreving.status} og kan ikke avbrytes",
            )
        }
    }

    private fun sjekkAtTilbakekrevingIkkeErFerdigstiltEllerAvbrutt(tilbakekreving: TilbakekrevingBehandling) {
        if (tilbakekreving.status in listOf(TilbakekrevingStatus.AVBRUTT, TilbakekrevingStatus.ATTESTERT)) {
            throw TilbakekrevingFeilTilstandException(
                "Tilbakekreving har status ${tilbakekreving.status} og kan ikke endres",
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
        saksbehandler: Saksbehandler,
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
