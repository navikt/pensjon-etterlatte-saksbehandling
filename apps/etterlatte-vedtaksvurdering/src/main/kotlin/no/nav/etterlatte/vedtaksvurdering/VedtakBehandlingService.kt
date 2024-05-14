package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.SakIdOgReferanse
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.REVURDERING_AARSAK
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.Samordningsvedtak
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.SamordningsvedtakWrapper
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.vedtaksvurdering.grunnlag.GrunnlagVersjonValidering.validerVersjon
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.SamKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.TrygdetidKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtakBehandlingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val samKlient: SamKlient,
    private val trygdetidKlient: TrygdetidKlient,
) {
    private val logger = LoggerFactory.getLogger(VedtakBehandlingService::class.java)

    fun sjekkOmVedtakErLoependePaaDato(
        sakId: Long,
        dato: LocalDate,
    ): LoependeYtelse {
        logger.info("Sjekker om det finnes løpende vedtak for sak $sakId på dato $dato")
        val alleVedtakForSak = repository.hentVedtakForSak(sakId)
        return Vedtakstidslinje(alleVedtakForSak).erLoependePaa(dato)
    }

    suspend fun opprettEllerOppdaterVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
        val vedtak = repository.hentVedtak(behandlingId)

        if (vedtak != null) {
            verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        }
        val (behandling, vilkaarsvurdering, beregningOgAvkorting, _, trygdetider) =
            hentDataForVedtak(behandlingId, brukerTokenInfo)
        validerGrunnlagsversjon(vilkaarsvurdering, beregningOgAvkorting, trygdetider)

        val vedtakType = vedtakType(behandling.behandlingType, vilkaarsvurdering)
        val virkningstidspunkt = behandling.virkningstidspunkt().dato

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

        verifiserGyldigBehandlingStatus(behandlingKlient.kanFatteVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        val (_, vilkaarsvurdering, beregningOgAvkorting, _, trygdetider) = hentDataForVedtak(behandlingId, brukerTokenInfo)
        validerGrunnlagsversjon(vilkaarsvurdering, beregningOgAvkorting, trygdetider)

        val sak = behandlingKlient.hentSak(vedtak.sakId, brukerTokenInfo)

        val fattetVedtak =
            repository.inTransaction { tx ->
                fattVedtak(
                    behandlingId,
                    VedtakFattet(
                        saksbehandler,
                        sak.enhet,
                        Tidspunkt.now(),
                    ),
                    tx,
                ).also { fattetVedtak ->
                    runBlocking {
                        behandlingKlient.fattVedtakBehandling(
                            brukerTokenInfo = brukerTokenInfo,
                            vedtakEndringDTO =
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
                        )
                    }
                }
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

    private fun validerGrunnlagsversjon(
        vilkaarsvurdering: VilkaarsvurderingDto?,
        beregningOgAvkorting: BeregningOgAvkorting?,
        trygdetider: List<TrygdetidDto>,
    ) {
        validerVersjon(vilkaarsvurdering, beregningOgAvkorting, trygdetider)
    }

    suspend fun attesterVedtak(
        behandlingId: UUID,
        kommentar: String,
        brukerTokenInfo: BrukerTokenInfo,
        attestant: String = brukerTokenInfo.ident(),
    ): VedtakOgRapid {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.kanAttestereVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))
        attestantHarAnnenIdentEnnSaksbehandler(vedtak.vedtakFattet!!.ansvarligSaksbehandler, brukerTokenInfo)

        val (behandling, _, _, sak) = hentDataForVedtak(behandlingId, brukerTokenInfo)

        val attestertVedtak =
            repository.inTransaction { tx ->
                val attestertVedtak =
                    repository.attesterVedtak(
                        behandlingId,
                        Attestasjon(
                            attestant,
                            sak.enhet,
                            Tidspunkt.now(),
                        ),
                        tx,
                    )
                runBlocking {
                    behandlingKlient.attesterVedtak(
                        brukerTokenInfo = brukerTokenInfo,
                        vedtakEndringDTO =
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
                    )
                }
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
                        KILDE_KEY to behandling.kilde,
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

        verifiserGyldigBehandlingStatus(behandlingKlient.kanUnderkjenneVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val underkjentTid = Tidspunkt.now()
        val underkjentVedtak =
            repository.inTransaction { tx ->
                val underkjentVedtak = repository.underkjennVedtak(behandlingId, tx)
                runBlocking {
                    behandlingKlient.underkjennVedtak(
                        brukerTokenInfo,
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
                    )
                }
                underkjentVedtak
            }

        return VedtakOgRapid(
            repository.hentVedtak(behandlingId)!!.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.UNDERKJENT,
                vedtak = underkjentVedtak.toDto(),
                tekniskTid = underkjentTid,
                behandlingId = behandlingId,
            ),
        )
    }

    suspend fun tilSamordningVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Setter vedtak til til_samordning for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT))

        val tilSamordningVedtakLocal =
            repository.inTransaction { tx ->
                repository.tilSamordningVedtak(behandlingId, tx = tx)
                    .also {
                        runBlocking {
                            behandlingKlient.tilSamordning(behandlingId, brukerTokenInfo, it.id)
                        }
                    }
            }

        val tilSamordning =
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.TIL_SAMORDNING,
                vedtak = tilSamordningVedtakLocal.toDto(),
                tekniskTid = Tidspunkt.now(),
                behandlingId = behandlingId,
            )

        if (tilSamordningVedtakLocal.isRegulering()) {
            logger.info("Oppretter ikke samordning ved regulering [behandlingId=$behandlingId]")

            val vedtakEtterSvar = samordnetVedtak(behandlingId, brukerTokenInfo, tilSamordningVedtakLocal)!!
            return VedtakOgRapid(vedtakEtterSvar.vedtak, tilSamordning, vedtakEtterSvar.rapidInfo1)
        } else if (!samKlient.samordneVedtak(
                vedtak = tilSamordningVedtakLocal,
                etterbetaling = erVedtakMedEtterbetaling(tilSamordningVedtakLocal, repository),
                brukerTokenInfo = brukerTokenInfo,
            )
        ) {
            logger.info("Svar fra samordning: ikke nødvendig å vente for vedtak=${vedtak.id} [behandlingId=$behandlingId]")

            val vedtakEtterSvar = samordnetVedtak(behandlingId, brukerTokenInfo, tilSamordningVedtakLocal)!!
            return VedtakOgRapid(vedtakEtterSvar.vedtak, tilSamordning, vedtakEtterSvar.rapidInfo1)
        } else {
            logger.info("Svar fra samordning: må vente for vedtak=${vedtak.id} [behandlingId=$behandlingId]")
        }

        return VedtakOgRapid(tilSamordningVedtakLocal.toDto(), tilSamordning)
    }

    fun samordnetVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakTilSamordning: Vedtak? = null,
    ): VedtakOgRapid? {
        logger.info("Setter vedtak til samordnet for behandling med behandlingId=$behandlingId")
        val vedtak = vedtakTilSamordning ?: hentVedtakNonNull(behandlingId)

        when (vedtak.status) {
            VedtakStatus.TIL_SAMORDNING -> {
                val samordnetVedtakLocal =
                    repository.inTransaction { tx ->
                        repository.samordnetVedtak(behandlingId, tx = tx)
                            .also {
                                runBlocking {
                                    behandlingKlient.samordnet(behandlingId, brukerTokenInfo, it.id)
                                }
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

    suspend fun samordningsinfo(sakId: Long): List<SamordningsvedtakWrapper> {
        val vedtaksliste = repository.hentVedtakForSak(sakId)
        return vedtaksliste.firstOrNull()?.let { vedtak ->
            return samKlient.hentSamordningsdata(vedtak, alleVedtak = true)
                .map { supplementSamordningsinfo(it, vedtaksliste) }
        } ?: emptyList()
    }

    suspend fun samordningsinfo(behandlingId: UUID): List<SamordningsvedtakWrapper> {
        val vedtak = hentVedtakNonNull(behandlingId)
        return samKlient.hentSamordningsdata(vedtak, alleVedtak = false)
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

    suspend fun iverksattVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Setter vedtak til iverksatt for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT, VedtakStatus.SAMORDNET))
        val iverksattVedtak =
            repository.inTransaction { tx ->
                val iverksattVedtakLocal =
                    repository.iverksattVedtak(behandlingId, tx = tx).also {
                        runBlocking {
                            behandlingKlient.iverksett(behandlingId, brukerTokenInfo, it.id)
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

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak {
        return requireNotNull(repository.hentVedtak(behandlingId)) { "Vedtak for behandling $behandlingId finnes ikke" }
    }

    private fun verifiserGyldigBehandlingStatus(
        gyldigForOperasjon: Boolean,
        vedtak: Vedtak,
    ) {
        if (!gyldigForOperasjon) throw BehandlingstilstandException(vedtak)
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
        vilkaarsvurdering: VilkaarsvurderingDto?,
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
                                behandling.sakType,
                                opphoerFraOgMed = opphoerFraOgMed,
                            ),
                        revurderingAarsak = behandling.revurderingsaarsak,
                        revurderingInfo = behandling.revurderingInfo,
                        opphoerFraOgMed = opphoerFraOgMed,
                    ),
            )

        return repository.opprettVedtak(opprettetVedtak)
    }

    private fun oppdaterVedtak(
        behandling: DetaljertBehandling,
        eksisterendeVedtak: Vedtak,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: VilkaarsvurderingDto?,
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
                                behandling.sakType,
                                opphoerFraOgMed = opphoerFraOgMed,
                            ),
                        revurderingInfo = behandling.revurderingInfo,
                        opphoerFraOgMed = opphoerFraOgMed,
                    ),
            )
        return repository.oppdaterVedtak(oppdatertVedtak)
    }

    private fun utledOpphoerFraOgMed(
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        behandling: DetaljertBehandling,
    ) = when (vedtakType) {
        VedtakType.OPPHOER -> {
            virkningstidspunkt
        }
        else -> {
            if (virkningstidspunkt == behandling.opphoerFraOgMed) {
                null
            } else {
                behandling.opphoerFraOgMed
            }
        }
    }

    private fun vedtakType(
        behandlingType: BehandlingType,
        vilkaarsvurdering: VilkaarsvurderingDto?,
    ): VedtakType {
        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                when (vilkaarsvurderingUtfallNonNull(vilkaarsvurdering?.resultat?.utfall)) {
                    VilkaarsvurderingUtfall.OPPFYLT -> VedtakType.INNVILGELSE
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakType.AVSLAG
                }
            }

            BehandlingType.REVURDERING -> {
                when (vilkaarsvurderingUtfallNonNull(vilkaarsvurdering?.resultat?.utfall)) {
                    VilkaarsvurderingUtfall.OPPFYLT -> VedtakType.ENDRING
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakType.OPPHOER
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
                                requireNotNull(beregningOgAvkorting?.beregning?.beregningsperioder) {
                                    "Mangler beregning"
                                }
                            beregningsperioder.map {
                                Utbetalingsperiode(
                                    periode = Periode(it.datoFOM, it.datoTOM),
                                    beloep = it.utbetaltBeloep.toBigDecimal(),
                                    type = UtbetalingsperiodeType.UTBETALING,
                                )
                            }
                        }

                        SakType.OMSTILLINGSSTOENAD -> {
                            val avkortetYtelse =
                                beregningOgAvkorting?.avkorting?.avkortetYtelse
                                    ?: throw ManglerAvkortetYtelse()

                            avkortetYtelse.map {
                                Utbetalingsperiode(
                                    periode = Periode(it.fom, it.tom),
                                    beloep = it.ytelseEtterAvkorting.toBigDecimal(),
                                    type = UtbetalingsperiodeType.UTBETALING,
                                )
                            }
                        }
                    }
                }

                VedtakType.OPPHOER,
                VedtakType.TILBAKEKREVING,
                VedtakType.AVSLAG,
                VedtakType.AVVIST_KLAGE,
                -> emptyList()
            }

        val perioderMedOpphoer =
            if (opphoerFraOgMed != null) {
                listOf(
                    Utbetalingsperiode(
                        periode = Periode(opphoerFraOgMed, null),
                        beloep = null,
                        type = UtbetalingsperiodeType.OPPHOER,
                    ),
                )
            } else {
                emptyList()
            }

        return perioderMedUtbetaling + perioderMedOpphoer
    }

    private suspend fun hentDataForVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakData {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            val sak = behandlingKlient.hentSak(behandling.sak, brukerTokenInfo)

            val trygdetider = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)

            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                    val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, brukerTokenInfo)
                    when (vilkaarsvurdering?.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakData(behandling, vilkaarsvurdering, null, sak, trygdetider)
                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregningOgAvkorting =
                                beregningKlient.hentBeregningOgAvkorting(
                                    behandlingId,
                                    brukerTokenInfo,
                                    sak.sakType,
                                )
                            VedtakData(behandling, vilkaarsvurdering, beregningOgAvkorting, sak, trygdetider)
                        }

                        null -> throw Exception("Mangler resultat av vilkårsvurdering for behandling $behandlingId")
                    }
                }
            }
        }
    }

    private fun vilkaarsvurderingUtfallNonNull(vilkaarsvurderingUtfall: VilkaarsvurderingUtfall?) =
        requireNotNull(vilkaarsvurderingUtfall) { "Behandling mangler utfall på vilkårsvurdering" }

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak? = repository.tilbakestillIkkeIverksatteVedtak(behandlingId)

    fun hentIverksatteVedtakISak(sakId: Long): List<Vedtak> {
        return repository.hentVedtakForSak(sakId)
            .filter { it.status == VedtakStatus.IVERKSATT }
    }

    private fun Vedtak.isRegulering() =
        this.innhold is VedtakInnhold.Behandling &&
            Revurderingaarsak.REGULERING == this.innhold.revurderingAarsak
}

class VedtakTilstandException(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) :
    Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")

class ManglerAvkortetYtelse :
    UgyldigForespoerselException(
        code = "VEDTAKSVURDERING_MANGLER_AVKORTET_YTELSE",
        detail =
            "Det må legges til inntektsavkorting selv om mottaker ikke har inntekt. Legg inn \"0\" kr i alle felter.",
    )

class ForeldreloesTrygdetid(behandlingId: UUID) : UgyldigForespoerselException(
    code = "FORELDRELOES_TRYGDETID",
    detail = "Flere avdødes trygdetid er ikke støttet for vedtaksvurdering $behandlingId",
)
