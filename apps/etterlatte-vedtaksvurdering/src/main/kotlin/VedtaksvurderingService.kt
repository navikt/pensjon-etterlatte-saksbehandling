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
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.atomic.AtomicReference


class VedtaksvurderingService(private val repository: VedtaksvurderingRepository, private val rapid: AtomicReference<MessageContext> = AtomicReference()) {
    companion object{
        val logger: Logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)
    }
    fun lagreAvkorting(sakId: String, behandlingId: UUID, fnr: String, avkorting: AvkortingsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreAvkorting(sakId, behandlingId, fnr, avkorting)
        } else {
            repository.oppdaterAvkorting(sakId, behandlingId, avkorting)
        }
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingId: UUID, fnr: String, vilkaarResultat: VilkaarResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreVilkaarsresultat(sakId, behandlingId, fnr, vilkaarResultat)
        } else {
            repository.oppdaterVilkaarsresultat(sakId, behandlingId, vilkaarResultat)
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingId: UUID, fnr: String, beregningsResultat: BeregningsResultat) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)
        if(vedtak == null) {
            repository.lagreBeregningsresultat(sakId, behandlingId, fnr, beregningsResultat)
        } else {
            repository.oppdaterBeregningsgrunnlag(sakId, behandlingId, beregningsResultat)
        }
    }

    fun lagreKommerSoekerTilgodeResultat(sakId: String, behandlingId: UUID, fnr: String, kommerSoekerTilgodeResultat: KommerSoekerTilgode) {
        val vedtak = repository.hentVedtak(sakId, behandlingId)

        if(vedtak == null) {
            repository.lagreKommerSoekerTilgodeResultat(sakId, behandlingId, fnr, kommerSoekerTilgodeResultat)
        } else {
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
                Periode(it.vilkaarsResultat?.vilkaar?.find { it.navn == Vilkaartyper.DOEDSFALL_ER_REGISTRERT }?.kriterier?.find { it.navn == Kriterietyper.DOEDSFALL_ER_REGISTRERT_I_PDL }?.basertPaaOpplysninger?.find { it.kriterieOpplysningsType == KriterieOpplysningsType.DOEDSDATO }?.opplysning?.let { it as Doedsdato }?.doedsdato?.with(
                    TemporalAdjusters.firstDayOfNextMonth())?.let { YearMonth.of(it.year, it.month) } ?: YearMonth.now(), null), //må få inn dette på toppnivå?
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
                null,
                it.saksbehandlerId?.let { ansvarligSaksbehadnlier -> VedtakFattet(ansvarligSaksbehadnlier, "0000", it.datoFattet?.atZone(
                    ZoneOffset.UTC)!!) }, //logikk inn der fatting skjer. DB utvides med enhet og timestamp?
                it.attestant?.let {attestant-> Attestasjon(attestant, "0000",  it.datoattestert!!.atZone(ZoneOffset.UTC)) }
            )
        }
    }

    fun fattVedtak(sakId: String, behandlingId: UUID, saksbehandler: String) {
        //repository
        return repository.fattVedtak(saksbehandler, sakId, behandlingId)
    }


    fun attesterVedtak(sakId: String, behandlingId: UUID, saksbehandler: String) {
        repository.attesterVedtak(saksbehandler, sakId, behandlingId)
        publiserHendelse("ATTESTERT", sakId, behandlingId)

    }
    fun publiserHendelse(hendelse: String, sakId: String, behandlingId: UUID ){
        rapid.get().also {
            if (it == null){
                logger.warn("Skulle publisere hendelse om at VEDTAK:$hendelse, men rapid er ikke intiert i servicelaget")
            } else {
                hentFellesVedtak(sakId, behandlingId)!!.also { vedtak ->
                    it.publish(vedtak.vedtakId.toString(), newMessage(mapOf("@event" to "VEDTAK:$hendelse", "@vedtak" to vedtak)).toJson())
                }
            }
        }
    }
}
