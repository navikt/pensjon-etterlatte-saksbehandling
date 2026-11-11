package no.nav.etterlatte.behandling.vedtaksvurdering.service

import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerDto
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.util.UUID

class VedtakEtteroppgjoerService(
    private val repository: VedtaksvurderingRepository,
    private val vedtakSamordningService: VedtakSamordningService,
) {
    fun hentVedtakslisteIEtteroppgjoersAar(
        sakId: SakId,
        etteroppgjoersAar: Int,
        tilOgMedBehandlingId: UUID? = null,
    ): List<VedtakEtteroppgjoerDto> =
        hentVedtakForSak(sakId)
            .retainIverksatteOgAttesterteAvslag()
            .tidslinjejustert(tilOgMedBehandlingId, etteroppgjoersAar)
            .map { vedtakSamordningDto ->
                VedtakEtteroppgjoerDto(
                    vedtakId = vedtakSamordningDto.vedtakId,
                    perioder =
                        vedtakSamordningDto.perioder.map { periode ->
                            VedtakEtteroppgjoerPeriode(
                                fom = periode.fom,
                                tom = periode.tom,
                                ytelseEtterAvkorting = periode.ytelseEtterAvkorting,
                            )
                        },
                )
            }

    private fun List<Vedtak>.tidslinjejustert(
        tilOgMedBehandlingId: UUID?,
        etteroppgjoersAar: Int,
    ): List<VedtakSamordningDto> =
        vedtakSamordningService
            .tidslinjejustertVedtaksliste(
                vedtakListe = this,
                tilOgMedBehandlingId = tilOgMedBehandlingId,
                fomDato = LocalDate.of(etteroppgjoersAar, 1, 1),
            )

    private fun List<Vedtak>.retainIverksatteOgAttesterteAvslag(): List<Vedtak> =
        filter {
            it.status == VedtakStatus.IVERKSATT ||
                (it.type == VedtakType.AVSLAG && it.status == VedtakStatus.ATTESTERT)
        }

    private fun hentVedtakForSak(sakId: SakId): List<Vedtak> {
        val vedtak = repository.hentVedtakForSak(sakId)
        krev(vedtak.isNotEmpty()) { "Fant ingen vedtak for sakId=$sakId" }
        return vedtak
    }
}
