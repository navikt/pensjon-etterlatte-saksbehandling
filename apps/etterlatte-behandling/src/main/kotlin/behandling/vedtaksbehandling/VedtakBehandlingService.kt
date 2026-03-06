package no.nav.etterlatte.behandling.vedtaksbehandling

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.behandling.etteroppgjoer.revurdering.EtteroppgjoerRevurderingService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.vedtaksbehandling.klienter.SamordningsKlient
import no.nav.etterlatte.grunnlag.GrunnlagVersjonValidering.validerVersjon
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.REVURDERING_AARSAK
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.migrering.KILDE_KEY
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtakBehandlingService(
    private val vedtaksvurderingRepository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val behandlingStatusService: BehandlingStatusService,
    private val behandlingService: BehandlingService,
    private val samordningsKlient: SamordningsKlient,
    private val trygdetidKlient: TrygdetidKlient,
    private val etteroppgjorRevurderingService: EtteroppgjoerRevurderingService,
    private val sakLesDao: SakLesDao,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun sjekkOmVedtakErLoependePaaDato(
        sakId: SakId,
        dato: LocalDate,
    ): LoependeYtelse {
        logger.info("Sjekker om det finnes løpende vedtak for sak $sakId på dato $dato")
        val alleVedtakForSak =
            vedtaksvurderingRepository
                .hentVedtakForSak(sakId)
                .filter { it.vedtakFattet != null && it.attestasjon != null }
        return Vedtakstidslinje(alleVedtakForSak).harLoependeVedtakPaaEllerEtter(dato)
    }

    suspend fun opprettEllerOppdaterVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
        val vedtak = vedtaksvurderingRepository.hentVedtak(behandlingId)

        if (vedtak != null) {
            verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        }
        val (behandling, vilkaarsvurdering, beregningOgAvkorting, _, trygdetider, etteroppgjoerResultat) =
            hentDataForVedtak(behandlingId, brukerTokenInfo)

        validerVersjon(vilkaarsvurdering, beregningOgAvkorting, trygdetider, behandling)

        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val vedtakType =
            vedtakType(
                behandling.behandlingType,
                behandling.revurderingsaarsak,
                vilkaarsvurdering,
                etteroppgjoerResultat,
            )

        return if (vedtak != null) {
            logger.info("Oppdaterer vedtak for behandling med behandlingId=$behandlingId")
            oppdaterVedtak(behandling, vedtak, vedtakType, virkningstidspunkt, beregningOgAvkorting, vilkaarsvurdering)
        } else {
            logger.info("Oppretter vedtak for behandling med behandlingId=$behandlingId")
            opprettVedtak(behandling, vedtakType, virkningstidspunkt, beregningOgAvkorting, vilkaarsvurdering)
        }
    }

    suspend fun fattVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        saksbehandler: String = brukerTokenInfo.ident(),
    ): VedtakOgRapid {
        logger.info("Fatter vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        behandlingStatusService.sjekkOmKanFatteVedtak(behandlingId)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        val (behandling, vilkaarsvurdering, beregningOgAvkorting, sak, trygdetider, etteroppgjoerResultat) =
            hentDataForVedtak(
                behandlingId,
                brukerTokenInfo,
            )
        validerVersjon(vilkaarsvurdering, beregningOgAvkorting, trygdetider, behandling)

        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val vedtakType =
            vedtakType(
                behandling.behandlingType,
                behandling.revurderingsaarsak,
                vilkaarsvurdering,
                etteroppgjoerResultat,
            )

        oppdaterVedtak(
            behandling = behandling,
            eksisterendeVedtak = vedtak,
            vedtakType = vedtakType,
            virkningstidspunkt = virkningstidspunkt,
            beregningOgAvkorting = beregningOgAvkorting,
            vilkaarsvurdering = vilkaarsvurdering,
        )

        val fattetVedtak =
            vedtaksvurderingRepository.inTransaction { tx ->
                fattVedtak(
                    behandlingId,
                    VedtakFattet(
                        saksbehandler,
                        sak.enhet,
                        Tidspunkt.now(),
                    ),
                    tx,
                ).also { fattetVedtak ->
                    val behandling =
                        behandlingService.hentBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
                    behandlingStatusService.settFattetVedtak(
                        behandling = behandling,
                        vedtak =
                            VedtakEndringDTO(
                                sakIdOgReferanse =
                                    SakIdOgReferanse(
                                        sakId = sak.id,
                                        referanse = behandlingId.toString(),
                                    ),
                                vedtakHendelse =
                                    VedtakHendelse(
                                        vedtakId = fattetVedtak.id,
                                        inntruffet = fattetVedtak.vedtakFattet?.tidspunkt!!,
                                        saksbehandler = fattetVedtak.vedtakFattet.ansvarligSaksbehandler,
                                    ),
                                vedtakType = fattetVedtak.type,
                                opphoerFraOgMed = (vedtak.innhold as VedtakInnhold.Behandling).opphoerFraOgMed,
                            ),
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
            }

        beregningOgAvkorting?.let {
            VedtakOgBeregningSammenligner.sammenlign(it, fattetVedtak)
        }

        return VedtakOgRapid(
            fattetVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                vedtak = fattetVedtak.toDto(),
                tekniskTid = fattetVedtak.vedtakFattet!!.tidspunkt,
                behandlingId = behandlingId,
            ),
        )
    }

    suspend fun attesterVedtak(
        behandlingId: UUID,
        kommentar: String,
        brukerTokenInfo: BrukerTokenInfo,
        attestant: String = brukerTokenInfo.ident(),
    ): VedtakOgRapid {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        behandlingStatusService.sjekkOmKanAttestere(behandlingId)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))
        attestantHarAnnenIdentEnnSaksbehandler(vedtak.vedtakFattet!!.ansvarligSaksbehandler, brukerTokenInfo)

        val (behandling, _, _, sak) = hentDataForVedtak(behandlingId, brukerTokenInfo)

        val attestertVedtak =
            vedtaksvurderingRepository.inTransaction { tx ->
                val attestertVedtak =
                    vedtaksvurderingRepository.attesterVedtak(
                        behandlingId,
                        Attestasjon(
                            attestant,
                            sak.enhet,
                            Tidspunkt.now(),
                        ),
                        tx,
                    )
                val behandling = behandlingService.hentBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
                behandlingStatusService.settAttestertVedtak(
                    brukerTokenInfo = brukerTokenInfo,
                    vedtak =
                        VedtakEndringDTO(
                            sakIdOgReferanse =
                                SakIdOgReferanse(
                                    sakId = attestertVedtak.sakId,
                                    referanse = attestertVedtak.behandlingId.toString(),
                                ),
                            vedtakHendelse =
                                VedtakHendelse(
                                    vedtakId = attestertVedtak.id,
                                    inntruffet = attestertVedtak.attestasjon?.tidspunkt!!,
                                    saksbehandler = attestertVedtak.attestasjon.attestant,
                                    kommentar = kommentar,
                                ),
                            vedtakType = attestertVedtak.type,
                        ),
                    behandling = behandling,
                )

                attestertVedtak
            }

        return VedtakOgRapid(
            attestertVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                vedtak = attestertVedtak.toDto(),
                tekniskTid = attestertVedtak.attestasjon!!.tidspunkt,
                behandlingId = behandlingId,
                extraParams =
                    mapOf(
                        SKAL_SENDE_BREV to behandling.sendeBrev,
                        KILDE_KEY to behandling.vedtaksloesning,
                        REVURDERING_AARSAK to behandling.revurderingsaarsak.toString(),
                    ),
            ),
        )
    }

    suspend fun underkjennVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        begrunnelse: UnderkjennVedtakDto,
    ): VedtakOgRapid {
        logger.info("Underkjenner vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        behandlingStatusService.sjekkOmKanReturnereVedtak(behandlingId)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val underkjentTid = Tidspunkt.now()
        val underkjentVedtak =
            vedtaksvurderingRepository.inTransaction { tx ->
                val underkjentVedtak = vedtaksvurderingRepository.underkjennVedtak(behandlingId, tx)
                val behandling = behandlingService.hentBehandling(behandlingId) ?: throw GenerellIkkeFunnetException()
                behandlingStatusService.settReturnertVedtak(
                    behandling = behandling,
                    vedtak =
                        VedtakEndringDTO(
                            sakIdOgReferanse = SakIdOgReferanse(vedtak.sakId, behandlingId.toString()),
                            vedtakHendelse =
                                VedtakHendelse(
                                    vedtakId = underkjentVedtak.id,
                                    inntruffet = underkjentTid,
                                    saksbehandler = brukerTokenInfo.ident(),
                                    kommentar = begrunnelse.kommentar,
                                    valgtBegrunnelse = begrunnelse.valgtBegrunnelse,
                                ),
                            vedtakType = underkjentVedtak.type,
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )

                underkjentVedtak
            }

        return VedtakOgRapid(
            vedtaksvurderingRepository.hentVedtak(behandlingId)!!.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.UNDERKJENT,
                vedtak = underkjentVedtak.toDto(),
                tekniskTid = underkjentTid,
                behandlingId = behandlingId,
            ),
        )
    }

    fun tilSamordningVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Setter vedtak til til_samordning for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT))

        val tilSamordningVedtakLocal =
            vedtaksvurderingRepository.inTransaction { tx ->
                vedtaksvurderingRepository
                    .tilSamordningVedtak(behandlingId, tx = tx)
                    .also {
                        behandlingStatusService.settTilSamordnetVedtak(
                            behandlingId,
                            VedtakHendelse(
                                vedtakId = it.id,
                                inntruffet = Tidspunkt.now(),
                            ),
                        )
                    }
            }

        val tilSamordning =
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.TIL_SAMORDNING,
                vedtak = tilSamordningVedtakLocal.toDto(),
                tekniskTid = Tidspunkt.now(),
                behandlingId = behandlingId,
            )

        return VedtakOgRapid(tilSamordningVedtakLocal.toDto(), tilSamordning)
    }

    suspend fun samordne(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Kaller SAM for å samordne vedtak behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        if (vedtak.isRegulering()) {
            logger.info("Oppretter ikke samordning ved regulering [behandlingId=$behandlingId]")
            return false
        }

        val grunnbeloep = beregningKlient.hentGrunnbeloep(brukerTokenInfo)
        val etterbetaling = vedtak.erVedtakMedEtterbetaling(vedtaksvurderingRepository, grunnbeloep)

        return samordningsKlient
            .samordneVedtak(vedtak, etterbetaling, brukerTokenInfo)
            .also {
                logger.info("Samordning: skal vente? $it [vedtak=${vedtak.id}, behandlingId=$behandlingId]")
            }
    }

    fun samordnetVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid? {
        logger.info("Setter vedtak til samordnet for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        when (vedtak.status) {
            VedtakStatus.TIL_SAMORDNING -> {
                val samordnetVedtakLocal =
                    vedtaksvurderingRepository.inTransaction { tx ->
                        vedtaksvurderingRepository
                            .samordnetVedtak(behandlingId, tx = tx)
                            .also {
                                behandlingStatusService.settSamordnetVedtak(
                                    behandlingId,
                                    VedtakHendelse(
                                        vedtakId = it.id,
                                        inntruffet = Tidspunkt.now(),
                                    ),
                                )
                            }
                    }

                return VedtakOgRapid(
                    samordnetVedtakLocal.toDto(),
                    RapidInfo(
                        vedtakhendelse = VedtakKafkaHendelseHendelseType.SAMORDNET,
                        vedtak = samordnetVedtakLocal.toDto(),
                        tekniskTid = Tidspunkt.now(),
                        behandlingId = behandlingId,
                    ),
                )
            }

            VedtakStatus.IVERKSATT -> {
                logger.warn(
                    "Behandlet svar på samording for vedtak ${vedtak.id}, " +
                        "men vedtaket er allerede iverksatt [behandling=$behandlingId. Skipper",
                )
            }

            else -> {
                verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.TIL_SAMORDNING))
            }
        }
        return null
    }

    suspend fun samordningsinfo(sakId: SakId): List<SamordningsvedtakWrapper> {
        val vedtaksliste = vedtaksvurderingRepository.hentVedtakForSak(sakId)
        return vedtaksliste.firstOrNull()?.let { vedtak ->
            return samordningsKlient
                .hentSamordningsdata(vedtak, alleVedtak = true)
                .map { supplementSamordningsinfo(it, vedtaksliste) }
        } ?: emptyList()
    }

    suspend fun samordningsinfo(behandlingId: UUID): List<SamordningsvedtakWrapper> {
        val vedtak = hentVedtakNonNull(behandlingId)
        return samordningsKlient
            .hentSamordningsdata(vedtak, alleVedtak = false)
            .map { supplementSamordningsinfo(it, listOf(vedtak)) }
    }

    private fun supplementSamordningsinfo(
        samordningsvedtak: Samordningsvedtak,
        vedtaksliste: List<Vedtak>,
    ): SamordningsvedtakWrapper {
        val vedtak =
            vedtaksliste.find { it.id == samordningsvedtak.vedtakId }
                ?: throw UgyldigForespoerselException(
                    code = "VEDTAK_IKKE_FUNNET",
                    detail = "Fant ikke vedtak med id=${samordningsvedtak.vedtakId}",
                )
        return SamordningsvedtakWrapper(samordningsvedtak, vedtak.behandlingId)
    }

    suspend fun oppdaterSamordningsmelding(
        samordningmelding: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        vedtaksvurderingRepository.lagreManuellBehandlingSamordningsmelding(samordningmelding, brukerTokenInfo)

        try {
            samordningsKlient.oppdaterSamordningsmelding(samordningmelding, brukerTokenInfo)
        } catch (e: Exception) {
            vedtaksvurderingRepository.slettManuellBehandlingSamordningsmelding(samordningmelding.samId)
            throw InternfeilException("Klarte ikke å oppdatere samordningsmelding", e)
        }
    }

    fun iverksattVedtak(behandlingId: UUID): VedtakOgRapid {
        logger.info("Setter vedtak til iverksatt for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT, VedtakStatus.SAMORDNET))
        val iverksattVedtak =
            vedtaksvurderingRepository.inTransaction { tx ->
                val iverksattVedtakLocal =
                    vedtaksvurderingRepository.iverksattVedtak(behandlingId, tx = tx).also {
                        runBlocking {
                            behandlingStatusService.settIverksattVedtak(
                                behandlingId,
                                VedtakHendelse(
                                    vedtakId = it.id,
                                    inntruffet = Tidspunkt.now(),
                                ),
                            )
                        }
                    }
                iverksattVedtakLocal
            }

        return VedtakOgRapid(
            iverksattVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.IVERKSATT,
                vedtak = iverksattVedtak.toDto(),
                tekniskTid = Tidspunkt.now(),
                behandlingId = behandlingId,
            ),
        )
    }

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak =
        krevIkkeNull(vedtaksvurderingRepository.hentVedtak(behandlingId)) {
            "Vedtak for behandling $behandlingId finnes ikke"
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

    private fun opprettVedtak(
        behandling: DetaljertBehandling,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: Vilkaarsvurdering?,
    ): Vedtak {
        val opphoerFraOgMed = utledOpphoerFraOgMed(vedtakType, virkningstidspunkt, behandling)
        val opprettetVedtak =
            OpprettVedtak(
                soeker = behandling.soeker.let { Folkeregisteridentifikator.of(it) },
                sakId = behandling.sak,
                sakType = behandling.sakType,
                behandlingId = behandling.id,
                status = VedtakStatus.OPPRETTET,
                type = vedtakType,
                innhold =
                    VedtakInnhold.Behandling(
                        behandlingType = behandling.behandlingType,
                        virkningstidspunkt = virkningstidspunkt,
                        beregning = beregningOgAvkorting?.beregning?.toObjectNode(),
                        avkorting = beregningOgAvkorting?.avkorting?.toObjectNode(),
                        vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
                        utbetalingsperioder =
                            opprettUtbetalingsperioder(
                                vedtakType = vedtakType,
                                beregningOgAvkorting = beregningOgAvkorting,
                                sakType = behandling.sakType,
                                opphoerFraOgMed = opphoerFraOgMed,
                            ),
                        revurderingAarsak = behandling.revurderingsaarsak,
                        revurderingInfo = behandling.revurderingInfo?.toJsonNode(),
                        opphoerFraOgMed = opphoerFraOgMed,
                    ),
            )

        return vedtaksvurderingRepository.opprettVedtak(opprettetVedtak)
    }

    private fun oppdaterVedtak(
        behandling: DetaljertBehandling,
        eksisterendeVedtak: Vedtak,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: Vilkaarsvurdering?,
    ): Vedtak {
        val opphoerFraOgMed = utledOpphoerFraOgMed(vedtakType, virkningstidspunkt, behandling)
        val oppdatertVedtak =
            eksisterendeVedtak.copy(
                type = vedtakType,
                innhold =
                    (eksisterendeVedtak.innhold as VedtakInnhold.Behandling).copy(
                        virkningstidspunkt = virkningstidspunkt,
                        beregning = beregningOgAvkorting?.beregning?.toObjectNode(),
                        avkorting = beregningOgAvkorting?.avkorting?.toObjectNode(),
                        vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
                        utbetalingsperioder =
                            opprettUtbetalingsperioder(
                                vedtakType = vedtakType,
                                beregningOgAvkorting = beregningOgAvkorting,
                                sakType = behandling.sakType,
                                opphoerFraOgMed = opphoerFraOgMed,
                            ),
                        revurderingInfo = behandling.revurderingInfo?.toJsonNode(),
                        opphoerFraOgMed = opphoerFraOgMed,
                    ),
            )
        return vedtaksvurderingRepository.oppdaterVedtak(oppdatertVedtak)
    }

    private fun utledOpphoerFraOgMed(
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        behandling: DetaljertBehandling,
    ) = when (vedtakType) {
        VedtakType.OPPHOER -> {
            virkningstidspunkt
        }

        VedtakType.AVSLAG -> {
            behandling.opphoerFraOgMed
        }

        else -> {
            if (virkningstidspunkt == behandling.opphoerFraOgMed) {
                throw VirkningstidspunktOgOpphoerFomPaaSammeDatoException()
            } else if (behandling.opphoerFraOgMed != null && virkningstidspunkt > behandling.opphoerFraOgMed) {
                throw VirkningstidspunktEtterOpphoerException()
            } else {
                behandling.opphoerFraOgMed
            }
        }
    }

    private fun vedtakType(
        behandlingType: BehandlingType,
        revurderingaarsak: Revurderingaarsak?,
        vilkaarsvurdering: Vilkaarsvurdering?,
        etteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto?,
    ): VedtakType =
        when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                when (vilkaarsvurderingUtfallNonNull(vilkaarsvurdering?.resultat?.utfall)) {
                    VilkaarsvurderingUtfall.OPPFYLT -> VedtakType.INNVILGELSE
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakType.AVSLAG
                }
            }

            BehandlingType.REVURDERING -> {
                when (vilkaarsvurderingUtfallNonNull(vilkaarsvurdering?.resultat?.utfall)) {
                    VilkaarsvurderingUtfall.OPPFYLT -> {
                        val resultatIngenEndring =
                            etteroppgjoerResultat?.resultatType == EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING ||
                                etteroppgjoerResultat?.resultatType == EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING

                        if (revurderingaarsak == Revurderingaarsak.ETTEROPPGJOER && resultatIngenEndring) {
                            VedtakType.INGEN_ENDRING
                        } else {
                            VedtakType.ENDRING
                        }
                    }

                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> {
                        if (revurderingaarsak == Revurderingaarsak.NY_SOEKNAD) {
                            VedtakType.AVSLAG
                        } else {
                            VedtakType.OPPHOER
                        }
                    }
                }
            }
        }

    private fun opprettUtbetalingsperioder(
        vedtakType: VedtakType,
        beregningOgAvkorting: BeregningOgAvkorting?,
        sakType: SakType,
        opphoerFraOgMed: YearMonth?,
    ): List<Utbetalingsperiode> {
        val perioderMedUtbetaling =
            when (vedtakType) {
                VedtakType.INNVILGELSE, VedtakType.ENDRING -> {
                    when (sakType) {
                        SakType.BARNEPENSJON -> {
                            val beregningsperioder =
                                krevIkkeNull(beregningOgAvkorting?.beregning?.beregningsperioder) {
                                    "Mangler beregning"
                                }
                            beregningsperioder.map {
                                Utbetalingsperiode(
                                    periode =
                                        Periode(
                                            fom = it.datoFOM,
                                            tom = it.datoTOM ?: opphoerFraOgMed?.minusMonths(1),
                                        ),
                                    beloep = it.utbetaltBeloep.toBigDecimal(),
                                    type = UtbetalingsperiodeType.UTBETALING,
                                    regelverk = it.regelverk ?: Regelverk.fraDato(it.datoFOM.atDay(1)),
                                )
                            }
                        }

                        SakType.OMSTILLINGSSTOENAD -> {
                            val avkortetYtelse =
                                beregningOgAvkorting?.avkorting?.avkortetYtelse
                                    ?: throw ManglerAvkortetYtelse()

                            avkortetYtelse.map {
                                Utbetalingsperiode(
                                    periode =
                                        Periode(
                                            fom = it.fom,
                                            tom = it.tom ?: opphoerFraOgMed?.minusMonths(1),
                                        ),
                                    beloep = it.ytelseEtterAvkorting.toBigDecimal(),
                                    type = UtbetalingsperiodeType.UTBETALING,
                                    regelverk =
                                        hentRegelverkFraBeregningForPeriode(
                                            beregningOgAvkorting.beregning.beregningsperioder,
                                            it.fom,
                                        ),
                                )
                            }
                        }
                    }
                }

                VedtakType.OPPHOER,
                VedtakType.TILBAKEKREVING,
                VedtakType.AVSLAG,
                VedtakType.AVVIST_KLAGE,
                VedtakType.INGEN_ENDRING,
                -> {
                    emptyList()
                }
            }

        val perioderMedOpphoer =
            if (opphoerFraOgMed != null) {
                listOf(
                    Utbetalingsperiode(
                        periode = Periode(opphoerFraOgMed, null),
                        beloep = null,
                        type = UtbetalingsperiodeType.OPPHOER,
                        regelverk = Regelverk.fraDato(opphoerFraOgMed.atDay(1)),
                    ),
                )
            } else {
                emptyList()
            }

        return perioderMedUtbetaling + perioderMedOpphoer
    }

    private fun hentRegelverkFraBeregningForPeriode(
        beregningsperioder: List<Beregningsperiode>,
        fom: YearMonth,
    ): Regelverk {
        val samsvarendeBeregningsperiode =
            beregningsperioder
                .sortedBy { it.datoFOM }
                .firstOrNull { fom >= it.datoFOM && (it.datoTOM == null || fom <= it.datoTOM) }
                ?: throw InternfeilException("Fant ingen beregningsperioder som samsvarte med avkortingsperiode $fom")

        return samsvarendeBeregningsperiode.regelverk
            ?: Regelverk.fraDato(samsvarendeBeregningsperiode.datoFOM.atDay(1))
    }

    private suspend fun hentDataForVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakData =
        coroutineScope {
            val behandling =
                behandlingService.hentDetaljertBehandling(
                    behandlingId = behandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                ) ?: throw GenerellIkkeFunnetException()
            val sak =
                sakLesDao.hentSak(behandling.sak)
                    ?: throw GenerellIkkeFunnetException()

            val trygdetider =
                trygdetidKlient.hentTrygdetid(
                    behandlingId = behandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                )

            val etteroppgjoerResultat =
                if (behandling.revurderingsaarsak === Revurderingaarsak.ETTEROPPGJOER) {
                    etteroppgjorRevurderingService.hentBeregnetResultatForRevurdering(
                        behandlingId = behandling.id,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                } else {
                    null
                }

            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                    val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandling.id)

                    when (vilkaarsvurdering?.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> {
                            VedtakData(
                                detaljertBehandling = behandling,
                                vilkaarsvurdering = vilkaarsvurdering,
                                beregningOgAvkorting = null,
                                sak = sak,
                                trygdetid = trygdetider,
                                etteroppgjoerResultat = etteroppgjoerResultat,
                            )
                        }

                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregning =
                                beregningKlient.hentBeregning(
                                    behandlingId = behandling.id,
                                    brukerTokenInfo = brukerTokenInfo,
                                ) ?: throw GenerellIkkeFunnetException()

                            val avkorting =
                                if (sak.sakType == SakType.OMSTILLINGSSTOENAD) {
                                    beregningKlient.hentAvkorting(
                                        behandlingId = behandling.id,
                                        brukerTokenInfo = brukerTokenInfo,
                                    )
                                } else {
                                    null
                                }

                            val beregningOgAvkorting =
                                BeregningOgAvkorting(
                                    beregning = beregning,
                                    avkorting = avkorting,
                                )

                            VedtakData(
                                detaljertBehandling = behandling,
                                vilkaarsvurdering = vilkaarsvurdering,
                                beregningOgAvkorting = beregningOgAvkorting,
                                sak = sak,
                                trygdetid = trygdetider,
                                etteroppgjoerResultat = etteroppgjoerResultat,
                            )
                        }

                        null -> {
                            throw InternfeilException("Mangler resultat av vilkårsvurdering for behandling $behandlingId")
                        }
                    }
                }
            }
        }

    private fun vilkaarsvurderingUtfallNonNull(vilkaarsvurderingUtfall: VilkaarsvurderingUtfall?) =
        krevIkkeNull(vilkaarsvurderingUtfall) { "Behandling mangler utfall på vilkårsvurdering" }

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak? =
        vedtaksvurderingRepository.tilbakestillIkkeIverksatteVedtak(behandlingId)

    fun hentIverksatteVedtakISak(sakId: SakId): List<Vedtak> =
        vedtaksvurderingRepository
            .hentVedtakForSak(sakId)
            .filter { it.status == VedtakStatus.IVERKSATT }

    private fun Vedtak.isRegulering() =
        this.innhold is VedtakInnhold.Behandling &&
            Revurderingaarsak.REGULERING == this.innhold.revurderingAarsak

    fun hentVedtakForBehandling(behandlingId: UUID): Vedtak? = vedtaksvurderingRepository.hentVedtak(behandlingId)
}

class VedtakTilstandException(
    gjeldendeStatus: VedtakStatus,
    forventetStatus: List<VedtakStatus>,
) : Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class ManglerAvkortetYtelse :
    UgyldigForespoerselException(
        code = "VEDTAKSVURDERING_MANGLER_AVKORTET_YTELSE",
        detail =
            "Det må legges til inntektsavkorting selv om mottaker ikke har inntekt. Legg inn \"0\" kr i alle felter.",
    )

class VirkningstidspunktEtterOpphoerException :
    UgyldigForespoerselException(
        code = "VIRKNINGSTIDSPUNKT_ETTER_OPPHOER",
        detail = "Virkningstidspunkt kan ikke være senere enn opphør fra og med",
    )

class VirkningstidspunktOgOpphoerFomPaaSammeDatoException :
    UgyldigForespoerselException(
        code = "VIRKNINGSTIDSPUNKT_OG_OPPHOER_FRA_OG_MED_PAA_SAMME_DATO",
        detail = "Virkningstidspunkt og opphør fra og med kan ikke være på samme dato",
    )
