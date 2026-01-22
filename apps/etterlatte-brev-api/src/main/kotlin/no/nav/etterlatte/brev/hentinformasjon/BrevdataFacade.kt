package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.hentGjenlevende
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

data class PersonerISakOgSak(
    val personerISak: PersonerISak,
    val sak: Sak,
)

class BrevdataFacade(
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
) {
    private val logger: Logger = LoggerFactory.getLogger(BrevdataFacade::class.java)

    suspend fun hentPersonerISakforBrev(
        sakId: SakId,
        behandlingId: UUID?,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersonerISakOgSak =
        coroutineScope {
            val sakDeferred = async { behandlingService.hentSak(sakId, brukerTokenInfo) }
            val vedtakDeferred = behandlingId?.let { async { vedtaksvurderingService.hentVedtak(it, brukerTokenInfo) } }
            val vedtakType = vedtakDeferred?.await()?.type
            val grunnlag = grunnlagService.hentGrunnlag(vedtakType, sakId, brukerTokenInfo, behandlingId)
            val sak = sakDeferred.await()
            val brevutfallDeferred = behandlingId?.let { async { behandlingService.hentBrevutfall(it, brukerTokenInfo) } }

            val brevutfallDto = brevutfallDeferred?.await()
            val verge = grunnlagService.hentVergeForSak(sak.sakType, brevutfallDto, grunnlag)
            val personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(brevutfallDto?.aldersgruppe),
                    avdoede = grunnlag.mapAvdoede(),
                    verge = verge,
                )
            PersonerISakOgSak(
                personerISak = personerISak,
                sak = sak,
            )
        }

    suspend fun hentGenerellBrevData(
        sakId: SakId,
        behandlingId: UUID?,
        overstyrSpraak: Spraak? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): GenerellBrevData =
        coroutineScope {
            val sakDeferred = async { behandlingService.hentSak(sakId, brukerTokenInfo) }
            val vedtakDeferred = behandlingId?.let { async { vedtaksvurderingService.hentVedtak(it, brukerTokenInfo) } }
            val brevutfallDeferred =
                behandlingId?.let { async { behandlingService.hentBrevutfall(it, brukerTokenInfo) } }

            val vedtakType = vedtakDeferred?.await()?.type
            val grunnlag = grunnlagService.hentGrunnlag(vedtakType, sakId, brukerTokenInfo, behandlingId)

            val sak = sakDeferred.await()
            val brevutfallDto = brevutfallDeferred?.await()
            val verge = grunnlagService.hentVergeForSak(sak.sakType, brevutfallDto, grunnlag)
            val personerISak =
                PersonerISak(
                    innsender = grunnlag.mapInnsender(),
                    soeker = grunnlag.mapSoeker(brevutfallDto?.aldersgruppe),
                    avdoede = grunnlag.mapAvdoede(),
                    verge = verge,
                    gjenlevende = grunnlag.hentGjenlevende(),
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
            val systemkilde = behandling?.vedtaksloesning ?: Vedtaksloesning.GJENNY // Dette kan være en pesys-sak
            val spraak = overstyrSpraak ?: grunnlag.mapSpraak()
            val relatertBehandlingId = behandling?.relatertBehandlingId

            val forenkletVedtak =
                forenkletVedtak(
                    vedtak,
                    sak,
                    saksbehandlerIdent,
                    attestantIdent,
                    brukerTokenInfo,
                    relatertBehandlingId,
                    behandling,
                )
            val revurderingaarsak = behandling?.revurderingsaarsak

            GenerellBrevData(
                sak = sak,
                personerISak = personerISak,
                behandlingId = behandlingId,
                forenkletVedtak = forenkletVedtak,
                spraak = spraak,
                revurderingsaarsak = revurderingaarsak,
                systemkilde = systemkilde,
                utlandstilknytning = behandling?.utlandstilknytning,
                prosesstype = behandling?.prosesstype,
            )
        }

    private suspend fun forenkletVedtak(
        vedtak: VedtakDto?,
        sak: Sak,
        saksbehandlerIdent: String,
        attestantIdent: String?,
        bruker: BrukerTokenInfo,
        relatertBehandlingId: String?,
        behandling: DetaljertBehandling?,
    ): ForenkletVedtak? =
        when (vedtak?.type) {
            VedtakType.INNVILGELSE,
            VedtakType.OPPHOER,
            VedtakType.AVSLAG,
            VedtakType.ENDRING,
            -> {
                (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).let { vedtakInnhold ->
                    ForenkletVedtak(
                        vedtak.id,
                        vedtak.status,
                        vedtak.type,
                        sak.enhet,
                        saksbehandlerIdent,
                        attestantIdent,
                        vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                        virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
                        klage =
                            hentKlageForBehandling(relatertBehandlingId, behandling, bruker)?.also {
                                logger.info(
                                    "Hentet klage med id=$relatertBehandlingId fra behandling med id=${behandling?.id} " +
                                        "for behandlingType=${behandling?.behandlingType} " +
                                        "omgjøring etter klage i sak=${behandling?.sak?.sakId}, " +
                                        "med klageStatus=${it.status}",
                                )
                            },
                    )
                }
            }

            VedtakType.TILBAKEKREVING -> {
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
                )
            }

            VedtakType.AVVIST_KLAGE -> {
                ForenkletVedtak(
                    vedtak.id,
                    vedtak.status,
                    vedtak.type,
                    sak.enhet,
                    saksbehandlerIdent,
                    attestantIdent,
                    vedtak.vedtakFattet?.tidspunkt?.toNorskLocalDate(),
                    klage =
                        objectMapper.readValue(
                            (vedtak.innhold as VedtakInnholdDto.Klage).klage.toJson(),
                        ),
                )
            }

            VedtakType.INGEN_ENDRING -> {
                throw InternfeilException("VedtakType INGEN_ENDRING kun tilgjengelig i ny brevflyt")
            }

            null -> {
                null
            }
        }

    suspend fun hentKlageForBehandling(
        relatertBehandlingId: String?,
        behandling: DetaljertBehandling?,
        bruker: BrukerTokenInfo,
    ): Klage? =
        if (behandling?.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING && relatertBehandlingId != null) {
            try {
                val klageId = UUID.fromString(relatertBehandlingId)
                behandlingService.hentKlage(klageId, bruker)
            } catch (e: Exception) {
                logger.error("Fant ikke klage med id=$relatertBehandlingId", e)
                logger.info(
                    "Kunne ikke finne klage med id=$relatertBehandlingId, denne førstegangsbehandlingen med id=${behandling.id} gjelder ikke omgjøring på grunn av klage",
                )
                null
            }
        } else if (behandling?.revurderingsaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
            val klageId = UUID.fromString(relatertBehandlingId)
            behandlingService.hentKlage(klageId, bruker)
        } else {
            null
        }
}
