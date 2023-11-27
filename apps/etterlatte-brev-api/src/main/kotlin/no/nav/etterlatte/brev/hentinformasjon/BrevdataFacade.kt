package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.hentUtbetaltBeloep
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.behandling.mapVerge
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BrevdataFacade(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingKlient: BehandlingKlient,
    private val sakService: SakService,
    private val trygdetidService: TrygdetidService,
) {
    suspend fun hentEtterbetaling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtterbetalingDTO? {
        return behandlingKlient.hentEtterbetaling(behandlingId, brukerTokenInfo)
    }

    suspend fun hentInnvilgelsesdato(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): LocalDate? {
        return vedtaksvurderingKlient.hentInnvilgelsesdato(sakId, brukerTokenInfo)
    }

    suspend fun hentGenerellBrevData(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): GenerellBrevData {
        return coroutineScope {
            val sakDeferred = async { sakService.hentSak(sakId, brukerTokenInfo) }
            val vedtakDeferred = async { vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo) }
            val grunnlag =
                when (vedtakDeferred.await().type) {
                    VedtakType.TILBAKEKREVING ->
                        async {
                            grunnlagKlient.hentGrunnlagForSak(
                                sakId,
                                brukerTokenInfo,
                            )
                        }.await()

                    else -> async { grunnlagKlient.hentGrunnlag(behandlingId, brukerTokenInfo) }.await()
                }
            val sak = sakDeferred.await()
            val personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(),
                    avdoede = grunnlag.mapAvdoede(),
                    verge = grunnlag.mapVerge(sak.sakType),
                )
            val vedtak = vedtakDeferred.await()
            val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()
            val ansvarligEnhet = vedtak.vedtakFattet?.ansvarligEnhet ?: sak.enhet
            val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
            val attestantIdent =
                vedtak.vedtakFattet?.let { vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent }

            val systemkilde =
                if (vedtak.type == VedtakType.INNVILGELSE) {
                    // Dette kan være en pesys-sak
                    behandlingKlient.hentKilde(behandlingId, brukerTokenInfo)
                } else {
                    // alle andre vedtak kommer fra Gjenny
                    Vedtaksloesning.GJENNY
                }

            when (vedtak.type) {
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
                                    ansvarligEnhet,
                                    saksbehandlerIdent,
                                    attestantIdent,
                                    vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                                    virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
                                    revurderingInfo = vedtakInnhold.behandling.revurderingInfo,
                                ),
                            spraak = grunnlag.mapSpraak(),
                            revurderingsaarsak = vedtakInnhold.behandling.revurderingsaarsak,
                            systemkilde = systemkilde,
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
                                ansvarligEnhet,
                                saksbehandlerIdent,
                                attestantIdent,
                                vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                                tilbakekreving =
                                    objectMapper.readValue(
                                        (vedtak.innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving.toJson(),
                                    ),
                            ),
                        spraak = grunnlag.mapSpraak(),
                        systemkilde = systemkilde,
                    )
            }
        }
    }

    suspend fun finnForrigeUtbetalingsinfo(
        sakId: Long,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): Utbetalingsinfo? =
        finnUtbetalingsinfoNullable(
            behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo).id,
            virkningstidspunkt,
            brukerTokenInfo,
        )

    suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): Utbetalingsinfo =
        requireNotNull(finnUtbetalingsinfoNullable(behandlingId, virkningstidspunkt, brukerTokenInfo)) {
            "Utbetalingsinfo er nødvendig, men mangler"
        }

    private suspend fun finnUtbetalingsinfoNullable(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): Utbetalingsinfo? {
        val beregning = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo) ?: return null

        val beregningsperioder =
            beregning.beregningsperioder.map {
                Beregningsperiode(
                    datoFOM = it.datoFOM.atDay(1),
                    datoTOM = it.datoTOM?.atEndOfMonth(),
                    grunnbeloep = Kroner(it.grunnbelop),
                    antallBarn = (it.soeskenFlokk?.size ?: 0) + 1, // Legger til 1 pga at beregning fjerner soeker
                    utbetaltBeloep = Kroner(it.utbetaltBeloep),
                    trygdetid = it.trygdetid,
                    institusjon = it.institusjonsopphold != null,
                )
            }

        val soeskenjustering = beregning.beregningsperioder.any { !it.soeskenFlokk.isNullOrEmpty() }
        val antallBarn = if (soeskenjustering) beregningsperioder.last().antallBarn else 1

        return Utbetalingsinfo(
            antallBarn,
            Kroner(beregningsperioder.hentUtbetaltBeloep()),
            virkningstidspunkt.atDay(1),
            soeskenjustering,
            beregningsperioder,
        )
    }

    suspend fun hentGrunnbeloep(brukerTokenInfo: BrukerTokenInfo) = beregningKlient.hentGrunnbeloep(brukerTokenInfo)

    suspend fun finnAvkortingsinfo(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkortingsinfo? {
        if (sakType == SakType.BARNEPENSJON || vedtakType == VedtakType.OPPHOER) return null

        val ytelseMedGrunnlag = beregningKlient.hentYtelseMedGrunnlag(behandlingId, brukerTokenInfo)

        val beregningsperioder =
            ytelseMedGrunnlag.perioder.map {
                AvkortetBeregningsperiode(
                    datoFOM = it.periode.fom.atDay(1),
                    datoTOM = it.periode.tom?.atEndOfMonth(),
                    inntekt = Kroner(it.aarsinntekt - it.fratrekkInnAar),
                    ytelseFoerAvkorting = Kroner(it.ytelseFoerAvkorting),
                    utbetaltBeloep = Kroner(it.ytelseEtterAvkorting),
                    trygdetid = it.trygdetid,
                )
            }

        val aarsInntekt = ytelseMedGrunnlag.perioder.first().aarsinntekt
        val grunnbeloep = ytelseMedGrunnlag.perioder.first().grunnbelop

        return Avkortingsinfo(
            grunnbeloep = Kroner(grunnbeloep),
            inntekt = Kroner(aarsInntekt),
            virkningsdato = virkningstidspunkt.atDay(1),
            beregningsperioder,
        )
    }

    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val beregning = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)!!

        return trygdetidService.finnTrygdetidsgrunnlag(behandlingId, beregning, brukerTokenInfo)
    }
}
