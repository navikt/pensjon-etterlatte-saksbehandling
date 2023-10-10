package no.nav.etterlatte.brev.hentinformasjon

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.AvkortetBeregningsperiode
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.behandling.hentUtbetaltBeloep
import no.nav.etterlatte.brev.behandling.mapAvdoed
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.behandling.mapVerge
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

interface IBehandlingService {
    suspend fun hentBehandling(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling
}

class BehandlingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingKlient: BehandlingKlient,
    private val trygdetidKlient: TrygdetidKlient,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
) : IBehandlingService {
    override suspend fun hentBehandling(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling =
        coroutineScope {
            val vedtak = async { vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo) }
            val grunnlag = async { grunnlagKlient.hentGrunnlag(sakId, brukerTokenInfo) }
            val sak = async { behandlingKlient.hentSak(sakId, brukerTokenInfo) }
            val vilkaarsvurdering =
                async { vilkaarsvurderingKlient.hentVilkaarsvurdering(behandlingId, brukerTokenInfo) }
            val etterbetaling = async { behandlingKlient.hentEtterbetaling(behandlingId, brukerTokenInfo) }
            val grunnbeloep = async { beregningKlient.hentGrunnbeloep(brukerTokenInfo) }

            mapBehandling(
                vedtak.await(),
                grunnlag.await(),
                sak.await(),
                vilkaarsvurdering.await(),
                etterbetaling.await(),
                grunnbeloep.await(),
                brukerTokenInfo,
            )
        }

    private suspend fun mapBehandling(
        vedtak: VedtakDto,
        grunnlag: Grunnlag,
        sak: Sak,
        vilkaarsvurdering: VilkaarsvurderingDto,
        etterbetaling: EtterbetalingDTO?,
        grunnbeloep: Grunnbeloep,
        brukerTokenInfo: BrukerTokenInfo,
    ): Behandling {
        val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()
        val ansvarligEnhet = vedtak.vedtakFattet?.ansvarligEnhet ?: sak.enhet
        val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
        val attestantIdent = vedtak.vedtakFattet?.let { vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent }

        val opprinneligInnvilgelsesdato =
            if (vedtak.behandling.revurderingInfo != null) {
                vedtaksvurderingKlient.hentInnvilgelsesdato(sak.id, brukerTokenInfo)
            } else {
                null
            }

        return Behandling(
            sakId = vedtak.sak.id,
            sakType = vedtak.sak.sakType,
            behandlingId = vedtak.behandling.id,
            spraak = grunnlag.mapSpraak(),
            personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(),
                    avdoed = grunnlag.mapAvdoed(), // TODO: Avdød kan potensielt være liste (begge foreldre døde).
                    verge = grunnlag.mapVerge(vedtak.sak.sakType),
                ),
            vedtak =
                ForenkletVedtak(
                    vedtak.vedtakId,
                    vedtak.status,
                    vedtak.type,
                    ansvarligEnhet,
                    saksbehandlerIdent,
                    attestantIdent,
                    vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                ),
            utbetalingsinfo =
                if (vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingUtfall.OPPFYLT) {
                    finnUtbetalingsinfo(
                        vedtak.behandling.id,
                        vedtak.virkningstidspunkt,
                        brukerTokenInfo,
                    )
                } else {
                    null
                },
            forrigeUtbetalingsinfo = finnForrigeUbetalingsinfo(vedtak, sak, brukerTokenInfo),
            avkortingsinfo =
                finnYtelseMedGrunnlag(
                    vedtak.behandling.id,
                    vedtak.sak.sakType,
                    vedtak.virkningstidspunkt,
                    vedtak.type,
                    brukerTokenInfo,
                ),
            revurderingsaarsak = vedtak.behandling.revurderingsaarsak,
            revurderingInfo = vedtak.behandling.revurderingInfo,
            virkningsdato = vedtak.virkningstidspunkt,
            opprinneligInnvilgelsesdato = opprinneligInnvilgelsesdato,
            adopsjonsdato = LocalDate.now(), // TODO: Denne må vi hente anten frå PDL eller brukarinput
            trygdetid =
                finnTrygdetid(
                    vedtak.behandling.id,
                    brukerTokenInfo,
                ),
            vilkaarsvurdering = vilkaarsvurdering,
            etterbetalingDTO = etterbetaling,
            grunnbeloep = grunnbeloep,
        )
    }

    private suspend fun finnForrigeUbetalingsinfo(
        vedtak: VedtakDto,
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Utbetalingsinfo? {
        val forrigeUtbetalingsinfo =
            when (vedtak.behandling.type) {
                BehandlingType.REVURDERING -> {
                    val sisteIverksatteBehandling =
                        behandlingKlient.hentSisteIverksatteBehandling(sak.id, brukerTokenInfo)
                    finnUtbetalingsinfo(
                        sisteIverksatteBehandling.id,
                        vedtak.virkningstidspunkt,
                        brukerTokenInfo,
                    )
                }

                else -> null
            }
        return forrigeUtbetalingsinfo
    }

    private suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)

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

    private suspend fun finnYtelseMedGrunnlag(
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

    private suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val trygdetidMedGrunnlag = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo) ?: return null

        val trygdetidsperioder =
            trygdetidMedGrunnlag.trygdetidGrunnlag.map {
                Trygdetidsperiode(
                    datoFOM = it.periodeFra,
                    datoTOM = it.periodeTil,
                    land = it.bosted,
                    opptjeningsperiode = it.beregnet?.aar.toString(),
                )
            }

        val beregnetTrygdetid = trygdetidMedGrunnlag.beregnetTrygdetid?.resultat
        val samlaTrygdetid = beregnetTrygdetid?.samletTrygdetidNorge ?: beregnetTrygdetid?.samletTrygdetidTeoretisk ?: 0
        val aarTrygdetid = samlaTrygdetid % 12

        return Trygdetid(
            aarTrygdetid = aarTrygdetid,
            maanederTrygdetid = samlaTrygdetid - aarTrygdetid,
            perioder = trygdetidsperioder,
        )
    }
}
