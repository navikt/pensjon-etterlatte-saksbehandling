package no.nav.etterlatte

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.database.Vedtak
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.domene.vedtak.*
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.*
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.helse.rapids_rivers.JsonMessage.Companion.newMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class KanIkkeEndreFattetVedtak(vedtak: Vedtak): Exception("Vedtak ${vedtak.id} kan ikke oppdateres fordi det allerede er fattet"){
    val vedtakId: Long = vedtak.id
}

class VedtaksvurderingService(private val repository: VedtaksvurderingRepository, private val rapid: AtomicReference<MessageContext> = AtomicReference()) {
    companion object{
        val logger: Logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)
    }
    fun lagreAvkorting(sakId: String, behandlingId: UUID, fnr: String, avkorting: AvkortingsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreAvkorting(sakId, behandlingId, fnr, avkorting)
        } else {
            if(vedtak.vedtakFattet == true){
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterAvkorting(sakId, behandlingId, avkorting)
        }
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingId: UUID, fnr: String, vilkaarResultat: VilkaarResultat, virkningsDato: LocalDate) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkaarResultat, virkningsDato)
        } else {
            if(vedtak.vedtakFattet == true){
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            migrer(vedtak, fnr, virkningsDato)
            repository.oppdaterVilkaarsresultat(sakId, behandlingId, vilkaarResultat)
        }
    }

    private fun migrer(vedtak: Vedtak, fnr: String, virkningsDato: LocalDate) {
        if(vedtak.fnr == null){ //Migrere v2 til v3
            repository.lagreFnr(vedtak.sakId, vedtak.behandlingId, fnr)
        }
        if(vedtak.virkningsDato == null){ //Migrere v3 til v4
            repository.lagreDatoVirk(vedtak.sakId, vedtak.behandlingId, virkningsDato)
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingId: UUID, fnr: String, beregningsResultat: BeregningsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreBeregningsresultat(sakId, behandlingId, fnr, beregningsResultat)
        } else {
            if(vedtak.vedtakFattet == true){
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterBeregningsgrunnlag(sakId, behandlingId, beregningsResultat)
        }
    }

    fun lagreKommerSoekerTilgodeResultat(sakId: String, behandlingId: UUID, fnr: String, kommerSoekerTilgodeResultat: KommerSoekerTilgode) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)

        if(vedtak == null) {
            repository.lagreKommerSoekerTilgodeResultat(sakId, behandlingId, fnr, kommerSoekerTilgodeResultat)
        } else {
            if(vedtak.vedtakFattet == true){
                throw KanIkkeEndreFattetVedtak(vedtak)
            }
            repository.oppdaterKommerSoekerTilgodeResultat(sakId, behandlingId, kommerSoekerTilgodeResultat)
        }
    }

    fun hentVedtak(sakId: String, behandlingId: UUID): Vedtak? {
        return repository.hentVedtak(sakId, behandlingId)
    }

    fun hentFellesVedtak(sakId: String, behandlingId: UUID): no.nav.etterlatte.domene.vedtak.Vedtak? {
        //Placeholder for tingene som må inn for å fylle vedtaksmodellen
        return repository.hentVedtak(sakId, behandlingId)?.let {
            Vedtak(
                it.id, //må få med vedtak-id fra databasen
                Periode(
                    it.virkningsDato?.let(YearMonth::from) ?: (it.vilkaarsResultat?.vilkaar?.find { it.navn == Vilkaartyper.DOEDSFALL_ER_REGISTRERT }?.kriterier?.find { it.navn == Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL }?.basertPaaOpplysninger?.find { it.kriterieOpplysningsType == KriterieOpplysningsType.DOEDSDATO }?.opplysning?.let { it as Doedsdato }?.doedsdato?.with(
                    TemporalAdjusters.firstDayOfNextMonth())?.let { YearMonth.of(it.year, it.month) }) ?: YearMonth.now(), null), //må få inn dette på toppnivå?
                Sak(it.fnr!!, "BARNEPENSJON", sakId.toLong()), //mer sakinfo inn i prosess og vedtaksammendrag
                Behandling(BehandlingType.FORSTEGANGSBEHANDLING, behandlingId), // Behandlingsinfo må lagres
                if(it.vilkaarsResultat?.resultat == VurderingsResultat.OPPFYLT) VedtakType.INNVILGELSE else VedtakType.AVSLAG, //Hvor skal vi bestemme vedtakstype?
                emptyList(), //Ikke lenger aktuell
                it.vilkaarsResultat, //Bør periodiseres
                BilagMedSammendrag(objectMapper.valueToTree(it.beregningsResultat) as ObjectNode, it.beregningsResultat?.beregningsperioder?.map { Beregningsperiode(Periode(
                    YearMonth.from(it.datoFOM), YearMonth.from(it.datoTOM)), BigDecimal.valueOf(it.belop.toLong())
                ) }?: emptyList()), // sammendraget bør lages av beregning
                BilagMedSammendrag(objectMapper.valueToTree(it.avkortingsResultat) as ObjectNode, it.avkortingsResultat?.beregningsperioder?.map { Beregningsperiode(Periode(
                    YearMonth.from(it.datoFOM), YearMonth.from(it.datoTOM)), BigDecimal.valueOf(it.belop.toLong())
                ) }?: emptyList()), // sammendraget bør lages av avkorting,
                repository.hentUtbetalingsPerioder(it.id),
                it.saksbehandlerId?.let { ansvarligSaksbehadnlier -> VedtakFattet(ansvarligSaksbehadnlier, "0000", it.datoFattet?.atZone(
                    ZoneOffset.UTC)!!) }, //logikk inn der fatting skjer. DB utvides med enhet og timestamp?
                it.attestant?.let {attestant-> Attestasjon(attestant, "0000",  it.datoattestert!!.atZone(ZoneOffset.UTC)) }
            )
        }
    }

    fun fattVedtakSaksbehandler(sakId: String, behandlingId: UUID, saksbehandler: String){
        val vedtak = requireNotNull( hentVedtak(sakId, behandlingId))
        rapid.get().publish( vedtak.sakId,
            newMessage(
                mapOf(
                    "@event" to "SAKSBEHANDLER:FATT_VEDTAK",
                    "@sakId" to sakId.toLong(),
                    "@vedtakId" to vedtak.id,
                    "@behandlingId" to behandlingId.toString(),
                    "@saksbehandler" to saksbehandler,
                )
            ).toJson()
        )
    }

    fun attesterVedtakSaksbehandler(sakId: String, behandlingId: UUID, saksbehandler: String) {
        val vedtak = requireNotNull( hentVedtak(sakId, behandlingId))
        rapid.get().publish( vedtak.sakId,
            newMessage(
                mapOf(
                    "@event" to "SAKSBEHANDLER:ATTESTER_VEDTAK",
                    "@sakId" to sakId.toLong(),
                    "@vedtakId" to vedtak.id,
                    "@behandlingId" to behandlingId.toString(),
                    "@saksbehandler" to saksbehandler,
                )
            ).toJson()
        )

    }

    fun fattVedtak(sakId: String, behandlingId: UUID, saksbehandler: String): no.nav.etterlatte.domene.vedtak.Vedtak {
        requireNotNull( hentFellesVedtak(sakId, behandlingId)).also {
            require(it.vedtakFattet == null)
            require(it.type == VedtakType.INNVILGELSE)
            require(it.vilkaarsvurdering != null)
            require(it.beregning != null)
            require(it.avkorting != null)
        }
        repository.fattVedtak(saksbehandler, sakId, behandlingId)
        return requireNotNull( hentFellesVedtak(sakId, behandlingId))
    }


    fun attesterVedtak(sakId: String, behandlingId: UUID, saksbehandler: String): no.nav.etterlatte.domene.vedtak.Vedtak {
        val vedtak = requireNotNull( hentFellesVedtak(sakId, behandlingId)).also {
            require(it.vedtakFattet != null)
            require(it.attestasjon == null)
        }
        repository.attesterVedtak(saksbehandler, sakId, behandlingId, vedtak.vedtakId, utbetalingsperioderFraVedtak(vedtak))
        return requireNotNull( hentFellesVedtak(sakId, behandlingId))

    }

    fun utbetalingsperioderFraVedtak(vedtak: no.nav.etterlatte.domene.vedtak.Vedtak): List<Utbetalingsperiode> {
        if(vedtak.type != VedtakType.INNVILGELSE) return listOf(Utbetalingsperiode(0, vedtak.virk.copy(tom = null), null, UtbetalingsperiodeType.OPPHOER))

        val perioderFraBeregning = vedtak.beregning?.sammendrag?.map { Utbetalingsperiode(0, it.periode, it.beloep, UtbetalingsperiodeType.UTBETALING) }?.sortedBy { it.periode.fom } ?: emptyList()

        val manglendePerioderMellomBeregninger = perioderFraBeregning
            .map { it.periode }
            .zipWithNext()
            .map { Periode(requireNotNull( it.first.tom).plusMonths(1), it.second.fom.minusMonths(1)) }
            .filter { !it.tom!!.isBefore(it.fom) }
            .map { Utbetalingsperiode(0, it, null, UtbetalingsperiodeType.OPPHOER) }
        val fomBeregninger = perioderFraBeregning.firstOrNull()?.periode?.fom
        val manglendeStart = if (fomBeregninger == null || vedtak.virk.fom.isBefore(fomBeregninger)) Utbetalingsperiode(0, Periode(vedtak.virk.fom, null), null, UtbetalingsperiodeType.OPPHOER)  else null
        val manglendeSlutt = perioderFraBeregning.lastOrNull()?.periode?.tom?.let { Utbetalingsperiode(0,Periode( it.plusMonths(1), null), null, UtbetalingsperiodeType.OPPHOER) }

        return (perioderFraBeregning + manglendePerioderMellomBeregninger + manglendeStart + manglendeSlutt).filterNotNull().sortedBy { it.periode.fom }
    }
}
