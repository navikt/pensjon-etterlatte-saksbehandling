package no.nav.etterlatte.behandling.tilbakekreving

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.feilhaandtering.sjekkIkkeNull
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.FattetVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVilkaar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilbakekrevingService(
    private val tilbakekrevingDao: TilbakekrevingDao,
    private val sakDao: SakLesDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val vedtakKlient: VedtakKlient,
    private val brevApiKlient: BrevApiKlient,
    private val brevService: BrevService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevinghendelser: TilbakekrevingHendelserService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentTilbakekrevinger(sakId: SakId) =
        inTransaction {
            logger.info("Henter tilbakekrevinger sak=$sakId")
            tilbakekrevingDao.hentTilbakekrevinger(sakId)
        }

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Oppretter tilbakekreving=${kravgrunnlag.kravgrunnlagId} på sak=${kravgrunnlag.sakId}")

            val sak =
                sakDao.hentSak(SakId(kravgrunnlag.sakId.value))
                    ?: throw TilbakekrevingHarMangelException("Tilbakekreving mangler sak")

            tilbakekrevingDao
                .hentTilbakekrevinger(sak.id)
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

            varsleOmUkjenteKlasseTyper(kravgrunnlag, tilbakekreving.id)

            val oppgaveFraBehandlingMedFeilutbetaling =
                oppgaveService
                    .hentOppgaverForSak(sak.id)
                    .filter { it.type == OppgaveType.TILBAKEKREVING }
                    .filter { !it.erAvsluttet() }
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
                oppgaveService.opprettOppgave(
                    referanse = tilbakekreving.id.toString(),
                    sakId = tilbakekreving.sak.id,
                    kilde = OppgaveKilde.TILBAKEKREVING,
                    type = OppgaveType.TILBAKEKREVING,
                    merknad = "Kravgrunnlag mottatt",
                )
            }

            lagreTilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.OPPRETTET)

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(tilbakekreving),
                type = TilbakekrevingHendelseType.OPPRETTET,
            )

            tilbakekreving
        }

    fun endreTilbakekrevingOppgaveStatus(
        sakId: SakId,
        paaVent: Boolean,
    ) {
        inTransaction {
            logger.info("Endrer oppgavestatus for oppgave på tilbakekreving for sakId=$sakId")
            val tilbakekreving = tilbakekrevingDao.hentNyesteTilbakekreving(sakId)

            sjekkAtTilbakekrevingIkkeErFerdigstiltEllerAvbrutt(tilbakekreving)

            val oppgave =
                oppgaveService
                    .hentOppgaverForReferanse(tilbakekreving.id.toString())
                    .firstOrNull { it.type == OppgaveType.TILBAKEKREVING } ?: throw TilbakekrevingFeilTilstandException(
                    "Fant ingen oppgave tilknyttet ${tilbakekreving.id}",
                )

            val merknad = if (paaVent) "Kravgrunnlag er sperret" else "Sperre på kravgrunnlag opphevet"
            val aarsak = if (paaVent) PaaVentAarsak.KRAVGRUNNLAG_SPERRET else null
            oppgaveService.endrePaaVent(oppgaveId = oppgave.id, merknad = merknad, paavent = paaVent, aarsak = aarsak)
        }
    }

    fun avbrytTilbakekreving(
        sakId: SakId,
        merknad: String,
    ): TilbakekrevingBehandling =
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

            oppgaveService.avbrytAapneOppgaverMedReferanse(tilbakekreving.id.toString(), merknad)

            lagreTilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.AVBRUTT)

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(avbruttTilbakekreving),
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

    fun oppdaterKravgrunnlag(
        tilbakekrevingId: UUID,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Oppdaterer kravgrunnlag tilknyttet tilbakekreving $tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)

            sjekkAtTilbakekrevingErUnderBehandling(tilbakekreving)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

            val oppdatertKravgrunnlag =
                runBlocking {
                    tilbakekrevingKlient.hentKravgrunnlag(
                        saksbehandler,
                        tilbakekreving.sak.id,
                        tilbakekreving.tilbakekreving.kravgrunnlag.kravgrunnlagId.value,
                    )
                }

            // Dersom kontrollfeltet er forskjellig betyr det at kravgrunnlaget har blitt endret hos økonomi
            if (oppdatertKravgrunnlag.kontrollFelt.value != tilbakekreving.tilbakekreving.kravgrunnlag.kontrollFelt.value) {
                logger.info("Oppdaterer kravgrunnlag tilknyttet tilbakekreving $tilbakekrevingId")
                val oppdatertTilbakekreving =
                    tilbakekreving
                        .oppdaterKravgrunnlag(
                            oppdatertKravgrunnlag,
                        ).copy(status = TilbakekrevingStatus.UNDER_ARBEID)

                varsleOmUkjenteKlasseTyper(oppdatertKravgrunnlag, tilbakekrevingId)

                tilbakekrevingDao.lagreTilbakekrevingMedNyePerioder(oppdatertTilbakekreving)
            } else {
                logger.info("Kravgrunnlag tilknyttet tilbakekreving $tilbakekrevingId er ikke endret - beholder vurderinger")
                tilbakekreving
            }
        }

    /**
     * Logger dersom vi får andre klassetyper enn YTEL, FEIL og TREK
     */
    private fun varsleOmUkjenteKlasseTyper(
        oppdatertKravgrunnlag: Kravgrunnlag,
        tilbakekrevingId: UUID,
    ) {
        val kjenteKlasseTyper = listOf(KlasseType.YTEL, KlasseType.FEIL, KlasseType.TREK)
        oppdatertKravgrunnlag.perioder.forEach { periode ->
            periode.grunnlagsbeloep.forEach { grunnlagsbeloep ->
                if (grunnlagsbeloep.klasseType !in (kjenteKlasseTyper)) {
                    logger.error(
                        "Fikk en klasseType som ikke var forventet (${grunnlagsbeloep.klasseType}) i tilbakekreving " +
                            "$tilbakekrevingId. I utgangspunktet vil ikke dette påvirke saksbehandlingen sånn " +
                            "det er satt opp nå siden dette bare sendes uendret til vedtak tilsvarende klassetypen FEIL, " +
                            "men følg opp saken for å se at dette blir riktig også i dette tilfellet. Oppdater i så fall " +
                            "kjenteKlasseTyper for å unngå å logge dette på nytt.",
                    )
                }
            }
        }
    }

    fun fattVedtak(
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

        val vedtak =
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

        lagreTilbakekrevingHendelse(tilbakekreving, TilbakekrevingHendelseType.FATTET_VEDTAK, vedtak.id, saksbehandler)

        tilbakekrevinghendelser.sendTilbakekreving(
            statistikkTilbakekreving = tilbakekrevingForStatistikk(oppdatertTilbakekreving),
            type = TilbakekrevingHendelseType.FATTET_VEDTAK,
        )

        oppgaveService.tilAttestering(
            referanse = tilbakekreving.id.toString(),
            type = OppgaveType.TILBAKEKREVING,
            merknad = "Tilbakekreving kan attesteres",
        )

        oppdatertTilbakekreving
    }

    fun attesterVedtak(
        tilbakekrevingId: UUID,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingBehandling =
        inTransaction {
            logger.info("Attesterer vedtak for tilbakekreving=$tilbakekrevingId")
            val tilbakekreving = tilbakekrevingDao.hentTilbakekreving(tilbakekrevingId)
            val sendeBrev = tilbakekreving.sendeBrev

            sjekkForventetStatus(tilbakekreving, TilbakekrevingStatus.FATTET_VEDTAK)
            sjekkAtOppgavenErTildeltSaksbehandlerOgErUnderBehandling(tilbakekreving.id, saksbehandler)

            if (sendeBrev) {
                logger.info("Ferdigstiller vedtaksbrev for tilbakekreving=$tilbakekrevingId")
                runBlocking { brevService.ferdigstillStrukturertBrev(tilbakekrevingId, saksbehandler) }
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

            runBlocking {
                logger.info("Sender vedtak til tilbakekrevingskomponenten for tilbakekreving=$tilbakekrevingId")
                tilbakekrevingKlient.sendTilbakekrevingsvedtak(
                    saksbehandler,
                    tilbakekrevingVedtak(oppdatertTilbakekreving, vedtak),
                )
            }

            lagreTilbakekrevingHendelse(
                tilbakekreving,
                TilbakekrevingHendelseType.ATTESTERT,
                vedtak.id,
                saksbehandler,
                kommentar,
            )

            oppgaveService.ferdigStillOppgaveUnderBehandling(
                referanse = tilbakekreving.id.toString(),
                type = OppgaveType.TILBAKEKREVING,
                saksbehandler = saksbehandler,
            )

            if (sendeBrev) {
                tilbakekrevinghendelser.sendVedtakForJournalfoeringOgDistribusjonAvBrev(tilbakekrevingId, vedtak)
            }

            tilbakekrevinghendelser.sendTilbakekreving(
                statistikkTilbakekreving = tilbakekrevingForStatistikk(oppdatertTilbakekreving),
                type = TilbakekrevingHendelseType.ATTESTERT,
            )

            oppdatertTilbakekreving
        }

    fun underkjennVedtak(
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

            val vedtak =
                runBlocking {
                    vedtakKlient.underkjennVedtakTilbakekreving(
                        tilbakekrevingId = tilbakekreving.id,
                        brukerTokenInfo = saksbehandler,
                    )
                }

            val oppdatertTilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(tilbakekreving.copy(status = TilbakekrevingStatus.UNDERKJENT))

            lagreTilbakekrevingHendelse(
                tilbakekreving = tilbakekreving,
                vedtakId = vedtak.id,
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
                statistikkTilbakekreving = tilbakekrevingForStatistikk(oppdatertTilbakekreving),
                type = TilbakekrevingHendelseType.UNDERKJENT,
            )

            oppdatertTilbakekreving
        }

    private fun lagreTilbakekrevingHendelse(
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
        vedtak: VedtakDto,
    ) = TilbakekrevingVedtak(
        sakId = tilbakekreving.sak.id,
        vedtakId = tilbakekreving.tilbakekreving.kravgrunnlag.vedtakId.value,
        fattetVedtak =
            sjekkIkkeNull(vedtak.vedtakFattet) { "Vedtak ${vedtak.id} i tilbakekreving ${tilbakekreving.id} er ikke fattet" }.let {
                FattetVedtak(
                    saksbehandler = it.ansvarligSaksbehandler,
                    enhet = it.ansvarligEnhet,
                    dato = it.tidspunkt.toLocalDate(),
                )
            },
        aarsak =
            krevIkkeNull(tilbakekreving.tilbakekreving.vurdering?.aarsak) {
                "Årsak for tilbakekreving mangler"
            },
        hjemmel =
            hjemmelFraVurdering(
                krevIkkeNull(tilbakekreving.tilbakekreving.vurdering) {
                    "Kan ikke opprette hjemmel uten vurdering"
                },
            ),
        kravgrunnlagId =
            tilbakekreving.tilbakekreving.kravgrunnlag.kravgrunnlagId.value
                .toString(),
        kontrollfelt = tilbakekreving.tilbakekreving.kravgrunnlag.kontrollFelt.value,
        perioder = tilbakekreving.tilbakekreving.perioder,
    )

    private fun hjemmelFraVurdering(vurdering: TilbakekrevingVurdering): TilbakekrevingHjemmel =
        if (vurdering.vilkaarsresultat == TilbakekrevingVilkaar.IKKE_OPPFYLT) {
            TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD
        } else {
            krevIkkeNull(vurdering.rettsligGrunnlag) {
                "Rettslig grunnlag mangler"
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
        val oppgaveForTilbakekreving = oppgaveService.hentOppgaveForAttesterbarBehandling(tilbakekrevingId.toString())

        if (oppgaveForTilbakekreving?.erIkkeAvsluttet() != true) {
            throw TilbakekrevingFeilTilstandUgyldig(
                code = "OPPGAVE_IKKE_UNDER_BEHANDLING",
                "Oppgaven tilknyttet tilbakekreving $tilbakekrevingId er ikke under behandling",
            )
        }

        if (oppgaveForTilbakekreving.saksbehandler?.ident != saksbehandler.ident()) {
            throw TilbakekrevingFeilTilstandUgyldig(
                code = "SAKSBEHANDLER_IKKE_TILDELT_OPPGAVE",
                "Saksbehandler ${saksbehandler.ident()} er ikke tilknyttet oppgave ${oppgaveForTilbakekreving.id}",
            )
        }
    }
}
