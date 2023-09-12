package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import java.time.LocalDate
import javax.xml.datatype.XMLGregorianCalendar

object KravgrunnlagMapper {

    fun toKravgrunnlag(grunnlag: DetaljertKravgrunnlagDto) = Kravgrunnlag(
        sakId = SakId(grunnlag.fagsystemId.toLong()),
        kravgrunnlagId = KravgrunnlagId(grunnlag.kravgrunnlagId.toLong()),
        vedtakId = VedtakId(grunnlag.vedtakId.toLong()),
        kontrollFelt = Kontrollfelt(grunnlag.kontrollfelt),
        status = KravgrunnlagStatus.valueOf(grunnlag.kodeStatusKrav),
        saksbehandler = NavIdent(grunnlag.saksbehId),
        sisteUtbetalingslinjeId = UUID30(grunnlag.referanse),
        grunnlagsperioder = grunnlag.tilbakekrevingsPeriode.map { periode ->
            toGrunnlagsperiode(periode)
        }
    )

    private fun toGrunnlagsperiode(periode: DetaljertKravgrunnlagPeriodeDto) =
        Grunnlagsperiode(
            Periode(
                fraOgMed = periode.periode.fom.toLocalDate(),
                tilOgMed = periode.periode.tom.toLocalDate()
            ),
            beloepSkattMnd = periode.belopSkattMnd,
            grunnlagsbeloep = periode.tilbakekrevingsBelop.map { beloep ->
                toGrunnlagsbeloep(beloep)
            }
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

fun XMLGregorianCalendar.toLocalDate(): LocalDate = this.toGregorianCalendar().toZonedDateTime().toLocalDate()