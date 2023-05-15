package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit,
    private val clock: Clock = utcKlokke()
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak {
        return requireNotNull(hentVedtak(behandlingId)) { "Vedtak for behandling $behandlingId finnes ikke" }
    }

    fun sjekkOmVedtakErLoependePaaDato(sakId: Long, dato: LocalDate): LoependeYtelse {
        logger.info("Sjekker om det finnes løpende vedtak for sak $sakId på dato $dato")
        val alleVedtakForSak = repository.hentVedtakForSak(sakId)
        return Vedtakstidslinje(alleVedtakForSak).erLoependePaa(dato)
    }

    suspend fun opprettEllerOppdaterVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        val vedtak = hentVedtak(behandlingId)

        if (vedtak != null) {
            verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        }

        val (behandling, vilkaarsvurdering, beregningOgAvkorting) = hentDataForVedtak(behandlingId, bruker)
        val vedtakType = vedtakType(behandling.behandlingType, vilkaarsvurdering)
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato) {
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

    suspend fun fattVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        logger.info("Fatter vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.fattVedtak(behandlingId, bruker), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        val sak = behandlingKlient.hentSak(vedtak.sakId, bruker)

        val fattetVedtak = repository.fattVedtak(
            behandlingId,
            VedtakFattet(
                bruker.ident(),
                sak.enhet
                    ?: throw SaksbehandlerManglerEnhetException(
                        "Vedtak mangler ansvarlig enhet vedtakid: ${vedtak.id}"
                    ),
                Tidspunkt.now(clock)
            )
        )

        behandlingKlient.fattVedtak(
            behandlingId = behandlingId,
            bruker = bruker,
            vedtakHendelse = VedtakHendelse(
                vedtakId = fattetVedtak.id,
                inntruffet = fattetVedtak.vedtakFattet?.tidspunkt!!,
                saksbehandler = fattetVedtak.vedtakFattet.ansvarligSaksbehandler
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.FATTET,
                vedtak = fattetVedtak,
                tekniskTid = fattetVedtak.vedtakFattet.tidspunkt.toLocalDatetimeUTC()
            ),
            behandlingId
        )

        return fattetVedtak
    }

    suspend fun attesterVedtak(behandlingId: UUID, kommentar: String, bruker: Bruker): Vedtak {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.attester(behandlingId, bruker), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))
        val (behandling, _, _, sak) = hentDataForVedtak(behandlingId, bruker)

        val attestertVedtak = repository.attesterVedtak(
            behandlingId,
            Attestasjon(
                bruker.ident(),
                sak.enhet
                    ?: throw SaksbehandlerManglerEnhetException(
                        "Vedtak mangler ansvarlig enhet vedtakid: ${vedtak.id}"
                    ),
                Tidspunkt.now(clock)
            )
        )

        behandlingKlient.attester(
            behandlingId,
            bruker,
            VedtakHendelse(
                vedtakId = attestertVedtak.id,
                inntruffet = attestertVedtak.attestasjon?.tidspunkt!!,
                saksbehandler = attestertVedtak.attestasjon.attestant,
                kommentar = kommentar
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.ATTESTERT,
                vedtak = attestertVedtak,
                tekniskTid = attestertVedtak.attestasjon.tidspunkt.toLocalDatetimeUTC(),
                mapOf(
                    SKAL_SENDE_BREV to when (behandling.revurderingsaarsak) {
                        RevurderingAarsak.REGULERING -> false
                        else -> true
                    }
                )
            ),
            behandlingId
        )

        return attestertVedtak
    }

    suspend fun underkjennVedtak(
        behandlingId: UUID,
        bruker: Bruker,
        begrunnelse: UnderkjennVedtakDto
    ): Vedtak {
        logger.info("Underkjenner vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.underkjenn(behandlingId, bruker), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val underkjentVedtak = repository.underkjennVedtak(behandlingId)
        val underkjentTid = Tidspunkt.now(clock)

        behandlingKlient.underkjenn(
            behandlingId,
            bruker,
            VedtakHendelse(
                vedtakId = underkjentVedtak.id,
                inntruffet = underkjentTid,
                saksbehandler = bruker.ident(),
                kommentar = begrunnelse.kommentar,
                valgtBegrunnelse = begrunnelse.valgtBegrunnelse
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                KafkaHendelseType.UNDERKJENT,
                underkjentVedtak,
                underkjentTid.toLocalDatetimeUTC()
            ),
            behandlingId
        )

        return repository.hentVedtak(behandlingId)!!
    }

    fun iverksattVedtak(behandlingId: UUID): Vedtak {
        logger.info("Setter vedtak til iverksatt for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        // TODO trenger vi sjekk mot behandling her?
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.ATTESTERT))

        val iverksattVedtak = repository.iverksattVedtak(behandlingId)

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.IVERKSATT,
                vedtak = iverksattVedtak,
                tekniskTid = Tidspunkt.now(clock).toLocalDatetimeUTC()
            ),
            behandlingId
        )

        return iverksattVedtak
    }

    private fun verifiserGyldigBehandlingStatus(gyldigForOperasjon: Boolean, vedtak: Vedtak) {
        if (!gyldigForOperasjon) throw BehandlingstilstandException(vedtak)
    }

    private fun verifiserGyldigVedtakStatus(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) {
        if (gjeldendeStatus !in forventetStatus) throw VedtakTilstandException(gjeldendeStatus, forventetStatus)
    }

    private fun opprettVedtak(
        behandling: DetaljertBehandling,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: VilkaarsvurderingDto?
    ): Vedtak {
        val opprettetVedtak = OpprettVedtak(
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
            utbetalingsperioder = opprettUtbetalingsperioder(
                vedtakType = vedtakType,
                virkningstidspunkt = virkningstidspunkt,
                beregningOgAvkorting = beregningOgAvkorting,
                behandling.sakType
            )
        )

        return repository.opprettVedtak(opprettetVedtak)
    }

    private fun oppdaterVedtak(
        behandling: DetaljertBehandling,
        eksisterendeVedtak: Vedtak,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregningOgAvkorting: BeregningOgAvkorting?,
        vilkaarsvurdering: VilkaarsvurderingDto?
    ): Vedtak {
        val oppdatertVedtak = eksisterendeVedtak.copy(
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregningOgAvkorting?.beregning?.toObjectNode(),
            avkorting = beregningOgAvkorting?.avkorting?.toObjectNode(),
            vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
            utbetalingsperioder = opprettUtbetalingsperioder(
                vedtakType = vedtakType,
                virkningstidspunkt = virkningstidspunkt,
                beregningOgAvkorting = beregningOgAvkorting,
                behandling.sakType
            )
        )
        return repository.oppdaterVedtak(oppdatertVedtak)
    }

    private fun vedtakType(
        behandlingType: BehandlingType,
        vilkaarsvurdering: VilkaarsvurderingDto?
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
        sakType: SakType
    ): List<Utbetalingsperiode> {
        return when (vedtakType) {
            VedtakType.INNVILGELSE, VedtakType.ENDRING -> {
                // TODO Skal endres når barnepensjon tar i bruk avkorting
                if (sakType == SakType.BARNEPENSJON) {
                    val beregningsperioder = requireNotNull(beregningOgAvkorting?.beregning?.beregningsperioder) {
                        "Mangler beregning"
                    }
                    beregningsperioder.map {
                        Utbetalingsperiode(
                            periode = Periode(it.datoFOM, it.datoTOM),
                            beloep = it.utbetaltBeloep.toBigDecimal(),
                            type = UtbetalingsperiodeType.UTBETALING
                        )
                    }
                } else {
                    val avkortetYtelse = requireNotNull(beregningOgAvkorting?.avkorting?.avkortetYtelse) {
                        "Mangler avkortet ytelse"
                    }
                    avkortetYtelse.map {
                        Utbetalingsperiode(
                            periode = Periode(YearMonth.from(it.fom), YearMonth.from(it.fom)),
                            beloep = it.ytelseEtterAvkorting.toBigDecimal(),
                            type = UtbetalingsperiodeType.UTBETALING
                        )
                    }
                }
            }

            VedtakType.OPPHOER ->
                listOf(
                    Utbetalingsperiode(
                        periode = Periode(virkningstidspunkt, null),
                        beloep = null,
                        type = UtbetalingsperiodeType.OPPHOER
                    )
                )

            VedtakType.AVSLAG -> emptyList()
        }
    }

    private suspend fun hentDataForVedtak(
        behandlingId: UUID,
        bruker: Bruker
    ): VedtakData {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val sak = behandlingKlient.hentSak(behandling.sak, bruker)
            when (behandling.behandlingType) {
                BehandlingType.MANUELT_OPPHOER -> VedtakData(behandling, null, null, sak)

                BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                    val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, bruker)
                    when (vilkaarsvurdering?.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakData(behandling, vilkaarsvurdering, null, sak)
                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregningOgAvkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, bruker)
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

    private fun lagStatistikkMelding(
        vedtakhendelse: KafkaHendelseType,
        vedtak: Vedtak,
        tekniskTid: LocalDateTime,
        extraParams: Map<String, Any> = emptyMap()
    ) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to vedtakhendelse.toString(),
                "vedtak" to vedtak.toDto(),
                TEKNISK_TID_KEY to tekniskTid
            ) + extraParams
        ).toJson()

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak? =
        repository.tilbakestillIkkeIverksatteVedtak(behandlingId)
}

class VedtakTilstandException(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) :
    Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")

class SaksbehandlerManglerEnhetException(message: String) : Exception(message)