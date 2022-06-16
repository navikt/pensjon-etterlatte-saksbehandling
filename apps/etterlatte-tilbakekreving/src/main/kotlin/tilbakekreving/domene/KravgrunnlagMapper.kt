package no.nav.etterlatte.tilbakekreving.domene

import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Grunnlagsperiode
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep.KlasseKode
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Grunnlagsperiode.Grunnlagsbeloep.KlasseType
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Grunnlagsperiode.Periode
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.Kontrollfelt
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.KravgrunnlagStatus
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.NavIdent
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.SakId
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.UUID30
import no.nav.etterlatte.tilbakekreving.domene.Kravgrunnlag.VedtakId
import no.nav.etterlatte.tilbakekreving.utils.toLocalDate
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto

class KravgrunnlagMapper {

    fun toKravgrunnlag(grunnlag: DetaljertKravgrunnlagDto) = Kravgrunnlag(
        sakId = SakId(grunnlag.fagsystemId.toLong()),
        kravgrunnlagId = KravgrunnlagId(grunnlag.kravgrunnlagId),
        vedtakId = VedtakId(grunnlag.vedtakId),
        kontrollFelt = Kontrollfelt(grunnlag.kontrollfelt),
        status = KravgrunnlagStatus.valueOf(grunnlag.kodeStatusKrav),
        saksbehandler = NavIdent(grunnlag.saksbehId),
        sisteUtbetalingslinjeId = UUID30(grunnlag.referanse),
        behandlingId = null,
        grunnlagsperioder = grunnlag.tilbakekrevingsPeriode.map { periode ->
            Grunnlagsperiode(
                Periode(
                    fraOgMed = periode.periode.fom.toLocalDate(), tilOgMed = periode.periode.tom.toLocalDate()
                ),
                beloepSkattMnd = periode.belopSkattMnd,
                grunnlagsbeloep = periode.tilbakekrevingsBelop.map { beloep ->
                    Grunnlagsbeloep(
                        kode = KlasseKode(beloep.kodeKlasse),
                        type = KlasseType.valueOf(beloep.typeKlasse.value()),
                        beloepTidligereUtbetaling = beloep.belopOpprUtbet,
                        beloepNyUtbetaling = beloep.belopNy,
                        beloepSkalTilbakekreves = beloep.belopTilbakekreves,
                        beloepSkalIkkeTilbakekreves = beloep.belopUinnkrevd,
                        skatteProsent = beloep.skattProsent
                    )
                },
            )
        }
    )
}

