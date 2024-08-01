package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonBeregningsperiode
import no.nav.etterlatte.brev.model.BrevVedleggKey
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

    // Beregningsmetode per nå er det samme på tvers av trygdetider
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
    )
}
