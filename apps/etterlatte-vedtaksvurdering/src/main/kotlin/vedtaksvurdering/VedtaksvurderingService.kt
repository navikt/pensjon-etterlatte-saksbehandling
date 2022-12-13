package no.nav.etterlatte

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Beregningsperiode
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.JsonMessage
import rapidsandrivers.vedlikehold.VedlikeholdService
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

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit
) : VedlikeholdService {

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

        val (beregning, vilkaarsvurdering, behandling) =
            hentDataForVedtak(behandlingId, accessToken)
        val virk = vilkaarsvurdering.virkningstidspunkt.atDay(1)

        if (vedtak == null) {
            repository.opprettVedtak(
                behandlingId,
                behandling.sak,
                behandling.soeker!!,
                SakType.BARNEPENSJON,
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
        val oppdatertVedtak = hentFellesVedtakMedUtbetalingsperioder(behandlingId)!!

        sendToRapid(lagVedtakHendelseMelding("VEDTAK:BEREGNET", oppdatertVedtak), behandlingId)
        sendToRapid(lagVedtakHendelseMelding("VEDTAK:VILKAARSVURDERT", oppdatertVedtak), behandlingId)
        return oppdatertVedtak
    }

    suspend fun hentDataForVedtak(
        behandlingId: UUID,
        accessToken: String
    ): Triple<BeregningsResultat, Vilkaarsvurdering, DetaljertBehandling> {
        return coroutineScope {
            val beregningDTO = async { beregningKlient.hentBeregning(behandlingId, accessToken) }

            val beregningsResultat = BeregningsResultat(
                id = beregningDTO.await().beregningId,
                type = Beregningstyper.GP,
                endringskode = Endringskode.NY,
                resultat = BeregningsResultatType.BEREGNET,
                beregningsperioder = beregningDTO.await().beregningsperioder,
                beregnetDato = LocalDateTime.from(beregningDTO.await().beregnetDato.toNorskTid()),
                grunnlagVersjon = beregningDTO.await().grunnlagMetadata.versjon
            )

            val vilkaarsvurdering = async {
                vilkaarsvurderingKlient.hentVilkaarsvurdering(
                    behandlingId,
                    accessToken
                )
            }
            val behandling = async {
                behandlingKlient.hentBehandling(behandlingId, accessToken)
            }
            Triple(beregningsResultat, vilkaarsvurdering.await(), behandling.await())
        }
    }

    fun hentFellesvedtak(behandlingId: UUID): Vedtak? {
        return hentFellesVedtakMedUtbetalingsperioder(behandlingId)
    }

    fun hentFellesVedtakMedUtbetalingsperioder(behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(behandlingId)?.let {
            it.toDTO(repository.hentUtbetalingsPerioder(it.id))
        }
    }

    fun fattVedtak(behandlingId: UUID, saksbehandler: String): Vedtak {
        val v = requireNotNull(hentVedtak(behandlingId)).also {
            if (it.vedtakFattet == true) throw KanIkkeEndreFattetVedtak(it)
        }

        requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            if (it.type == VedtakType.INNVILGELSE) {
                if (it.beregning == null) throw VedtakKanIkkeFattes(v)
            }
            if (it.vilkaarsvurdering == null) throw VedtakKanIkkeFattes(v)
            repository.fattVedtak(saksbehandler, behandlingId)
        }

        val fattetVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))
        val fattetVedtakMessage = JsonMessage.newMessage(
            mapOf(
                eventNameKey to "VEDTAK:FATTET",
                "vedtak" to fattetVedtak,
                "behandlingId" to behandlingId,
                "sakId" to fattetVedtak.sak.id,
                "vedtakId" to fattetVedtak.vedtakId,
                "eventtimestamp" to fattetVedtak.vedtakFattet?.tidspunkt?.toTidspunkt()!!,
                "saksbehandler" to fattetVedtak.vedtakFattet?.ansvarligSaksbehandler!!
            )
        )

        sendToRapid(fattetVedtakMessage.toJson(), behandlingId)

        return fattetVedtak
    }

    fun attesterVedtak(
        behandlingId: UUID,
        saksbehandler: String
    ): Vedtak {
        val vedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            requireThat(it.vedtakFattet != null) { VedtakKanIkkeAttesteresFoerDetFattes(it) }
            requireThat(it.attestasjon == null) { VedtakKanIkkeAttesteresAlleredeAttestert(it) }
        }
        repository.attesterVedtak(
            saksbehandler,
            behandlingId,
            vedtak.vedtakId,
            utbetalingsperioderFraVedtak(vedtak)
        )
        val attestertVedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId))

        val message = JsonMessage.newMessage("VEDTAK:ATTESTERT")
            .apply {
                this["vedtak"] = attestertVedtak
                this["vedtakId"] = attestertVedtak.vedtakId
                this["sakId"] = attestertVedtak.sak.id
                this["eventtimestamp"] = attestertVedtak.attestasjon?.tidspunkt?.toTidspunkt()!!
            }
        sendToRapid(message.toJson(), behandlingId)

        return attestertVedtak
    }

    private fun requireThat(b: Boolean, function: () -> Exception) {
        if (!b) throw function()
    }

    fun underkjennVedtak(
        behandlingId: UUID
    ): VedtakEntity {
        requireNotNull(hentVedtak(behandlingId)).also {
            require(it.vedtakFattet != null) { VedtakKanIkkeUnderkjennesFoerDetFattes(it) }
            require(it.attestant == null) { VedtakKanIkkeUnderkjennesAlleredeAttestert(it) }
        }
        repository.underkjennVedtak(behandlingId)
        return repository.hentVedtak(behandlingId)!!
    }

    fun utbetalingsperioderFraVedtak(vedtak: Vedtak) =
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

    override fun slettSak(sakId: Long) {
        repository.slettSak(sakId)
    }
}

private fun lagVedtakHendelseMelding(vedtakhendelse: String, vedtak: Vedtak) =
    JsonMessage.newMessage(
        mapOf(
            eventNameKey to vedtakhendelse,
            "sakId" to vedtak.sak.id,
            "behandlingId" to vedtak.behandling.id,
            "vedtakId" to vedtak.vedtakId,
            "eventtimestamp" to Tidspunkt.now()
        )
    ).toJson()