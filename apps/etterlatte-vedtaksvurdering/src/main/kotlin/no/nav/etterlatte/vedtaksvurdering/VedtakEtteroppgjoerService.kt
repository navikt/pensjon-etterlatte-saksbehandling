package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.vedtaksvurdering.VedtakSamordningService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import java.time.LocalDate

class VedtakEtteroppgjoerService(
    private val repository: VedtaksvurderingRepository,
    private val vedtakSamordningService: VedtakSamordningService,
) {
    fun hentVedtakslisteIEtteroppgjoersAar(
        sakId: SakId,
        etteroppgjoersAar: Int,
    ): List<VedtakEtteroppgjoerDto> {
        val vedtak = repository.hentVedtakForSak(sakId).firstOrNull()
        krevIkkeNull(vedtak) { "Fant ingen vedtak for sakId=$sakId" }

        val fnr = Folkeregisteridentifikator.of(vedtak.soeker.value)

        val vedtaksliste = vedtakSamordningService.hentVedtaksliste(fnr, SakType.OMSTILLINGSSTOENAD, LocalDate.of(etteroppgjoersAar, 1, 1))

        return vedtaksliste.map { vedtak ->
            VedtakEtteroppgjoerDto(
                vedtakId = vedtak.vedtakId,
                perioder =
                    vedtak.perioder.map { periode ->
                        VedtakEtteroppgjoerPeriode(
                            fom = periode.fom,
                            tom = periode.tom,
                            ytelseEtterAvkorting = periode.ytelseEtterAvkorting,
                        )
                    },
            )
        }
    }
}
