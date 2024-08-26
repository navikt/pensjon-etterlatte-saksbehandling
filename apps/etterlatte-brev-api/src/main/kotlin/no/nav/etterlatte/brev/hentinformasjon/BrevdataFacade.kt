package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class BrevdataFacade(
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
) {
    suspend fun hentGenerellBrevData(
        sakId: Long,
        behandlingId: UUID?,
        overstyrSpraak: Spraak? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): GenerellBrevData =
        coroutineScope {
            val sakDeferred = async { behandlingService.hentSak(sakId, brukerTokenInfo) }
            val vedtakDeferred = behandlingId?.let { async { vedtaksvurderingService.hentVedtak(it, brukerTokenInfo) } }
            val brevutfallDeferred = behandlingId?.let { async { behandlingService.hentBrevutfall(it, brukerTokenInfo) } }

            val vedtakType = vedtakDeferred?.await()?.type
            val grunnlag = grunnlagService.hentGrunnlag(vedtakType, sakId, brukerTokenInfo, behandlingId)

            val sak = sakDeferred.await()
            val brevutfallDto = brevutfallDeferred?.await()
            val verge = grunnlagService.hentVergeForSak(sak.sakType, brevutfallDto, grunnlag)
            val personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(brevutfallDto),
                    avdoede = grunnlag.mapAvdoede(),
                    verge = verge,
                )
            val vedtak = vedtakDeferred?.await()
            val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()
            val saksbehandlerIdent = vedtak?.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
            val attestantIdent =
                vedtak?.vedtakFattet?.let { vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent }

            val behandling =
                if (behandlingId != null &&
                    (
                        vedtak?.type in
                            listOf(
                                VedtakType.INNVILGELSE,
                                VedtakType.AVSLAG,
                                VedtakType.OPPHOER,
                                VedtakType.ENDRING,
                            ) ||
                            vedtak == null
                    )
                ) {
                    behandlingService.hentBehandling(behandlingId, brukerTokenInfo)
                } else {
                    null
                }
            val systemkilde = behandling?.kilde ?: Vedtaksloesning.GJENNY // Dette kan være en pesys-sak
            val spraak = overstyrSpraak ?: grunnlag.mapSpraak()
            val relatertKlageId =
                when (behandling?.revurderingsaarsak) {
                    Revurderingaarsak.OMGJOERING_ETTER_KLAGE -> {
                        requireNotNull(behandling.relatertBehandlingId) {
                            "Vi må få med den relaterte klagen til behanldingen hvis dette vedtaket er en omgjøring " +
                                "etter klage, for å få riktig brev."
                        }
                    }
                    else -> null
                }

            val forenkletVedtak =
                vedtakOgRevurderingsaarsak(vedtak, sak, saksbehandlerIdent, attestantIdent, brukerTokenInfo, relatertKlageId)

            GenerellBrevData(
                sak = sak,
                personerISak = personerISak,
                behandlingId = behandlingId,
                forenkletVedtak = forenkletVedtak.first,
                spraak = spraak,
                revurderingsaarsak = forenkletVedtak.second,
                systemkilde = systemkilde,
                utlandstilknytning = behandling?.utlandstilknytning,
            )
        }

    private suspend fun vedtakOgRevurderingsaarsak(
        vedtak: VedtakDto?,
        sak: Sak,
        saksbehandlerIdent: String,
        attestantIdent: String?,
        bruker: BrukerTokenInfo,
        relatertKlageId: String?,
    ): Pair<ForenkletVedtak?, Revurderingaarsak?> =
        when (vedtak?.type) {
            VedtakType.INNVILGELSE,
            VedtakType.OPPHOER,
            VedtakType.AVSLAG,
            VedtakType.ENDRING,
            ->
                (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).let { vedtakInnhold ->
                    Pair(
                        ForenkletVedtak(
                            vedtak.id,
                            vedtak.status,
                            vedtak.type,
                            sak.enhet,
                            saksbehandlerIdent,
                            attestantIdent,
                            vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                            virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
                            revurderingInfo = vedtakInnhold.behandling.revurderingInfo,
                            klage =
                                if (vedtakInnhold.behandling.revurderingsaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
                                    val klageId = UUID.fromString(relatertKlageId)
                                    behandlingService.hentKlage(klageId, bruker)
                                } else {
                                    null
                                },
                        ),
                        vedtakInnhold.behandling.revurderingsaarsak,
                    )
                }

            VedtakType.TILBAKEKREVING ->
                Pair(
                    ForenkletVedtak(
                        vedtak.id,
                        vedtak.status,
                        vedtak.type,
                        sak.enhet,
                        saksbehandlerIdent,
                        attestantIdent,
                        vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                        tilbakekreving =
                            objectMapper.readValue(
                                (vedtak.innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving.toJson(),
                            ),
                    ),
                    null,
                )

            VedtakType.AVVIST_KLAGE ->
                Pair(
                    ForenkletVedtak(
                        vedtak.id,
                        vedtak.status,
                        vedtak.type,
                        sak.enhet,
                        saksbehandlerIdent,
                        null,
                        vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                        klage =
                            objectMapper.readValue(
                                (vedtak.innhold as VedtakInnholdDto.Klage).klage.toJson(),
                            ),
                    ),
                    null,
                )

            null -> Pair(null, null)
        }
}
