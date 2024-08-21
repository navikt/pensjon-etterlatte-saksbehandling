package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.ForskjelligTrygdetid
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManglerAvdoedBruktTilTrygdetid
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner

internal fun barnepensjonBeregningsperioder(utbetalingsinfo: Utbetalingsinfo): List<BarnepensjonBeregningsperiode> =
    utbetalingsinfo.beregningsperioder.map { BarnepensjonBeregningsperiode.fra(it) }

internal fun barnepensjonBeregning(
    innhold: InnholdMedVedlegg,
    avdoede: List<Avdoed>,
    utbetalingsinfo: Utbetalingsinfo,
    grunnbeloep: Grunnbeloep,
    beregningsperioder: List<BarnepensjonBeregningsperiode>,
    trygdetid: List<TrygdetidDto>,
    erForeldreloes: Boolean = false,
): BarnepensjonBeregning {
    val sisteBeregningsperiode = utbetalingsinfo.beregningsperioder.maxBy { periode -> periode.datoFOM }

    val forskjelligTrygdetid = finnForskjelligTrygdetid(trygdetid, utbetalingsinfo, avdoede)

    val anvendtMetode = sisteBeregningsperiode.beregningsMetodeAnvendt
    val metodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag

    return BarnepensjonBeregning(
        innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
        antallBarn = utbetalingsinfo.antallBarn,
        virkningsdato = utbetalingsinfo.virkningsdato,
        grunnbeloep = Kroner(grunnbeloep.grunnbeloep),
        beregningsperioder = beregningsperioder,
        sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM },
        trygdetid = trygdetid.map { it.fromDto(anvendtMetode, metodeFraGrunnlag, avdoede) },
        erForeldreloes = erForeldreloes,
        bruktTrygdetid =
            trygdetid
                .find { it.ident == sisteBeregningsperiode.trygdetidForIdent }
                ?.fromDto(anvendtMetode, metodeFraGrunnlag, avdoede)
                ?: throw ManglerAvdoedBruktTilTrygdetid(),
        forskjelligTrygdetid = forskjelligTrygdetid,
    )
}

fun finnForskjelligTrygdetid(
    trygdetid: List<TrygdetidDto>,
    utbetalingsinfo: Utbetalingsinfo,
    avdoede: List<Avdoed>,
): ForskjelligTrygdetid? {
    // Vi må sende med forskjellig trygdetid hvis trygdetidsgrunnlaget varierer over perioder
    val foersteBeregningsperiode = utbetalingsinfo.beregningsperioder.first()
    val sisteBeregningsperiode = utbetalingsinfo.beregningsperioder.last()
    if (foersteBeregningsperiode.avdoedeForeldre?.toSet() == sisteBeregningsperiode.avdoedeForeldre?.toSet()) {
        // Vi har det samme trygdetidsgrunnlaget over alle periodene
        return null
    }
    val behandlingId = trygdetid.first().behandlingId

    // Hvis vi har anvendt forskjellige trygdetider over beregningen må vi ha forskjeller i avdøde
    val forskjelligAvdoedPeriode =
        checkNotNull(finnEventuellForskjelligAvdoedPeriode(avdoede, utbetalingsinfo)) {
            "Vi har anvendt forskjellige trygdetider, men vi har ikke forskjellige perioder for hvilke" +
                "avdøde vi har brukt i beregningen. Dette bør ikke være mulig. " +
                "BehandlingId=$behandlingId"
        }

    val trygdetidForFoersteAvdoed =
        checkNotNull(trygdetid.find { it.ident == forskjelligAvdoedPeriode.foersteAvdoed.fnr.value }) {
            "Fant ikke trygdetiden som er brukt i første beregningsperiode i behandlingId=$behandlingId"
        }
    val trygdetidBruktSenere =
        checkNotNull(trygdetid.find { it.ident == sisteBeregningsperiode.trygdetidForIdent }) {
            "Fant ikke trygdetiden som er brukt i siste beregningsperiode i behandlingId=$behandlingId"
        }

    return ForskjelligTrygdetid(
        foersteTrygdetid = trygdetidForFoersteAvdoed,
        foersteVirkningsdato = utbetalingsinfo.virkningsdato,
        senereVirkningsdato = forskjelligAvdoedPeriode.senereVirkningsdato,
        harForskjelligMetode = foersteBeregningsperiode.beregningsMetodeFraGrunnlag != sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
        erForskjellig = trygdetidBruktSenere.ident != trygdetidForFoersteAvdoed.ident,
    )
}
