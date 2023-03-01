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
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit,
    private val saksbehandlere: Map<String, String>
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
        if (vedtak?.isVedtakFattet() == true) throw KanIkkeEndreFattetVedtak(vedtak)

        val (beregning, vilkaarsvurdering, behandling) = hentDataForVedtak(behandlingId, bruker)
        val virkningstidspunkt = requireNotNull(behandling.virkningstidspunkt?.dato) {
            "Behandling med behandlingId=$behandlingId mangler virkningstidspunkt"
        }

        return if (vedtak == null) {
            logger.info("Oppretter vedtak for behandling med behandlingId=$behandlingId")
            opprettVedtak(behandling, virkningstidspunkt, beregning, vilkaarsvurdering)
        } else {
            // TODO bør denne oppdatere utbetalingslinjer også?
            logger.info("Oppdaterer vedtak for behandling med behandlingId=$behandlingId")
            oppdaterVedtak(vedtak, virkningstidspunkt, beregning, vilkaarsvurdering)
        }
    }

    suspend fun fattVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        logger.info("Fatter vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        val harUgyldigBehandlingTilstand = !behandlingKlient.fattVedtak(behandlingId, bruker)
        if (harUgyldigBehandlingTilstand) throw BehandlingstilstandException(vedtak)

        val vedtakErAlleredeFattet = vedtak.isVedtakFattet()
        if (vedtakErAlleredeFattet) throw VedtakKanIkkeFattesAlleredeFattet(vedtak)

        val innvilgetVedtakManglerBeregning =
            vedtak.vedtakType() == VedtakType.INNVILGELSE && vedtak.beregning == null
        if (innvilgetVedtakManglerBeregning) throw VedtakKanIkkeFattes(vedtak)

        val vedtakManglerVilkaarsvurdering =
            vedtak.vilkaarsvurdering == null && vedtak.behandlingType != BehandlingType.MANUELT_OPPHOER
        if (vedtakManglerVilkaarsvurdering) throw VedtakKanIkkeFattes(vedtak)

        val fattetVedtak = repository.fattVedtak(
            behandlingId,
            VedtakFattet(bruker.ident(), bruker.saksbehandlerEnhet(saksbehandlere), ZonedDateTime.now())
        )

        behandlingKlient.fattVedtak(
            behandlingId = behandlingId,
            bruker = bruker,
            vedtakHendelse = VedtakHendelse(
                vedtakId = fattetVedtak.id,
                inntruffet = fattetVedtak.vedtakFattet?.tidspunkt?.toTidspunkt()!!,
                saksbehandler = fattetVedtak.vedtakFattet.ansvarligSaksbehandler
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.FATTET,
                vedtak = fattetVedtak,
                tekniskTid = fattetVedtak.vedtakFattet.tidspunkt.toLocalDateTime()
            ),
            behandlingId
        )

        return fattetVedtak
    }

    suspend fun attesterVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        logger.info("Attesterer vedtak for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        val harUgyldigBehandlingTilstand = !behandlingKlient.attester(behandlingId, bruker)
        if (harUgyldigBehandlingTilstand) throw BehandlingstilstandException(vedtak)

        val vedtakErIkkeFattet = !vedtak.isVedtakFattet()
        if (vedtakErIkkeFattet) throw VedtakKanIkkeAttesteresFoerDetFattes(vedtak)

        val vedtakErAlleredeAttestert = vedtak.isVedtakAttestert()
        if (vedtakErAlleredeAttestert) throw VedtakKanIkkeAttesteresAlleredeAttestert(vedtak)

        val attestertVedtak = repository.attesterVedtak(
            behandlingId,
            Attestasjon(bruker.ident(), bruker.saksbehandlerEnhet(saksbehandlere), ZonedDateTime.now())
        )

        behandlingKlient.attester(
            behandlingId,
            bruker,
            VedtakHendelse(
                vedtakId = attestertVedtak.id,
                inntruffet = attestertVedtak.attestasjon?.tidspunkt?.toTidspunkt()!!,
                saksbehandler = attestertVedtak.attestasjon.attestant
            )
        )

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.ATTESTERT,
                vedtak = attestertVedtak,
                tekniskTid = attestertVedtak.attestasjon.tidspunkt.toLocalDateTime()
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

        val harUgyldigBehandlingTilstand = !behandlingKlient.underkjenn(behandlingId, bruker)
        if (harUgyldigBehandlingTilstand) throw BehandlingstilstandException(vedtak)

        val vedtakErIkkeFattet = !vedtak.isVedtakFattet()
        if (vedtakErIkkeFattet) throw VedtakKanIkkeUnderkjennesFoerDetFattes(vedtak)

        val vedtakErAlleredeAttestert = vedtak.isVedtakAttestert()
        if (vedtakErAlleredeAttestert) throw VedtakKanIkkeUnderkjennesAlleredeAttestert(vedtak)

        // TODO hvor blir det av begrunnelsen? bør ikke den lagres her et sted?
        val underkjentVedtak = repository.underkjennVedtak(behandlingId)

        val underkjentTid = Tidspunkt.now().toLocalDatetimeUTC()

        behandlingKlient.underkjenn(
            behandlingId,
            bruker,
            VedtakHendelse(
                vedtakId = underkjentVedtak.id,
                inntruffet = underkjentTid.toNorskTidspunkt(),
                saksbehandler = bruker.ident(),
                kommentar = begrunnelse.kommentar,
                valgtBegrunnelse = begrunnelse.valgtBegrunnelse
            )
        )

        sendToRapid(lagStatistikkMelding(KafkaHendelseType.UNDERKJENT, underkjentVedtak, underkjentTid), behandlingId)

        return repository.hentVedtak(behandlingId)!!
    }

    fun iverksattVedtak(behandlingId: UUID): Vedtak {
        logger.info("Setter vedtak til iverksatt for behandling med behandlingId=$behandlingId")
        val vedtak = hentVedtakNonNull(behandlingId)

        // TODO ingen pre-sjekker mot behandling på denne?

        val vedtakErIkkeAttestert = !vedtak.isVedtakAttestert()
        if (vedtakErIkkeAttestert) throw VedtakKanIkkeSettesTilIverksattFoerDetAttesteres(vedtak)

        val vedtakErAlleredeIverksatt = vedtak.isVedtakIverksatt()
        if (vedtakErAlleredeIverksatt) throw VedtakKanIkkeSettesTilIverksattAlleredeIverksatt(vedtak)

        val iverksattVedtak = repository.iverksattVedtak(behandlingId)

        sendToRapid(
            lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.IVERKSATT,
                vedtak = iverksattVedtak,
                tekniskTid = Tidspunkt.now().toLocalDatetimeUTC()
            ),
            behandlingId
        )

        return iverksattVedtak
    }

    private fun opprettVedtak(
        behandling: DetaljertBehandling,
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
            beregning = beregning?.toObjectNode(),
            vilkaarsvurdering = vilkaarsvurdering?.toObjectNode(),
            utbetalingsperioder = opprettUtbetalingsperioder(
                behandlingType = behandling.behandlingType,
                virkningstidspunkt = virkningstidspunkt,
                vilkaarsvurdering = vilkaarsvurdering,
                beregning = beregning
            )
        )

        return repository.opprettVedtak(opprettetVedtak)
    }

    private fun oppdaterVedtak(
        vedtak: Vedtak,
        virkningstidspunkt: YearMonth,
        beregning: BeregningDTO?,
        vilkaarsvurdering: VilkaarsvurderingDto?
    ): Vedtak {
        val oppdatertVedtak = vedtak.copy(
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning?.toObjectNode(),
            vilkaarsvurdering = vilkaarsvurdering?.toObjectNode()
        )
        return repository.oppdaterVedtak(oppdatertVedtak)
    }

    private fun vedtakType(
        vilkaarsvurdering: VilkaarsvurderingDto?,
        behandlingType: BehandlingType
    ): VedtakType {
        return when (behandlingType) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                when (requireNotNull(vilkaarsvurdering?.resultat?.utfall) { "Mangler vilkårsvurdering" }) {
                    VilkaarsvurderingUtfall.OPPFYLT -> VedtakType.INNVILGELSE
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakType.AVSLAG
                }
            }
            BehandlingType.REVURDERING -> {
                when (requireNotNull(vilkaarsvurdering?.resultat?.utfall) { "Mangler vilkårsvurdering" }) {
                    VilkaarsvurderingUtfall.OPPFYLT -> VedtakType.INNVILGELSE
                    VilkaarsvurderingUtfall.IKKE_OPPFYLT -> VedtakType.OPPHOER
                }
            }
            BehandlingType.MANUELT_OPPHOER -> VedtakType.OPPHOER
            BehandlingType.OMREGNING -> VedtakType.INNVILGELSE
        }
    }

    private fun opprettUtbetalingsperioder(
        behandlingType: BehandlingType,
        virkningstidspunkt: YearMonth,
        vilkaarsvurdering: VilkaarsvurderingDto?,
        beregning: BeregningDTO?
    ): List<Utbetalingsperiode> {
        val vedtakType = vedtakType(vilkaarsvurdering, behandlingType)
        return when (vedtakType) {
            VedtakType.INNVILGELSE -> {
                val nonNullBeregning = requireNotNull(beregning) { "Mangler beregning" }
                nonNullBeregning.beregningsperioder.map {
                    Utbetalingsperiode(
                        id = 0,
                        periode = Periode(it.datoFOM, it.datoTOM),
                        beloep = it.utbetaltBeloep.toBigDecimal(),
                        type = UtbetalingsperiodeType.UTBETALING
                    )
                }
            }
            VedtakType.OPPHOER ->
                listOf(
                    Utbetalingsperiode(
                        id = 0,
                        periode = Periode(virkningstidspunkt, null),
                        beloep = BigDecimal.ZERO,
                        type = UtbetalingsperiodeType.OPPHOER
                    )
                )
            VedtakType.AVSLAG -> emptyList()
            VedtakType.ENDRING -> throw NotImplementedError("VedtakType.ENDRING er ikke støttet")
        }
    }

    private suspend fun hentDataForVedtak(
        behandlingId: UUID,
        bruker: Bruker
    ): Triple<BeregningDTO?, VilkaarsvurderingDto?, DetaljertBehandling> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)

            when (behandling.behandlingType) {
                BehandlingType.MANUELT_OPPHOER -> {
                    val beregning = beregningKlient.hentBeregning(behandlingId, bruker)
                    Triple(beregning, null, behandling)
                }
                else -> {
                    val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, bruker)
                    when (vilkaarsvurdering.resultat?.utfall) {
                        VilkaarsvurderingUtfall.IKKE_OPPFYLT -> Triple(null, vilkaarsvurdering, behandling)
                        VilkaarsvurderingUtfall.OPPFYLT -> {
                            val beregning = beregningKlient.hentBeregning(behandlingId, bruker)
                            Triple(beregning, vilkaarsvurdering, behandling)
                        }
                        null -> throw IllegalArgumentException(
                            "Resultat av vilkårsvurdering er null for behandling $behandlingId"
                        )
                    }
                }
            }
        }
    }

    private fun lagStatistikkMelding(vedtakhendelse: KafkaHendelseType, vedtak: Vedtak, tekniskTid: LocalDateTime) =
        JsonMessage.newMessage(
            mapOf(
                EVENT_NAME_KEY to vedtakhendelse.toString(),
                "vedtak" to vedtak.toDto(),
                TEKNISK_TID_KEY to tekniskTid
            )
        ).toJson()
}

class KanIkkeEndreFattetVedtak(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke oppdateres fordi det allerede er fattet")

class VedtakKanIkkeFattes(vedtak: Vedtak) : Exception("Vedtak ${vedtak.id} kan ikke fattes")

class VedtakKanIkkeFattesAlleredeFattet(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke fattes da det allerede er fattet")

class VedtakKanIkkeAttesteresAlleredeAttestert(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke attesteres da det allerede er attestert")

class VedtakKanIkkeAttesteresFoerDetFattes(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke attesteres da det ikke er fattet")

class VedtakKanIkkeUnderkjennesFoerDetFattes(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke underkjennes da det ikke er fattet")

class VedtakKanIkkeUnderkjennesAlleredeAttestert(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke underkjennes da det allerede er attestert")

class VedtakKanIkkeSettesTilIverksattFoerDetAttesteres(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke settes til iverksatt da det ikke er attestert")

class VedtakKanIkkeSettesTilIverksattAlleredeIverksatt(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke settes til iverksatt da det allerede er satt til iverksatt")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")