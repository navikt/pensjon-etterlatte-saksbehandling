package no.nav.etterlatte

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
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

class VedtakKanIkkeUnderkjennesFoerDetFattes(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke underkjennes da det ikke er fattet") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeUnderkjennesAlleredeAttestert(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke underkjennes da det allerede er attestert") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sendToRapid: (String, UUID) -> Unit
) : VedlikeholdService {

    fun lagreVilkaarsresultat(
        sakType: SakType,
        behandling: Behandling,
        fnr: String,
        vilkaarsvurdering: Vilkaarsvurdering,
        virkningsDato: LocalDate?
    ) {
        val vedtak = repository.hentVedtak(behandling.id)
        if (vedtak == null) {
            repository.lagreVilkaarsresultat(sakType, behandling, fnr, vilkaarsvurdering, virkningsDato)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            migrer(vedtak, fnr, virkningsDato)
            repository.oppdaterVilkaarsresultat(sakType, behandling.id, vilkaarsvurdering)
        }
    }

    private fun migrer(vedtak: VedtakEntity, fnr: String, virkningsDato: LocalDate?) {
        if (vedtak.fnr == null) { // Migrere v2 til v3
            repository.lagreFnr(vedtak.behandlingId, fnr)
        }
        if (vedtak.virkningsDato == null) { // Migrere v3 til v4
            lagreVirkningstidspunkt(vedtak.behandlingId, virkningsDato)
        }
    }

    fun lagreBeregningsresultat(
        behandling: Behandling,
        fnr: String,
        beregningsResultat: BeregningsResultat
    ) {
        val vedtak = repository.hentVedtak(behandling.id)
        if (vedtak == null) {
            repository.lagreBeregningsresultat(behandling, fnr, beregningsResultat)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterBeregningsgrunnlag(behandling.id, beregningsResultat)
        }
    }

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

    suspend fun populerOgHentFellesVedtak(behandlingId: UUID, accessToken: String): Vedtak? {
        val vedtak = hentFellesVedtakMedUtbetalingsperioder(behandlingId)
        if (vedtak?.vedtakFattet != null) {
            return vedtak
        } else {
            val hentetVedtak = coroutineScope {
                val beregningDTO = async { beregningKlient.hentBeregning(behandlingId, accessToken) }
                val vilkaarsvurdering = async {
                    vilkaarsvurderingKlient.hentVilkaarsvurdering(
                        behandlingId,
                        accessToken
                    )
                } // ktlint-disable max-line-length
                val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
                val behandlingMini = async { Behandling(behandling.await().behandlingType!!, behandlingId) }

                val beregningsResultat = BeregningsResultat(
                    id = beregningDTO.await().beregningId,
                    type = Beregningstyper.GP,
                    endringskode = Endringskode.NY,
                    resultat = BeregningsResultatType.BEREGNET,
                    beregningsperioder = beregningDTO.await().beregningsperioder,
                    beregnetDato = LocalDateTime.from(beregningDTO.await().beregnetDato.toNorskTid()),
                    grunnlagVersjon = beregningDTO.await().grunnlagMetadata.versjon
                )
                // TODO: sett inn alt i en sql -> EY-1308
                lagreBeregningsresultat(behandlingMini.await(), behandling.await().soeker!!, beregningsResultat)

                lagreVilkaarsresultat(
                    SakType.BARNEPENSJON, // TODO: SOS, hardkodet i behandling? https://jira.adeo.no/browse/EY-1300
                    behandlingMini.await(),
                    behandling.await().soeker!!,
                    vilkaarsvurdering.await(),
                    vilkaarsvurdering.await().virkningstidspunkt.dato.atDay(1)
                )
                repository.setSakid(sakId = behandling.await().sak, behandlingId = behandlingId)
                hentFellesVedtakMedUtbetalingsperioder(behandlingId)!!
            }

            sendToRapid(lagVedtakHendelseMelding("VEDTAK:BEREGNET", hentetVedtak), behandlingId)
            sendToRapid(lagVedtakHendelseMelding("VEDTAK:VILKAARSVURDERT", hentetVedtak), behandlingId)

            return hentetVedtak
        }
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
        val vedtak = requireNotNull(hentFellesVedtakMedUtbetalingsperioder(behandlingId)).also {
            require(it.vedtakFattet != null) { VedtakKanIkkeUnderkjennesFoerDetFattes(it) }
            require(it.attestasjon == null) { VedtakKanIkkeUnderkjennesAlleredeAttestert(it) }
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

    fun lagreVirkningstidspunkt(behandlingId: UUID, virkningsDato: LocalDate?) =
        repository.lagreDatoVirk(behandlingId, virkningsDato)

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