package no.nav.etterlatte.no.nav.etterlatte.testdata.features.dolly

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.testdata.features.dolly.Vedtak
import no.nav.etterlatte.testdata.features.dolly.VedtakTilPerson
import no.nav.etterlatte.testdata.features.dolly.VedtakType
import no.nav.etterlatte.testdata.features.dolly.VedtakUtbetaling
import java.time.LocalDate
import java.time.YearMonth

class VedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtak(fnr: Folkeregisteridentifikator): VedtakTilPerson {
        val vedtak = vedtaksvurderingKlient.hentVedtak(fnr)
        return VedtakTilPerson(
            vedtak = vedtak.filter { it.type.vanligBehandling }.map { it.fromDto() },
        )
    }
}

fun VedtakDto.fromDto(): Vedtak {
    val vedtakInnhold = (innhold as VedtakInnholdDto.VedtakBehandlingDto)
    return Vedtak(
        sakId = sak.id.sakId,
        sakType = sak.sakType.name,
        virkningstidspunkt = vedtakInnhold.virkningstidspunkt.atStartOfMonth(),
        type = VedtakType.valueOf(type.name),
        utbetaling =
            vedtakInnhold.utbetalingsperioder.map { utbetaling ->
                VedtakUtbetaling(
                    fraOgMed = utbetaling.periode.fom.atStartOfMonth(),
                    tilOgMed = utbetaling.periode.tom?.atEndOfMonth(),
                    beloep = utbetaling.beloep,
                )
            },
    )
}

fun YearMonth.atStartOfMonth(): LocalDate = this.atDay(1)
