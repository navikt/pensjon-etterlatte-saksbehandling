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
import java.util.UUID

internal fun barnepensjonBeregningsperioder(utbetalingsinfo: Utbetalingsinfo) =
    utbetalingsinfo.beregningsperioder.map {
        BarnepensjonBeregningsperiode(
            datoFOM = it.datoFOM,
            datoTOM = it.datoTOM,
            grunnbeloep = it.grunnbeloep,
            utbetaltBeloep = it.utbetaltBeloep,
            antallBarn = it.antallBarn,
        )
    }

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
            trygdetid.find {
                (it.ident == sisteBeregningsperiode.trygdetidForIdent) || (
                    it.behandlingId in
                        listOf(
                            UUID.fromString("36fb86d4-698e-43cb-ab5f-ee7a0febda2e"),
                            UUID.fromString("038dd6b8-c17b-482e-ae26-51ae591d5c42"),
                            UUID.fromString("278d1bdb-2f38-484a-8d98-b74a4220dcea"),
                            UUID.fromString("00051cfe-0405-4ebf-93f6-5775d23afcba"),
                        )
                )
            }?.fromDto(anvendtMetode, metodeFraGrunnlag, avdoede)
                ?: throw ManglerAvdoedBruktTilTrygdetid(),
    )
}
