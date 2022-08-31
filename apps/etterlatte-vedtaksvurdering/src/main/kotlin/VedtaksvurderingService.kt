package no.nav.etterlatte

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.database.Vedtak
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.domene.vedtak.Attestasjon
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.domene.vedtak.Beregningsperiode
import no.nav.etterlatte.domene.vedtak.BilagMedSammendrag
import no.nav.etterlatte.domene.vedtak.Periode
import no.nav.etterlatte.domene.vedtak.Sak
import no.nav.etterlatte.domene.vedtak.Utbetalingsperiode
import no.nav.etterlatte.domene.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.domene.vedtak.VedtakFattet
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*

class KanIkkeEndreFattetVedtak(vedtak: Vedtak) :
    Exception("Vedtak ${vedtak.id} kan ikke oppdateres fordi det allerede er fattet") {
    val vedtakId: Long = vedtak.id
}

class VedtakKanIkkeFattes(vedtak: Vedtak) : Exception("Vedtak ${vedtak.id} kan ikke fattes") {
    val vedtakId: Long = vedtak.id
}

class VedtakKanIkkeAttesteresAlleredeAttestert(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke attesteres da det allerede er attestert") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeAttesteresFoerDetFattes(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke attesteres da det ikke er fattet") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeUnderkjennesFoerDetFattes(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke underkjennes da det ikke er fattet") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtakKanIkkeUnderkjennesAlleredeAttestert(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak) :
    Exception("Vedtak ${vedtak.vedtakId} kan ikke underkjennes da det allerede er attestert") {
    val vedtakId: Long = vedtak.vedtakId
}

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository
) : VedlikeholdService {

    fun lagreAvkorting(sakId: String, behandling: Behandling, fnr: String, avkorting: AvkortingsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandling.id)
        if (vedtak == null) {
            repository.lagreAvkorting(sakId, behandling, fnr, avkorting)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterAvkorting(sakId, behandling.id, avkorting)
        }
    }

    fun lagreVilkaarsresultat(
        sakId: String,
        sakType: String,
        behandling: Behandling,
        fnr: String,
        vilkaarResultat: VilkaarResultat,
        virkningsDato: LocalDate?
    ) {
        val vedtak = repository.hentVedtak(sakId, behandling.id)
        if (vedtak == null) {
            repository.lagreVilkaarsresultat(sakId, sakType, behandling, fnr, vilkaarResultat, virkningsDato)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            migrer(vedtak, fnr, virkningsDato)
            repository.oppdaterVilkaarsresultat(sakId, sakType, behandling.id, vilkaarResultat)
        }
    }

    private fun migrer(vedtak: Vedtak, fnr: String, virkningsDato: LocalDate?) {
        if (vedtak.fnr == null) { // Migrere v2 til v3
            repository.lagreFnr(vedtak.sakId, vedtak.behandlingId, fnr)
        }
        if (vedtak.virkningsDato == null) { // Migrere v3 til v4
            repository.lagreDatoVirk(vedtak.sakId, vedtak.behandlingId, virkningsDato)
        }
    }

    fun lagreBeregningsresultat(
        sakId: String,
        behandling: Behandling,
        fnr: String,
        beregningsResultat: BeregningsResultat
    ) {
        val vedtak = repository.hentVedtak(sakId, behandling.id)
        if (vedtak == null) {
            repository.lagreBeregningsresultat(sakId, behandling, fnr, beregningsResultat)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterBeregningsgrunnlag(sakId, behandling.id, beregningsResultat)
        }
    }

    fun lagreKommerSoekerTilgodeResultat(
        sakId: String,
        behandling: Behandling,
        fnr: String,
        kommerSoekerTilgodeResultat: KommerSoekerTilgode
    ) {
        val vedtak = repository.hentVedtak(sakId, behandling.id)

        if (vedtak == null) {
            repository.lagreKommerSoekerTilgodeResultat(sakId, behandling, fnr, kommerSoekerTilgodeResultat)
        } else {
            if (vedtak.vedtakFattet == true) {
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterKommerSoekerTilgodeResultat(sakId, behandling.id, kommerSoekerTilgodeResultat)
        }
    }

    fun lagreIverksattVedtak(behandlingId: UUID) {
        repository.hentVedtak(behandlingId)?.also {
            repository.lagreIverksattVedtak(behandlingId)
        }
    }

    fun hentVedtakBolk(behandlingsidenter: List<UUID>): List<Vedtak> {
        return repository.hentVedtakBolk(behandlingsidenter)
    }

    fun hentVedtak(sakId: String, behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(sakId, behandlingId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(behandlingId)
    }

    fun hentFellesVedtak(behandlingId: UUID): no.nav.etterlatte.domene.vedtak.Vedtak? {
        // Placeholder for tingene som må inn for å fylle vedtaksmodellen
        return repository.hentVedtak(behandlingId)?.let { vedtak ->
            no.nav.etterlatte.domene.vedtak.Vedtak(
                vedtakId = vedtak.id,
                virk = Periode(
                    vedtak.virkningsDato?.let(YearMonth::from)
                        ?: (
                            vedtak.vilkaarsResultat?.vilkaar
                                ?.find { it.navn == Vilkaartyper.DOEDSFALL_ER_REGISTRERT }?.kriterier
                                ?.find { it.navn == Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL }?.basertPaaOpplysninger
                                ?.find { it.kriterieOpplysningsType == KriterieOpplysningsType.DOEDSDATO }?.opplysning
                                ?.let { it as Doedsdato }?.doedsdato?.with(
                                    TemporalAdjusters.firstDayOfNextMonth()
                                )?.let { YearMonth.of(it.year, it.month) }
                            ) ?: YearMonth.now(),
                    null
                ), // må få inn dette på toppnivå?
                sak = Sak(vedtak.fnr!!, vedtak.sakType!!, vedtak.sakId.toLong()),
                behandling = Behandling(vedtak.behandlingType, behandlingId),
                type = if (vedtak.vilkaarsResultat?.resultat == VurderingsResultat.OPPFYLT) {
                    VedtakType.INNVILGELSE
                } else if (vedtak.behandlingType == BehandlingType.REVURDERING) {
                    VedtakType.OPPHOER
                } else {
                    VedtakType.AVSLAG
                }, // Hvor skal vi bestemme vedtakstype?
                grunnlag = emptyList(), // Ikke lenger aktuell
                vilkaarsvurdering = vedtak.vilkaarsResultat, // Bør periodiseres
                beregning = vedtak.beregningsResultat?.let { bres ->
                    BilagMedSammendrag(
                        objectMapper.valueToTree(bres) as ObjectNode,
                        bres.beregningsperioder.map {
                            Beregningsperiode(
                                Periode(
                                    YearMonth.from(it.datoFOM),
                                    it.datoTOM?.takeIf { it.isBefore(YearMonth.from(LocalDateTime.MAX)) }
                                        ?.let(YearMonth::from)
                                ),
                                BigDecimal.valueOf(it.grunnbelopMnd.toLong())
                            )
                        }
                    )
                }, // sammendraget bør lages av beregning
                avkorting = vedtak.avkortingsResultat?.let { avkorting ->
                    BilagMedSammendrag(
                        objectMapper.valueToTree(avkorting) as ObjectNode,
                        avkorting.beregningsperioder.map {
                            Beregningsperiode(
                                Periode(
                                    YearMonth.from(it.datoFOM),
                                    it.datoTOM?.takeIf { it.isBefore(YearMonth.from(LocalDateTime.MAX)) }
                                        ?.let(YearMonth::from)
                                ),
                                BigDecimal.valueOf(it.belop.toLong())
                            )
                        }
                    )
                }, // sammendraget bør lages av avkorting,
                pensjonTilUtbetaling = repository.hentUtbetalingsPerioder(vedtak.id),
                vedtakFattet = vedtak.saksbehandlerId?.let { ansvarligSaksbehandler ->
                    VedtakFattet(
                        ansvarligSaksbehandler,
                        "0000",
                        vedtak.datoFattet?.atZone(
                            ZoneOffset.UTC
                        )!!
                    )
                }, // logikk inn der fatting skjer. DB utvides med enhet og timestamp?
                attestasjon = vedtak.attestant?.let { attestant ->
                    Attestasjon(
                        attestant,
                        "0000",
                        vedtak.datoattestert!!.atZone(ZoneOffset.UTC)
                    )
                }
            )
        }
    }

    fun fattVedtak(behandlingId: UUID, saksbehandler: String): no.nav.etterlatte.domene.vedtak.Vedtak {
        val v = requireNotNull(hentVedtak(behandlingId)).also {
            if (it.vedtakFattet == true) throw KanIkkeEndreFattetVedtak(it)
        }

        requireNotNull(hentFellesVedtak(behandlingId)).also {
            if (it.type == VedtakType.INNVILGELSE) {
                if (it.beregning == null || it.avkorting == null) throw VedtakKanIkkeFattes(v)
            }
            if (it.vilkaarsvurdering == null) throw VedtakKanIkkeFattes(v)
            repository.fattVedtak(saksbehandler, v.sakId, behandlingId)
        }
        return requireNotNull(hentFellesVedtak(behandlingId))
    }

    fun attesterVedtak(
        behandlingId: UUID,
        saksbehandler: String
    ): no.nav.etterlatte.domene.vedtak.Vedtak {
        val vedtak = requireNotNull(hentFellesVedtak(behandlingId)).also {
            requireThat(it.vedtakFattet != null) { VedtakKanIkkeAttesteresFoerDetFattes(it) }
            requireThat(it.attestasjon == null) { VedtakKanIkkeAttesteresAlleredeAttestert(it) }
        }
        repository.attesterVedtak(
            saksbehandler,
            vedtak.sak.id.toString(),
            behandlingId,
            vedtak.vedtakId,
            utbetalingsperioderFraVedtak(vedtak)
        )
        return requireNotNull(hentFellesVedtak(behandlingId))
    }

    private fun requireThat(b: Boolean, function: () -> Exception) {
        if (!b) throw function()
    }

    fun underkjennVedtak(
        behandlingId: UUID
    ): Vedtak {
        val vedtak = requireNotNull(hentFellesVedtak(behandlingId)).also {
            require(it.vedtakFattet != null) { VedtakKanIkkeUnderkjennesFoerDetFattes(it) }
            require(it.attestasjon == null) { VedtakKanIkkeUnderkjennesAlleredeAttestert(it) }
        }
        repository.underkjennVedtak(vedtak.sak.id.toString(), behandlingId)
        return repository.hentVedtak(behandlingId)!!
    }

    fun utbetalingsperioderFraVedtak(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak) =
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