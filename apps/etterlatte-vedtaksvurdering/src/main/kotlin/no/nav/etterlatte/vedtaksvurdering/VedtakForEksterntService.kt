package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakForEkstern
import no.nav.etterlatte.libs.common.vedtak.VedtakForEksternUtbetaling
import no.nav.etterlatte.libs.common.vedtak.VedtakForEksterntDto
import no.nav.etterlatte.libs.common.vedtak.VedtakTypeForEkstern
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository

class VedtakForEksterntService(
    private val repository: VedtaksvurderingRepository,
) {
    fun hentVedtak(fnr: String): VedtakForEksterntDto {
        val folkeregisteridentifikator = Folkeregisteridentifikator.of(fnr)
        val vedtak = repository.hentFerdigstilteVedtak(folkeregisteridentifikator)
        return VedtakForEksterntDto(
            vedtak =
                vedtak.filter { it.type.vanligBehandling }.map {
                    val vedtakInnhold = (it.innhold as VedtakInnhold.Behandling)
                    VedtakForEkstern(
                        virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
                        type = VedtakTypeForEkstern.valueOf(it.type.name),
                        utbetaling =
                            vedtakInnhold.utbetalingsperioder.map { utbetaling ->
                                VedtakForEksternUtbetaling(
                                    periode = utbetaling.periode,
                                    beloep = utbetaling.beloep,
                                )
                            },
                    )
                },
        )
    }
}
