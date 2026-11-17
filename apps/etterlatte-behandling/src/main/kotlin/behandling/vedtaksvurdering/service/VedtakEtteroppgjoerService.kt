package no.nav.etterlatte.behandling.vedtaksvurdering.service

import no.nav.etterlatte.behandling.etteroppgjoer.EtteroppgjoerToggles
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
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
    private val featureToggleService: FeatureToggleService,
) {
    fun hentVedtakslisteIEtteroppgjoersAar(
        sakId: SakId,
        etteroppgjoersAar: Int,
        tilOgMedBehandlingId: UUID? = null,
    ): List<VedtakEtteroppgjoerDto> {
        if (!featureToggleService.isEnabled(EtteroppgjoerToggles.VEDTAKSLISTE_HENT_GITT_SAKID, false)) {
            return hentVedtakslisteIEtteroppgjoersAarOld(sakId, etteroppgjoersAar, tilOgMedBehandlingId)
        }
        return hentVedtakForSak(sakId)
            .retainIverksatteOgAttesterteAvslag()
            .sammenstilt(tilOgMedBehandlingId, etteroppgjoersAar)
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
    }

    fun hentVedtakslisteIEtteroppgjoersAarOld(
        sakId: SakId,
        etteroppgjoersAar: Int,
        tilOgMedBehandlingId: UUID? = null,
    ): List<VedtakEtteroppgjoerDto> {
        val vedtak = repository.hentVedtakForSak(sakId).firstOrNull()
        krevIkkeNull(vedtak) { "Fant ingen vedtak for sakId=$sakId" }

        val fnr = Folkeregisteridentifikator.of(vedtak.soeker.value)

        val vedtaksliste =
            vedtakSamordningService.hentVedtaksliste(
                fnr,
                SakType.OMSTILLINGSSTOENAD,
                LocalDate.of(etteroppgjoersAar, 1, 1),
                tilOgMedBehandlingId,
            )

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

    private fun List<Vedtak>.sammenstilt(
        tilOgMedBehandlingId: UUID?,
        etteroppgjoersAar: Int,
    ): List<VedtakSamordningDto> =
        vedtakSamordningService
            .sammenstiltVedtaksliste(
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
