package no.nav.etterlatte.vedtak

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto

class VedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtak(fnr: Folkeregisteridentifikator): VedtakForEksterntDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(fnr)
        return VedtakForEksterntDto(
            vedtak = vedtak.filter { it.type.vanligBehandling }.map { it.toEksternDto() },
        )
    }
}

fun VedtakDto.toEksternDto(): VedtakEksternt {
    val vedtakInnhold = (innhold as VedtakInnholdDto.VedtakBehandlingDto)
    return VedtakEksternt(
        sakId = sak.id.sakId,
        sakType = sak.sakType.name,
        virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
        type = VedtakTypeEksternt.valueOf(type.name),
        utbetaling =
            vedtakInnhold.utbetalingsperioder.map { utbetaling ->
                VedtakEksterntUtbetaling(
                    periode = utbetaling.periode,
                    beloep = utbetaling.beloep,
                )
            },
    )
}
