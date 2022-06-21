package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Grunnlagsperiode
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep.KlasseKode
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep.KlasseType
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Grunnlagsperiode.Periode
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.Kontrollfelt
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.KravgrunnlagStatus
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.NavIdent
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.SakId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.UUID30
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag.VedtakId
import no.nav.etterlatte.tilbakekreving.utils.toLocalDate
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto

class KravgrunnlagMapper {

    fun toKravgrunnlag(grunnlag: DetaljertKravgrunnlagDto, kravgrunnlagXml: String) = Kravgrunnlag(
        sakId = SakId(grunnlag.fagsystemId.toLong()),
        kravgrunnlagId = KravgrunnlagId(grunnlag.kravgrunnlagId.toLong()),
        vedtakId = VedtakId(grunnlag.vedtakId.toLong()),
        kontrollFelt = Kontrollfelt(grunnlag.kontrollfelt),
        status = KravgrunnlagStatus.valueOf(grunnlag.kodeStatusKrav),
        saksbehandler = NavIdent(grunnlag.saksbehId),
        sisteUtbetalingslinjeId = UUID30(grunnlag.referanse),
        mottattKravgrunnlagXml = kravgrunnlagXml,
        grunnlagsperioder = grunnlag.tilbakekrevingsPeriode.map { periode ->
            toGrunnlagsperiode(periode)
        }
    )

    private fun toGrunnlagsperiode(periode: DetaljertKravgrunnlagPeriodeDto) =
        Grunnlagsperiode(
            Periode(
                fraOgMed = periode.periode.fom.toLocalDate(), tilOgMed = periode.periode.tom.toLocalDate()
            ),
            beloepSkattMnd = periode.belopSkattMnd,
            grunnlagsbeloep = periode.tilbakekrevingsBelop.map { beloep ->
                toGrunnlagsbeloep(beloep)
            },
        )

    private fun toGrunnlagsbeloep(beloep: DetaljertKravgrunnlagBelopDto) =
        Grunnlagsbeloep(
            kode = KlasseKode(beloep.kodeKlasse),
            type = KlasseType.valueOf(beloep.typeKlasse.value()),
            beloepTidligereUtbetaling = beloep.belopOpprUtbet,
            beloepNyUtbetaling = beloep.belopNy,
            beloepSkalTilbakekreves = beloep.belopTilbakekreves,
            beloepSkalIkkeTilbakekreves = beloep.belopUinnkrevd,
            skatteProsent = beloep.skattProsent
        )
}

