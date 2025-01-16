package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakOffentlig
import no.nav.etterlatte.libs.common.vedtak.VedtakOffentligDto
import no.nav.etterlatte.libs.common.vedtak.VedtakOffentligUtbetalingDto
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository

class OffentligVedtakService(
    private val repository: VedtaksvurderingRepository,
) {
    fun hentVedtak(fnr: String): VedtakOffentligDto {
        val folkeregisteridentifikator = Folkeregisteridentifikator.of(fnr)
        val vedtak = repository.hentFerdigstilteVedtak(folkeregisteridentifikator)

        // TODO må noe mer filtreres?

        return VedtakOffentligDto(
            vedtak =
                vedtak.map {
                    // TODO kun behandlignsvedtak?
                    val vedtakInnhold = (it.innhold as VedtakInnhold.Behandling)
                    VedtakOffentlig(
                        virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
                        type = it.type,
                        utbetaling =
                            vedtakInnhold.utbetalingsperioder.map { utbetaling ->
                                VedtakOffentligUtbetalingDto(
                                    periode = utbetaling.periode,
                                    beloep = utbetaling.beloep,
                                )
                            },
                    )
                },
        )
    }
}
