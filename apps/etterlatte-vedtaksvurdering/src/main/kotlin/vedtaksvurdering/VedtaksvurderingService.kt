package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
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
    private val saksbehandlere: Map<String, String>,
    private val clock: Clock = Clock.systemUTC()
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }

    private fun hentVedtakNonNull(behandlingId: UUID): Vedtak {
        return requireNotNull(hentVedtak(behandlingId)) { "Vedtak for behandling $behandlingId finnes ikke" }
    }

    fun sjekkOmVedtakErLoependePaaDato(sakId: Long, dato: LocalDate): LoependeYtelseDTO {
        logger.info("Sjekker om det finnes løpende vedtak for sak $sakId på dato $dato")
        val alleVedtakForSak = repository.hentVedtakForSak(sakId)
        return Vedtakstidslinje(alleVedtakForSak).erLoependePaa(dato)
    }

    suspend fun opprettEllerOppdaterVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        val vedtak = hentVedtak(behandlingId)

        if (vedtak != null) {
            verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))
        }

        val (behandling, vilkaarsvurdering, beregning) = hentDataForVedtak(behandlingId, bruker)
        val vedtakType = vedtakType(behandling.behandlingType, vilkaarsvurdering)
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato) {
            "Behandling med behandlingId=$behandlingId mangler virkningstidspunkt"
        }

        return if (vedtak != null) {
            logger.info("Oppdaterer vedtak for behandling med behandlingId=$behandlingId")
            oppdaterVedtak(vedtak, vedtakType, virkningstidspunkt, beregning, vilkaarsvurdering)
        } else {
            logger.info("Oppretter vedtak for behandling med behandlingId=$behandlingId")
            opprettVedtak(behandling, vedtakType, virkningstidspunkt, beregning, vilkaarsvurdering)
        }
    }

    suspend fun fattVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        logger.info("Fatter vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.fattVedtak(behandlingId, bruker), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        val fattetVedtak = repository.fattVedtak(
            behandlingId,
            VedtakFattet(bruker.ident(), bruker.saksbehandlerEnhet(saksbehandlere), Tidspunkt.now(clock))
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

    suspend fun attesterVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        verifiserGyldigBehandlingStatus(behandlingKlient.attester(behandlingId, bruker), vedtak)
        verifiserGyldigVedtakStatus(vedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val attestertVedtak = repository.attesterVedtak(
            behandlingId,
            Attestasjon(bruker.ident(), bruker.saksbehandlerEnhet(saksbehandlere), Tidspunkt.now(clock))
        )

        behandlingKlient.attester(
            behandlingId,
            bruker,
            VedtakHendelse(
                vedtakId = attestertVedtak.id,
                inntruffet = attestertVedtak.attestasjon?.tidspunkt!!,
                saksbehandler = attestertVedtak.attestasjon.attestant
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.ATTESTERT,
                vedtak = attestertVedtak,
                tekniskTid = attestertVedtak.attestasjon.tidspunkt.toLocalDatetimeUTC()
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
        beregning: BeregningDTO?,
        vilkaarsvurdering: VilkaarsvurderingDto?
    ): Vedtak {
        val opprettetVedtak = OpprettVedtak(
            soeker = behandling.soeker.let { Foedselsnummer.of(it) },
            sakId = behandling.sak,
            sakType = behandling.sakType,
            behandlingId = behandling.id,
            behandlingType = behandling.behandlingType,
            virkningstidspunkt = virkningstidspunkt,
            status = VedtakStatus.OPPRETTET,
            type = vedtakType,
            beregning = beregning?.toObjectNode(),
            vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
            utbetalingsperioder = opprettUtbetalingsperioder(
                vedtakType = vedtakType,
                virkningstidspunkt = virkningstidspunkt,
                beregning = beregning
            )
        )

        return repository.opprettVedtak(opprettetVedtak)
    }

    private fun oppdaterVedtak(
        eksisterendeVedtak: Vedtak,
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregning: BeregningDTO?,
        vilkaarsvurdering: VilkaarsvurderingDto?
    ): Vedtak {
        val oppdatertVedtak = eksisterendeVedtak.copy(
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning?.toObjectNode(),
            vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
            utbetalingsperioder = opprettUtbetalingsperioder(
                vedtakType = vedtakType,
                virkningstidspunkt = virkningstidspunkt,
                beregning = beregning
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
            BehandlingType.OMREGNING -> VedtakType.ENDRING
            BehandlingType.MANUELT_OPPHOER -> VedtakType.OPPHOER
        }
    }

    private fun opprettUtbetalingsperioder(
        vedtakType: VedtakType,
        virkningstidspunkt: YearMonth,
        beregning: BeregningDTO?
    ): List<Utbetalingsperiode> {
        return when (vedtakType) {
            VedtakType.INNVILGELSE, VedtakType.ENDRING -> {
                val nonNullBeregning = requireNotNull(beregning) { "Mangler beregning" }
                nonNullBeregning.beregningsperioder.map {
                    Utbetalingsperiode(
                        periode = Periode(it.datoFOM, it.datoTOM),
                        beloep = it.utbetaltBeloep.toBigDecimal(),
                        type = UtbetalingsperiodeType.UTBETALING
                    )
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
    ): Triple<DetaljertBehandling, VilkaarsvurderingDto?, BeregningDTO?> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)

            when (behandling.behandlingType) {
                BehandlingType.MANUELT_OPPHOER -> Triple(behandling, null, null)

                BehandlingType.OMREGNING -> {
                    val beregning = beregningKlient.hentBeregning(behandlingId, bruker)
                    Triple(behandling, null, beregning)
                }

                BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                    val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, bruker)
                    when (vilkaarsvurdering?.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> Triple(behandling, vilkaarsvurdering, null)
                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregning = beregningKlient.hentBeregning(behandlingId, bruker)
                            Triple(behandling, vilkaarsvurdering, beregning)
                        }
                        null -> throw Exception("Mangler resultat av vilkårsvurdering for behandling $behandlingId")
                    }
                }
            }
        }
    }

    private fun vilkaarsvurderingUtfallNonNull(vilkaarsvurderingUtfall: VilkaarsvurderingUtfall?) =
        requireNotNull(vilkaarsvurderingUtfall) { "Behandling mangler utfall på vilkårsvurdering" }

    private fun lagStatistikkMelding(vedtakhendelse: KafkaHendelseType, vedtak: Vedtak, tekniskTid: LocalDateTime) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to vedtakhendelse.toString(),
                "vedtak" to vedtak.toDto(),
                TEKNISK_TID_KEY to tekniskTid
            )
        ).toJson()
}

class VedtakTilstandException(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) :
    Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")