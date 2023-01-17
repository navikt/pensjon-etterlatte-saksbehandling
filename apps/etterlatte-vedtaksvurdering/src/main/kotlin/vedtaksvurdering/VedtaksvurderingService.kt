package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Beregningsperiode
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.JsonMessage
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
            repository.lagreIverksattVedtak(behandlingId)
        }
    }

    fun hentVedtakBolk(behandlingsidenter: List<UUID>): List<VedtakEntity> {
        return repository.hentVedtakBolk(behandlingsidenter)
    }

    fun hentVedtak(behandlingId: UUID): VedtakEntity? {
        return repository.hentVedtak(behandlingId)
    }

    suspend fun opprettEllerOppdaterVedtak(behandlingId: UUID, accessToken: String): Vedtak {
        val vedtak = hentVedtak(behandlingId)
        if (vedtak?.vedtakFattet == true) {
            throw KanIkkeEndreFattetVedtak(vedtak)
        }

        val (beregning, vilkaarsvurdering, behandling) = hentDataForVedtak(behandlingId, accessToken)
        val virk = vilkaarsvurdering.virkningstidspunkt.atDay(1)

        if (vedtak == null) {
            val sak = behandlingKlient.hentSak(behandling.sak, accessToken)
            repository.opprettVedtak(
                behandlingId,
                behandling.sak,
                behandling.soeker!!,
                sak.sakType,
                behandling.behandlingType!!,
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
        accessToken: String
    ): Triple<Beregningsresultat?, VilkaarsvurderingDto, DetaljertBehandling> {
        return coroutineScope {
            val behandling = async {
                behandlingKlient.hentBehandling(behandlingId, accessToken)
            }
            val vilkaarsvurdering = vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, accessToken)

            when (vilkaarsvurdering.resultat?.utfall) {
                VilkaarsvurderingUtfall.IKKE_OPPFYLT -> Triple(null, vilkaarsvurdering, behandling.await())
                VilkaarsvurderingUtfall.OPPFYLT -> {
                    val beregningDTO = beregningKlient.hentBeregning(behandlingId, accessToken)
                    val beregningsResultat = Beregningsresultat.fraDto(beregningDTO)
                    Triple(beregningsResultat, vilkaarsvurdering, behandling.await())
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
        saksbehandler: String,
        accessToken: String
    ): Vedtak {
        if (!behandlingKlient.fattVedtak(behandlingId, accessToken)) {
            throw BehandlingstilstandException
        }

        val v = requireNotNull(hentVedtak(behandlingId)).also {
            if (it.vedtakFattet == true) throw KanIkkeEndreFattetVedtak(it)
        }

        requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            if (it.type == VedtakType.INNVILGELSE) {
                if (it.beregning == null) throw VedtakKanIkkeFattes(v)
            }
            if (it.vilkaarsvurdering == null) throw VedtakKanIkkeFattes(v)
            val saksbehandlerEnhet = saksbehandlere[saksbehandler]
                ?: throw SaksbehandlerManglerEnhet("Saksbehandler $saksbehandler mangler enhet fra secret")

            repository.fattVedtak(saksbehandler, saksbehandlerEnhet, behandlingId)
        }

        val fattetVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
        val statistikkmelding = lagStatistikkMelding(KafkaHendelseType.FATTET, fattetVedtak)
        sendToRapid(statistikkmelding, behandlingId)
        behandlingKlient.fattVedtak(behandlingId, accessToken, true)

        return fattetVedtak
    }

    suspend fun attesterVedtak(
        behandlingId: UUID,
        saksbehandler: String,
        accessToken: String
    ): Vedtak {
        if (!behandlingKlient.attester(behandlingId, accessToken)) {
            throw BehandlingstilstandException
        }

        val vedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            requireThat(it.vedtakFattet != null) { VedtakKanIkkeAttesteresFoerDetFattes(it) }
            requireThat(it.attestasjon == null) { VedtakKanIkkeAttesteresAlleredeAttestert(it) }
        }

        val saksbehandlerEnhet = saksbehandlere[saksbehandler]
            ?: throw SaksbehandlerManglerEnhet("Saksbehandler $saksbehandler mangler enhet fra secret")

        repository.attesterVedtak(
            saksbehandler,
            saksbehandlerEnhet,
            behandlingId,
            vedtak.vedtakId,
            utbetalingsperioderFraVedtak(vedtak)
        )
        val attestertVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))

        val message = lagStatistikkMelding(KafkaHendelseType.ATTESTERT, attestertVedtak)
        sendToRapid(message, behandlingId)
        behandlingKlient.attester(behandlingId, accessToken, true)

        return attestertVedtak
    }

    private fun requireThat(b: Boolean, function: () -> Exception) {
        if (!b) throw function()
    }

    suspend fun underkjennVedtak(behandlingId: UUID, accessToken: String): VedtakEntity {
        if (!behandlingKlient.underkjenn(behandlingId, accessToken)) {
            throw BehandlingstilstandException
        }

        requireNotNull(hentVedtak(behandlingId)).also {
            require(it.vedtakFattet == true) { VedtakKanIkkeUnderkjennesFoerDetFattes(it) }
            require(it.attestant == null) { VedtakKanIkkeUnderkjennesAlleredeAttestert(it) }
        }
        repository.underkjennVedtak(behandlingId)
        behandlingKlient.underkjenn(behandlingId, accessToken, true)

        return repository.hentVedtak(behandlingId)!!
    }

    private fun utbetalingsperioderFraVedtak(vedtak: Vedtak) =
        utbetalingsperioderFraVedtak(vedtak.type, vedtak.virk, vedtak.beregning?.sammendrag ?: emptyList())

    fun utbetalingsperioderFraVedtak(
        vedtakType: VedtakType,
        virk: Periode,
        beregning: List<Beregningsperiode>
    ): List<Utbetalingsperiode> {
        if (vedtakType != VedtakType.INNVILGELSE) {
            return listOf(
                Utbetalingsperiode(
                    0,
                    virk.copy(tom = null),
                    null,
                    UtbetalingsperiodeType.OPPHOER
                )
            )
        }

        val perioderFraBeregning =
            beregning.map { Utbetalingsperiode(0, it.periode, it.beloep, UtbetalingsperiodeType.UTBETALING) }
                .sortedBy { it.periode.fom }

        val manglendePerioderMellomBeregninger = perioderFraBeregning
            .map { it.periode }
            .zipWithNext()
            .map { requireNotNull(it.first.tom).plusMonths(1) to it.second.fom.minusMonths(1) }
            .filter { !it.second!!.isBefore(it.first) }
            .map { Periode(it.first, it.second) }
            .map { Utbetalingsperiode(0, it, null, UtbetalingsperiodeType.OPPHOER) }
        val fomBeregninger = perioderFraBeregning.firstOrNull()?.periode?.fom
        val manglendeStart = if (fomBeregninger == null || virk.fom.isBefore(fomBeregninger)) {
            Utbetalingsperiode(
                0,
                Periode(virk.fom, fomBeregninger?.minusMonths(1)),
                null,
                UtbetalingsperiodeType.OPPHOER
            )
        } else {
            null
        }
        val manglendeSlutt = perioderFraBeregning.lastOrNull()?.periode?.tom?.let {
            Utbetalingsperiode(
                0,
                Periode(it.plusMonths(1), null),
                null,
                UtbetalingsperiodeType.OPPHOER
            )
        }

        return (perioderFraBeregning + manglendePerioderMellomBeregninger + manglendeStart + manglendeSlutt)
            .filterNotNull()
            .sortedBy { it.periode.fom }
    }

    suspend fun postTilVedtakhendelse(
        behandlingId: UUID,
        accessToken: String,
        hendelse: HendelseType,
        vedtakhendelse: VedtakHendelse
    ) {
        behandlingKlient.postVedtakHendelse(
            vedtakHendelse = vedtakhendelse,
            hendelse = hendelse,
            behandlingId = behandlingId,
            accessToken = accessToken
        )
    }
}

class SaksbehandlerManglerEnhet(message: String) : Exception(message)

private fun lagStatistikkMelding(vedtakhendelse: KafkaHendelseType, vedtak: Vedtak) =
    JsonMessage.newMessage(mapOf(eventNameKey to vedtakhendelse.toString(), "vedtak" to vedtak)).toJson()

data class VedtakHendelse(
    val vedtakId: Long,
    val inntruffet: Tidspunkt,
    val saksbehandler: String? = null,
    val kommentar: String? = null,
    val valgtBegrunnelse: String? = null
)