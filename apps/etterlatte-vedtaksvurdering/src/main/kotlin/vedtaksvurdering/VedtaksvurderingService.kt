package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.loependeYtelse.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.etterlatte.vedtaksvurdering.Vedtak as VedtakEntity

class KanIkkeEndreFattetVedtak(vedtak: VedtakEntity) :
    Exception("Vedtak ${vedtak.id} kan ikke oppdateres fordi det allerede er fattet") {
    val vedtakId: Long = vedtak.id
}

class VedtakKanIkkeFattes(vedtak: VedtakEntity) : Exception("Vedtak ${vedtak.id} kan ikke fattes") {
    val vedtakId: Long = vedtak.id
}

class VedtakKanIkkeAttesteresAlleredeAttestert(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke attesteres da det allerede er attestert") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeAttesteresFoerDetFattes(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke attesteres da det ikke er fattet") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeUnderkjennesFoerDetFattes(vedtak: VedtakEntity) :
    Exception("Vedtak ${vedtak.id} kan ikke underkjennes da det ikke er fattet") {
    val vedtakId: Long = vedtak.id
}

class VedtakKanIkkeUnderkjennesAlleredeAttestert(vedtak: VedtakEntity) :
    Exception("Vedtak ${vedtak.id} kan ikke underkjennes da det allerede er attestert") {
    val vedtakId: Long = vedtak.id
}

object BehandlingstilstandException : IllegalStateException("Statussjekk for behandling feilet")

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit,
    private val saksbehandlere: Map<String, String>
) {

    fun lagreIverksattVedtak(behandlingId: UUID) {
        repository.hentVedtak(behandlingId)?.also {
            val iverksattTidspunkt = Tidspunkt.now().toLocalDatetimeUTC()
            repository.lagreIverksattVedtak(behandlingId)
            val detaljertVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
            val statistikkMelding = lagStatistikkMelding(
                vedtakhendelse = KafkaHendelseType.IVERKSATT,
                vedtak = detaljertVedtak,
                tekniskTid = iverksattTidspunkt
            )
            sendToRapid(statistikkMelding, behandlingId)
        }
    }

    fun hentVedtak(behandlingId: UUID): VedtakEntity? {
        return repository.hentVedtak(behandlingId)
    }

    fun vedtakErLoependePaaDato(sakId: Long, dato: LocalDate): LoependeYtelseDTO {
        val alleVedtakForSak = repository.hentVedtakForSak(sakId)

        return Vedtakstidslinje(alleVedtakForSak).erLoependePaa(dato)
    }

    suspend fun opprettEllerOppdaterVedtak(behandlingId: UUID, bruker: Bruker): Vedtak {
        val vedtak = hentVedtak(behandlingId)
        if (vedtak?.vedtakFattet == true) {
            throw KanIkkeEndreFattetVedtak(vedtak)
        }

        val (beregning, vilkaarsvurdering, behandling) = hentDataForVedtak(behandlingId, bruker)
        val virk = vilkaarsvurdering?.virkningstidspunkt?.atDay(1) ?: behandling.virkningstidspunkt!!.dato.atDay(1)

        if (vedtak == null) {
            val sak = behandlingKlient.hentSak(behandling.sak, bruker)
            repository.opprettVedtak(
                behandlingId,
                behandling.sak,
                behandling.soeker!!,
                sak.sakType,
                behandling.behandlingType,
                virk,
                beregning,
                vilkaarsvurdering
            )
        } else {
            repository.oppdaterVedtak(
                behandlingId,
                beregning,
                vilkaarsvurdering,
                virk
            )
        }
        return hentFellesVedtakMedUtbetalingsperioder(behandlingId)!!
    }

    suspend fun hentDataForVedtak(
        behandlingId: UUID,
        bruker: Bruker
    ): Triple<Beregningsresultat?, VilkaarsvurderingDto?, DetaljertBehandling> {
        return coroutineScope {
            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            if (behandling.behandlingType == BehandlingType.MANUELT_OPPHOER) {
                val beregningDTO = beregningKlient.hentBeregning(behandlingId, bruker)
                val beregningsresultat = Beregningsresultat.fraDto(beregningDTO)
                return@coroutineScope Triple(beregningsresultat, null, behandling)
            }

            val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, bruker)

            when (vilkaarsvurdering.resultat?.utfall) {
                VilkaarsvurderingUtfall.IKKE_OPPFYLT -> Triple(null, vilkaarsvurdering, behandling)
                VilkaarsvurderingUtfall.OPPFYLT -> {
                    val beregningDTO = beregningKlient.hentBeregning(behandlingId, bruker)
                    val beregningsResultat = Beregningsresultat.fraDto(beregningDTO)
                    Triple(beregningsResultat, vilkaarsvurdering, behandling)
                }

                null -> throw IllegalArgumentException(
                    "Resultat av vilkårsvurdering er null for behandling $behandlingId"
                )
            }
        }
    }

    fun hentFellesvedtak(behandlingId: UUID): Vedtak? {
        return hentFellesVedtakMedUtbetalingsperioder(behandlingId)
    }

    private fun hentFellesVedtakMedUtbetalingsperioder(behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(behandlingId)?.let {
            it.toDTO(repository.hentUtbetalingsPerioder(it.id))
        }
    }

    suspend fun fattVedtak(
        behandlingId: UUID,
        bruker: Bruker
    ): Vedtak {
        if (!behandlingKlient.fattVedtak(behandlingId, bruker)) {
            throw BehandlingstilstandException
        }

        val v = requireNotNull(hentVedtak(behandlingId)).also {
            if (it.vedtakFattet == true) throw KanIkkeEndreFattetVedtak(it)
        }

        requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            if (it.type == VedtakType.INNVILGELSE) {
                if (it.beregning == null) throw VedtakKanIkkeFattes(v)
            }
            if (it.vilkaarsvurdering == null && it.behandling.type != BehandlingType.MANUELT_OPPHOER) {
                throw VedtakKanIkkeFattes(v)
            }
            val saksbehandler = bruker.ident()
            val saksbehandlerEnhet: String = bruker.saksbehandlerEnhet(saksbehandlere)

            repository.fattVedtak(saksbehandler, saksbehandlerEnhet, behandlingId)
        }

        val fattetVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
        val vedtakHendelse = VedtakHendelse(
            vedtakId = fattetVedtak.vedtakId,
            inntruffet = fattetVedtak.vedtakFattet?.tidspunkt?.toTidspunkt()!!,
            saksbehandler = fattetVedtak.vedtakFattet?.ansvarligSaksbehandler!!
        )
        behandlingKlient.fattVedtak(behandlingId, bruker, vedtakHendelse)

        val fattetTid = fattetVedtak.vedtakFattet?.tidspunkt?.toLocalDateTime()!!
        val statistikkmelding = lagStatistikkMelding(KafkaHendelseType.FATTET, fattetVedtak, fattetTid)
        sendToRapid(statistikkmelding, behandlingId)

        return fattetVedtak
    }

    suspend fun attesterVedtak(
        behandlingId: UUID,
        bruker: Bruker
    ): Vedtak {
        if (!behandlingKlient.attester(behandlingId, bruker)) {
            throw BehandlingstilstandException
        }

        val vedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            requireThat(it.vedtakFattet != null) { VedtakKanIkkeAttesteresFoerDetFattes(it) }
            requireThat(it.attestasjon == null) { VedtakKanIkkeAttesteresAlleredeAttestert(it) }
        }
        val saksbehandler = bruker.ident()
        val saksbehandlerEnhet: String = bruker.saksbehandlerEnhet(saksbehandlere)

        repository.attesterVedtak(
            saksbehandler,
            saksbehandlerEnhet,
            behandlingId,
            vedtak.vedtakId,
            utbetalingsperioderFraVedtak(vedtak)
        )
        val attestertVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
        val vedtakHendelse = VedtakHendelse(
            vedtakId = attestertVedtak.vedtakId,
            inntruffet = attestertVedtak.attestasjon?.tidspunkt?.toTidspunkt()!!,
            saksbehandler = attestertVedtak.attestasjon?.attestant!!
        )
        behandlingKlient.attester(behandlingId, bruker, vedtakHendelse)
        val attestertTid = attestertVedtak.attestasjon?.tidspunkt?.toLocalDateTime()!!
        val message = lagStatistikkMelding(KafkaHendelseType.ATTESTERT, attestertVedtak, attestertTid)
        sendToRapid(message, behandlingId)

        return attestertVedtak
    }

    private fun requireThat(b: Boolean, function: () -> Exception) {
        if (!b) throw function()
    }

    suspend fun underkjennVedtak(
        behandlingId: UUID,
        bruker: Bruker,
        begrunnelse: UnderkjennVedtakClientRequest
    ): VedtakEntity {
        if (!behandlingKlient.underkjenn(behandlingId, bruker)) {
            throw BehandlingstilstandException
        }

        requireNotNull(hentVedtak(behandlingId)).also {
            require(it.vedtakFattet == true) { VedtakKanIkkeUnderkjennesFoerDetFattes(it) }
            require(it.attestant == null) { VedtakKanIkkeUnderkjennesAlleredeAttestert(it) }
        }
        repository.underkjennVedtak(behandlingId)
        val underkjentVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
        val underkjentTid = Tidspunkt.now().toLocalDatetimeUTC()
        val vedtakHendelse = VedtakHendelse(
            vedtakId = underkjentVedtak.vedtakId,
            inntruffet = underkjentTid.toNorskTidspunkt(),
            saksbehandler = bruker.ident(),
            kommentar = begrunnelse.kommentar,
            valgtBegrunnelse = begrunnelse.valgtBegrunnelse
        )

        behandlingKlient.underkjenn(behandlingId, bruker, vedtakHendelse)
        val message = lagStatistikkMelding(KafkaHendelseType.UNDERKJENT, underkjentVedtak, underkjentTid)
        sendToRapid(message, behandlingId)
        return repository.hentVedtak(behandlingId)!!
    }

    private fun utbetalingsperioderFraVedtak(
        vedtak: Vedtak
    ): List<OpprettUtbetalingsperiode> {
        val beregningsperioder = vedtak.beregning?.sammendrag ?: emptyList()
        // TODO vi må se mer på konseptet rundt beregningsperioder ved feks opphør. Bør opphør hoppe over beregning?
        return when (vedtak.type) {
            VedtakType.INNVILGELSE ->
                beregningsperioder
                    .map { OpprettUtbetalingsperiode(it.periode, it.beloep, UtbetalingsperiodeType.UTBETALING) }
                    .sortedBy { it.periode.fom }
            VedtakType.OPPHOER -> listOf(OpprettUtbetalingsperiode(vedtak.virk, null, UtbetalingsperiodeType.OPPHOER))
            VedtakType.AVSLAG -> emptyList()
            VedtakType.ENDRING -> throw Exception("VedtakType ENDRING er ikke støttet")
        }
    }
}

private fun lagStatistikkMelding(vedtakhendelse: KafkaHendelseType, vedtak: Vedtak, tekniskTid: LocalDateTime) =
    JsonMessage.newMessage(
        mapOf(
            EVENT_NAME_KEY to vedtakhendelse.toString(),
            "vedtak" to vedtak,
            TEKNISK_TID_KEY to tekniskTid
        )
    ).toJson()