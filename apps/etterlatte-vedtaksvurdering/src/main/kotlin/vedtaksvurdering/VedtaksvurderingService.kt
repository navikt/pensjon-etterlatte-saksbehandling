package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.REVURDERING_AARSAK
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val clock: Clock = utcKlokke(),
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)

    fun hentVedtak(vedtakId: Long): Vedtak? {
        logger.info("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }

    fun hentVedtakSammendrag(behandlingId: UUID): VedtakSammendrag? {
        logger.info("Henter sammendrag for vedtak for behandling$behandlingId")
        return repository.hentVedtakSammendrag(behandlingId)
    }

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak {
        return requireNotNull(hentVedtak(behandlingId)) { "Vedtak for behandling $behandlingId finnes ikke" }
    }

    fun finnFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        virkFom: LocalDate,
    ): List<Vedtak> {
        return repository.hentFerdigstilteVedtak(fnr, virkFom)
    }

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
        val vedtak = hentVedtak(behandlingId)

        if (vedtak != null) {
            verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        }

        val (behandling, vilkaarsvurdering, beregningOgAvkorting) = hentDataForVedtak(behandlingId, brukerTokenInfo)
        val vedtakType = vedtakType(behandling.behandlingType, vilkaarsvurdering)
        val virkningstidspunkt =
            requireNotNull(behandling.virkningstidspunkt?.dato) {
                "Behandling med behandlingId=$behandlingId mangler virkningstidspunkt"
            }

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
    ): VedtakOgRapid {
        logger.info("Fatter vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.kanFatteVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        val sak = behandlingKlient.hentSak(vedtak.sakId, brukerTokenInfo)

        val fattetVedtak =
            repository.inTransaction { tx ->
                val fattetVedtakIntern =
                    fattVedtak(
                        behandlingId,
                        VedtakFattet(
                            brukerTokenInfo.ident(),
                            sak.enhet,
                            Tidspunkt.now(clock),
                        ),
                        tx,
                    ).also { fattetVedtak ->
                        runBlocking {
                            behandlingKlient.fattVedtakBehandling(
                                brukerTokenInfo = brukerTokenInfo,
                                vedtakEndringDTO =
                                    VedtakEndringDTO(
                                        vedtakOppgaveDTO =
                                            VedtakOppgaveDTO(
                                                sakId = sak.id,
                                                referanse = behandlingId.toString(),
                                            ),
                                        vedtakHendelse =
                                            VedtakHendelse(
                                                vedtakId = fattetVedtak.id,
                                                inntruffet = fattetVedtak.vedtakFattet?.tidspunkt!!,
                                                saksbehandler = fattetVedtak.vedtakFattet.ansvarligSaksbehandler,
                                            ),
                                    ),
                            )
                        }
                    }
                VedtakOgRapid(
                    fattetVedtakIntern.toDto(),
                    RapidInfo(
                        vedtakhendelse = VedtakKafkaHendelseType.FATTET,
                        vedtak = fattetVedtakIntern.toDto(),
                        tekniskTid = fattetVedtakIntern.vedtakFattet!!.tidspunkt,
                        behandlingId = behandlingId,
                    ),
                )
            }

        return fattetVedtak
    }

    suspend fun attesterVedtak(
        behandlingId: UUID,
        kommentar: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.kanAttestereVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))
        attestantHarAnnenIdentEnnSaksbehandler(vedtak.vedtakFattet!!.ansvarligSaksbehandler, brukerTokenInfo)

        val (behandling, _, _, sak) = hentDataForVedtak(behandlingId, brukerTokenInfo)
        verifiserGyldigVedtakForRevurdering(behandling, vedtak)
        val attestertVedtak =
            repository.inTransaction { tx ->
                val attestertVedtak =
                    repository.attesterVedtak(
                        behandlingId,
                        Attestasjon(
                            brukerTokenInfo.ident(),
                            sak.enhet,
                            Tidspunkt.now(clock),
                        ),
                        tx,
                    )
                runBlocking {
                    behandlingKlient.attesterVedtak(
                        brukerTokenInfo = brukerTokenInfo,
                        vedtakEndringDTO =
                            VedtakEndringDTO(
                                vedtakOppgaveDTO =
                                    VedtakOppgaveDTO(
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
                            ),
                    )
                }
                attestertVedtak
            }

        return VedtakOgRapid(
            attestertVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseType.ATTESTERT,
                vedtak = attestertVedtak.toDto(),
                tekniskTid = attestertVedtak.attestasjon!!.tidspunkt,
                behandlingId = behandlingId,
                extraParams =
                    mapOf(
                        SKAL_SENDE_BREV to
                            when {
                                behandling.revurderingsaarsak.skalIkkeSendeBrev() -> false
                                else -> true
                            },
                        KILDE_KEY to behandling.kilde,
                        REVURDERING_AARSAK to behandling.revurderingsaarsak.toString(),
                    ),
            ),
        )
    }

    private fun RevurderingAarsak?.skalIkkeSendeBrev() = this != null && !utfall.skalSendeBrev

    suspend fun underkjennVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        begrunnelse: UnderkjennVedtakDto,
    ): VedtakOgRapid {
        logger.info("Underkjenner vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.kanUnderkjenneVedtak(behandlingId, brukerTokenInfo), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val underkjentTid = Tidspunkt.now(clock)
        val underkjentVedtak =
            repository.inTransaction { tx ->
                val underkjentVedtak = repository.underkjennVedtak(behandlingId, tx)
                runBlocking {
                    behandlingKlient.underkjennVedtak(
                        brukerTokenInfo,
                        VedtakEndringDTO(
                            vedtakOppgaveDTO = VedtakOppgaveDTO(vedtak.sakId, behandlingId.toString()),
                            vedtakHendelse =
                                VedtakHendelse(
                                    vedtakId = underkjentVedtak.id,
                                    inntruffet = underkjentTid,
                                    saksbehandler = brukerTokenInfo.ident(),
                                    kommentar = begrunnelse.kommentar,
                                    valgtBegrunnelse = begrunnelse.valgtBegrunnelse,
                                ),
                        ),
                    )
                }
                underkjentVedtak
            }

        return VedtakOgRapid(
            repository.hentVedtak(behandlingId)!!.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseType.UNDERKJENT,
                vedtak = underkjentVedtak.toDto(),
                tekniskTid = underkjentTid,
                behandlingId = behandlingId,
            ),
        )
    }

    suspend fun iverksattVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Setter vedtak til iverksatt for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT))
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
                vedtakhendelse = VedtakKafkaHendelseType.IVERKSATT,
                vedtak = iverksattVedtak.toDto(),
                tekniskTid = Tidspunkt.now(clock),
                behandlingId = behandlingId,
            ),
        )
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

    private fun verifiserGyldigVedtakForRevurdering(
        behandling: DetaljertBehandling,
        vedtak: Vedtak,
    ) {
        if (!behandling.kanVedta(vedtak.type)) {
            throw OpphoersrevurderingErIkkeOpphoersvedtakException(behandling.revurderingsaarsak, vedtak.type)
        }
    }

    private fun opprettVedtak(
        behandling: DetaljertBehandling,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: VilkaarsvurderingDto?,
    ): Vedtak {
        val opprettetVedtak =
            OpprettVedtak(
                soeker = behandling.soeker.let { Folkeregisteridentifikator.of(it) },
                sakId = behandling.sak,
                sakType = behandling.sakType,
                behandlingId = behandling.id,
                behandlingType = behandling.behandlingType,
                virkningstidspunkt = virkningstidspunkt,
                status = VedtakStatus.OPPRETTET,
                type = vedtakType,
                beregning = beregningOgAvkorting?.beregning?.toObjectNode(),
                avkorting = beregningOgAvkorting?.avkorting?.toObjectNode(),
                vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
                utbetalingsperioder =
                    opprettUtbetalingsperioder(
                        vedtakType = vedtakType,
                        virkningstidspunkt = virkningstidspunkt,
                        beregningOgAvkorting = beregningOgAvkorting,
                        behandling.sakType,
                    ),
                revurderingsaarsak = behandling.revurderingsaarsak,
                revurderingInfo = behandling.revurderingInfo,
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
        val oppdatertVedtak =
            eksisterendeVedtak.copy(
                virkningstidspunkt = virkningstidspunkt,
                beregning = beregningOgAvkorting?.beregning?.toObjectNode(),
                avkorting = beregningOgAvkorting?.avkorting?.toObjectNode(),
                vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
                utbetalingsperioder =
                    opprettUtbetalingsperioder(
                        vedtakType = vedtakType,
                        virkningstidspunkt = virkningstidspunkt,
                        beregningOgAvkorting = beregningOgAvkorting,
                        behandling.sakType,
                    ),
                revurderingInfo = behandling.revurderingInfo,
            )
        return repository.oppdaterVedtak(oppdatertVedtak)
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

            BehandlingType.MANUELT_OPPHOER -> VedtakType.OPPHOER
        }
    }

    private fun opprettUtbetalingsperioder(
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        sakType: SakType,
    ): List<Utbetalingsperiode> {
        return when (vedtakType) {
            VedtakType.INNVILGELSE, VedtakType.ENDRING -> {
                if (sakType == SakType.BARNEPENSJON) {
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
                } else {
                    val avkortetYtelse =
                        requireNotNull(beregningOgAvkorting?.avkorting?.avkortetYtelse) {
                            "Mangler avkortet ytelse"
                        }
                    avkortetYtelse.map {
                        Utbetalingsperiode(
                            periode = Periode(YearMonth.from(it.fom), YearMonth.from(it.fom)),
                            beloep = it.ytelseEtterAvkorting.toBigDecimal(),
                            type = UtbetalingsperiodeType.UTBETALING,
                        )
                    }
                }
            }

            VedtakType.OPPHOER ->
                listOf(
                    Utbetalingsperiode(
                        periode = Periode(virkningstidspunkt, null),
                        beloep = null,
                        type = UtbetalingsperiodeType.OPPHOER,
                    ),
                )

            VedtakType.TILBAKEKREVING,
            VedtakType.AVSLAG,
            -> emptyList()
        }
    }

    private suspend fun hentDataForVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakData {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
            val sak = behandlingKlient.hentSak(behandling.sak, brukerTokenInfo)
            when (behandling.behandlingType) {
                BehandlingType.MANUELT_OPPHOER -> VedtakData(behandling, null, null, sak)

                BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                    val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, brukerTokenInfo)
                    when (vilkaarsvurdering?.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakData(behandling, vilkaarsvurdering, null, sak)
                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregningOgAvkorting =
                                beregningKlient.hentBeregningOgAvkorting(
                                    behandlingId,
                                    brukerTokenInfo,
                                    sak.sakType,
                                )
                            VedtakData(behandling, vilkaarsvurdering, beregningOgAvkorting, sak)
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

    fun hentNyesteBehandlingMedResultat(
        sakId: Long,
        resultat: VedtakType,
    ) = repository.hentVedtakForSak(sakId)
        .filter { it.status == VedtakStatus.IVERKSATT }
        .filter { it.type == resultat }
        .maxByOrNull { it.id }

    fun hentIverksatteVedtakISak(sakId: Long): List<Vedtak> {
        return repository.hentVedtakForSak(sakId)
            .filter { it.status == VedtakStatus.IVERKSATT }
    }
}

class VedtakTilstandException(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) :
    Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")

class OpphoersrevurderingErIkkeOpphoersvedtakException(revurderingAarsak: RevurderingAarsak?, vedtakType: VedtakType) :
    IllegalStateException(
        "Vedtaket er av type $vedtakType, men dette er " +
            "ikke gyldig for revurderingen med årsak $revurderingAarsak",
    )

class UgyldigAttestantException(ident: String) :
    IllegalArgumentException("Saksbehandler og attestant må være to forskjellige personer (ident=$ident)")
