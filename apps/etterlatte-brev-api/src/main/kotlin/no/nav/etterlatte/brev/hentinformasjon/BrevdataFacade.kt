package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.behandling.mapVerge
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode as CommonBeregningsperiode

class BrevdataFacade(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningService: BeregningService,
    private val behandlingKlient: BehandlingKlient,
    private val sakService: SakService,
    private val trygdetidService: TrygdetidService,
) {
    suspend fun hentBrevutfall(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevutfallDto? {
        return behandlingKlient.hentBrevutfall(behandlingId, brukerTokenInfo)
    }

    suspend fun hentEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtterbetalingDTO? {
        return behandlingKlient.hentEtterbetaling(behandlingId, brukerTokenInfo)
    }

    suspend fun hentGenerellBrevData(
        sakId: Long,
        behandlingId: UUID?,
        overstyrSpraak: Spraak? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): GenerellBrevData {
        return coroutineScope {
            val sakDeferred = async { sakService.hentSak(sakId, brukerTokenInfo) }
            val vedtakDeferred = behandlingId?.let { async { vedtaksvurderingKlient.hentVedtak(it, brukerTokenInfo) } }
            val brevutfallDeferred = behandlingId?.let { async { hentBrevutfall(it, brukerTokenInfo) } }

            val grunnlag =
                when (vedtakDeferred?.await()?.type) {
                    VedtakType.TILBAKEKREVING,
                    VedtakType.AVVIST_KLAGE,
                    ->
                        async {
                            grunnlagKlient.hentGrunnlagForSak(
                                sakId,
                                brukerTokenInfo,
                            )
                        }.await()

                    null -> async { grunnlagKlient.hentGrunnlagForSak(sakId, brukerTokenInfo) }.await()
                    else -> async { grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo) }.await()
                }
            val sak = sakDeferred.await()
            val brevutfallDto = brevutfallDeferred?.await()
            val personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(brevutfallDto),
                    avdoede = grunnlag.mapAvdoede(),
                    verge = grunnlag.mapVerge(sak.sakType, behandlingId, brevutfallDto),
                )
            val vedtak = vedtakDeferred?.await()
            val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()
            val saksbehandlerIdent = vedtak?.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
            val attestantIdent =
                vedtak?.vedtakFattet?.let { vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent }

            val behandling =
                if (behandlingId != null && (
                        vedtak?.type in
                            listOf(
                                VedtakType.INNVILGELSE,
                                VedtakType.AVSLAG,
                                VedtakType.OPPHOER,
                                VedtakType.ENDRING,
                            ) || vedtak == null
                    )
                ) {
                    behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
                } else {
                    null
                }
            val systemkilde = behandling?.kilde ?: Vedtaksloesning.GJENNY // Dette kan være en pesys-sak
            val spraak = overstyrSpraak ?: grunnlag.mapSpraak()

            when (vedtak?.type) {
                VedtakType.INNVILGELSE,
                VedtakType.OPPHOER,
                VedtakType.AVSLAG,
                VedtakType.ENDRING,
                ->
                    (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).let { vedtakInnhold ->
                        GenerellBrevData(
                            sak = sak,
                            personerISak = personerISak,
                            behandlingId = behandlingId,
                            forenkletVedtak =
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
                                ),
                            spraak = spraak,
                            revurderingsaarsak = vedtakInnhold.behandling.revurderingsaarsak,
                            systemkilde = systemkilde,
                            utlandstilknytning = behandling?.utlandstilknytning,
                        )
                    }

                VedtakType.TILBAKEKREVING ->
                    GenerellBrevData(
                        sak = sak,
                        personerISak = personerISak,
                        behandlingId = behandlingId,
                        forenkletVedtak =
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
                        spraak = spraak,
                        systemkilde = systemkilde,
                    )

                VedtakType.AVVIST_KLAGE ->
                    GenerellBrevData(
                        sak = sak,
                        personerISak = personerISak,
                        behandlingId = behandlingId,
                        forenkletVedtak =
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
                        spraak = spraak,
                        systemkilde = systemkilde,
                    )

                null ->
                    GenerellBrevData(
                        sak = sak,
                        personerISak = personerISak,
                        behandlingId = behandlingId,
                        forenkletVedtak = null,
                        spraak = spraak,
                        systemkilde = systemkilde,
                        utlandstilknytning = behandling?.utlandstilknytning,
                    )
            }
        }
    }

    suspend fun hentKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Klage = behandlingKlient.hentKlage(klageId, brukerTokenInfo)

    suspend fun finnForrigeUtbetalingsinfo(
        sakId: Long,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): Utbetalingsinfo? =
        beregningService.finnUtbetalingsinfoNullable(
            behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo).id,
            virkningstidspunkt,
            brukerTokenInfo,
            sakType,
        )

    suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        bruker: BrukerTokenInfo,
        sakType: SakType,
    ): Utbetalingsinfo = beregningService.finnUtbetalingsinfo(behandlingId, virkningstidspunkt, bruker, sakType)

    suspend fun hentGrunnbeloep(brukerTokenInfo: BrukerTokenInfo) = beregningService.hentGrunnbeloep(brukerTokenInfo)

    suspend fun finnAvkortingsinfo(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkortingsinfo {
        return beregningService.finnAvkortingsinfo(
            behandlingId,
            sakType,
            virkningstidspunkt,
            vedtakType,
            brukerTokenInfo,
        )
    }

    suspend fun finnForrigeAvkortingsinfo(
        sakId: Long,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkortingsinfo? {
        val forrigeIverksatteBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo).id
        return beregningService.finnAvkortingsinfoNullable(
            forrigeIverksatteBehandlingId,
            sakType,
            virkningstidspunkt,
            vedtakType,
            brukerTokenInfo,
        )
    }

    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = trygdetidService.finnTrygdetid(behandlingId, brukerTokenInfo)

    suspend fun hentVedtaksbehandlingKanRedigeres(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = behandlingKlient.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
}

fun hentBenyttetTrygdetidOgProratabroek(beregningsperiode: CommonBeregningsperiode): Pair<Int, IntBroek?> {
    return when (beregningsperiode.beregningsMetode) {
        BeregningsMetode.NASJONAL ->
            Pair(
                beregningsperiode.samletNorskTrygdetid ?: throw SamletTeoretiskTrygdetidMangler(),
                null,
            )

        BeregningsMetode.PRORATA -> {
            Pair(
                beregningsperiode.samletTeoretiskTrygdetid ?: throw SamletTeoretiskTrygdetidMangler(),
                beregningsperiode.broek ?: throw BeregningsperiodeBroekMangler(),
            )
        }

        BeregningsMetode.BEST -> throw UgyldigBeregningsMetode()
        null -> beregningsperiode.trygdetid to null
    }
}

class UgyldigBeregningsMetode : UgyldigForespoerselException(
    code = "UGYLDIG_BEREGNINGS_METODE",
    detail =
        "Kan ikke ha brukt beregningsmetode 'BEST' i en faktisk beregning, " +
            "siden best velger mellom nasjonal eller prorata når det beregnes.",
)

class SamletTeoretiskTrygdetidMangler : UgyldigForespoerselException(
    code = "SAMLET_TEORETISK_TRYGDETID_MANGLER",
    detail = "Samlet teoretisk trygdetid mangler i beregningen",
)

class BeregningsperiodeBroekMangler : UgyldigForespoerselException(
    code = "BEREGNINGSPERIODE_BROEK_MANGLER",
    detail = "Beregningsperioden mangler brøk",
)
