package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.FantIkkeIdentTilTrygdetidBlantAvdoede
import no.nav.etterlatte.brev.model.ForskjelligTrygdetid
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManglerAvdoedBruktTilTrygdetid
import no.nav.etterlatte.brev.model.OverstyrtTrygdetidManglerAvdoed
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.sikkerLogg
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.util.UUID

internal fun barnepensjonBeregningsperioder(
    utbetalingsinfo: Utbetalingsinfo,
    erForeldreloes: Boolean,
): List<BarnepensjonBeregningsperiode> = utbetalingsinfo.beregningsperioder.map { BarnepensjonBeregningsperiode.fra(it, erForeldreloes) }

internal fun barnepensjonBeregning(
    innhold: InnholdMedVedlegg,
    avdoede: List<Avdoed>,
    utbetalingsinfo: Utbetalingsinfo,
    grunnbeloep: Grunnbeloep,
    trygdetid: List<TrygdetidDto>,
    erForeldreloes: Boolean = false,
    landKodeverk: List<LandDto>,
): BarnepensjonBeregning {
    val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo, erForeldreloes)
    val sisteBeregningsperiodeFraUtbetalingsInfo = beregningsperioder.maxBy { periode -> periode.datoFOM }

    val mappedeTrygdetider =
        mapRiktigMetodeForAnvendteTrygdetider(trygdetid, avdoede, utbetalingsinfo.beregningsperioder, landKodeverk)

    val forskjelligTrygdetid =
        finnForskjelligTrygdetid(
            mappedeTrygdetider,
            utbetalingsinfo,
            avdoede,
            trygdetid.first().behandlingId,
        )

    val erYrkesskade = trygdetid.any { it.erYrkesskade() }

    return BarnepensjonBeregning(
        innhold = innhold.finnVedlegg(BrevVedleggKey.BP_BEREGNING_TRYGDETID),
        antallBarn = utbetalingsinfo.antallBarn,
        virkningsdato = utbetalingsinfo.virkningsdato,
        grunnbeloep = Kroner(grunnbeloep.grunnbeloep),
        beregningsperioder = beregningsperioder,
        sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM },
        trygdetid = mappedeTrygdetider,
        erForeldreloes = erForeldreloes,
        bruktTrygdetid =
            mappedeTrygdetider.find { it.ident == sisteBeregningsperiodeFraUtbetalingsInfo.trygdetidForIdent }
                ?: throw ManglerAvdoedBruktTilTrygdetid(),
        forskjelligTrygdetid = forskjelligTrygdetid,
        erYrkesskade = erYrkesskade,
    )
}

data class IdentMedMetodeIGrunnlagOgAnvendtMetode(
    val ident: String,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val beregningsMetodeAnvendt: BeregningsMetode,
)

private fun mapRiktigMetodeForAnvendteTrygdetider(
    trygdetid: List<TrygdetidDto>,
    avdoede: List<Avdoed>,
    beregningsperioder: List<Beregningsperiode>,
    landKodeverk: List<LandDto>,
): List<TrygdetidMedBeregningsmetode> {
    val anvendteTrygdetiderIdenter =
        beregningsperioder
            .mapNotNull { periode ->
                periode.trygdetidForIdent?.let {
                    IdentMedMetodeIGrunnlagOgAnvendtMetode(
                        ident = it,
                        beregningsMetodeFraGrunnlag = periode.beregningsMetodeFraGrunnlag,
                        beregningsMetodeAnvendt = periode.beregningsMetodeAnvendt,
                    )
                }
            }.associateBy { it.ident }

    if (anvendteTrygdetiderIdenter.isEmpty()) {
        throw ManglerAvdoedBruktTilTrygdetid()
    }

    // Ikke alle trygdetider er nødvendigvis brukt i beregningen, og har da ikke en anvendt trygdetid beregnet med
    // For disse trygdetidene fyller vi bare inn det som er brukt i den siste beregningsmetoden.
    // Brevet vet hvilke trygdetider den kan bruke til å si noe om beregningsmetode anvendt / i grunnlag
    val fallbackMetode =
        anvendteTrygdetiderIdenter[beregningsperioder.maxBy { it.datoFOM }.trygdetidForIdent]
            ?: throw ManglerAvdoedBruktTilTrygdetid()

    return trygdetid.map { trygdetidDto ->
        trygdetidMedBeregningsmetode(
            trygdetidDto = trygdetidDto,
            identMedMetoder = anvendteTrygdetiderIdenter[trygdetidDto.ident] ?: fallbackMetode,
            avdoede = avdoede,
            landKodeverk = landKodeverk,
        )
    }
}

fun finnForskjelligTrygdetid(
    trygdetid: List<TrygdetidMedBeregningsmetode>,
    utbetalingsinfo: Utbetalingsinfo,
    avdoede: List<Avdoed>,
    behandlingId: UUID,
): ForskjelligTrygdetid? {
    // Vi må sende med forskjellig trygdetid hvis trygdetidsgrunnlaget varierer over perioder
    val foersteBeregningsperiode = utbetalingsinfo.beregningsperioder.minBy { it.datoFOM }
    val sisteBeregningsperiode = utbetalingsinfo.beregningsperioder.maxBy { it.datoFOM }

    // For beregninger gjort på gammelt regelverk har vi kun en avdød, og den er ikke fylt inn
    // i "avdoedeForeldre" siden flere avdøde foreldre-regelen kjører kun på nytt regelverk
    val avdoedeForeldreFoerstePeriode = foersteBeregningsperiode.finnAvdoedeForeldreForPeriode()
    val avdoedeForeldreSistePeriode = sisteBeregningsperiode.finnAvdoedeForeldreForPeriode()

    if (avdoedeForeldreFoerstePeriode.toSet() == avdoedeForeldreSistePeriode.toSet()) {
        // Vi har det samme trygdetidsgrunnlaget over alle periodene
        return null
    }

    // Hvis vi har anvendt forskjellige trygdetider over beregningen må vi ha forskjeller i avdøde
    val forskjelligAvdoedPeriode =
        krevIkkeNull(finnEventuellForskjelligAvdoedPeriode(avdoede, utbetalingsinfo)) {
            "Vi har anvendt forskjellige trygdetider, men vi har ikke forskjellige perioder for hvilke" +
                "avdøde vi har brukt i beregningen. Dette bør ikke være mulig. " +
                "BehandlingId=$behandlingId"
        }

    val trygdetidForFoersteAvdoed =
        krevIkkeNull(trygdetid.find { it.ident == forskjelligAvdoedPeriode.foersteAvdoed.fnr.value }) {
            "Fant ikke trygdetiden som er brukt i første beregningsperiode i behandlingId=$behandlingId"
        }
    val trygdetidBruktSenere =
        krevIkkeNull(trygdetid.find { it.ident == sisteBeregningsperiode.trygdetidForIdent }) {
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

fun Beregningsperiode.finnAvdoedeForeldreForPeriode(): List<String> =
    if (datoFOM < BarnepensjonInnvilgelse.tidspunktNyttRegelverk) {
        listOfNotNull(trygdetidForIdent)
    } else {
        avdoedeForeldre?.filterNotNull() ?: emptyList()
    }

internal fun trygdetidMedBeregningsmetode(
    trygdetidDto: TrygdetidDto,
    identMedMetoder: IdentMedMetodeIGrunnlagOgAnvendtMetode,
    avdoede: List<Avdoed>,
    landKodeverk: List<LandDto>,
) = trygdetidDto.fromDto(
    identMedMetoder.beregningsMetodeAnvendt,
    identMedMetoder.beregningsMetodeFraGrunnlag,
    hentAvdoedNavn(trygdetidDto, avdoede),
    landKodeverk,
)

private fun hentAvdoedNavn(
    trygdetidDto: TrygdetidDto,
    avdoede: List<Avdoed>,
): String? {
    if (avdoede.isEmpty() && trygdetidDto.ident == UKJENT_AVDOED) {
        return null
    }

    return avdoede.find { it.fnr.value == trygdetidDto.ident }?.navn ?: run {
        if (trygdetidDto.beregnetTrygdetid?.resultat?.overstyrt == true) {
            sikkerLogg.warn(
                "Fant ikke avdød fra trygdetid (ident: ${trygdetidDto.ident}) blant avdøde fra " +
                    "grunnlag (${avdoede.joinToString { it.fnr.value }})",
            )
            throw OverstyrtTrygdetidManglerAvdoed()
        } else {
            throw FantIkkeIdentTilTrygdetidBlantAvdoede()
        }
    }
}
