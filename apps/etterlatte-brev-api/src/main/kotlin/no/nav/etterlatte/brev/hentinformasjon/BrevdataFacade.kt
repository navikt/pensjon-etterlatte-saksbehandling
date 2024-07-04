package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sikkerLogg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class BrevdataFacade(
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
    private val adresseService: AdresseService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentGenerellBrevData(
        sakId: Long,
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
            val verge = hentVergeForSak(sak.sakType, brevutfallDto, grunnlag)
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

    suspend fun hentVergeForSak(
        sakType: SakType,
        brevutfallDto: BrevutfallDto?,
        grunnlag: Grunnlag,
    ): Verge? {
        val verger =
            hentVerger(
                grunnlag.soeker
                    .hentSoekerPdlV1()!!
                    .verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
                grunnlag.soeker.hentFoedselsnummer()?.verdi,
            )
        return if (verger.size == 1) {
            val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident!!
            val vergenavn =
                adresseService
                    .hentMottakerAdresse(sakType, vergeFnr.value)
                    .navn
            Vergemaal(
                vergenavn,
                vergeFnr,
            )
        } else if (verger.size > 1) {
            logger.info(
                "Fant flere verger for bruker med fnr ${grunnlag.soeker.hentFoedselsnummer()?.verdi} i " +
                    "mapping av verge til brev.",
            )
            sikkerLogg.info(
                "Fant flere verger for bruker med fnr ${
                    grunnlag.soeker.hentFoedselsnummer()?.verdi?.value
                } i mapping av verge til brev.",
            )
            UkjentVergemaal()
        } else if (sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(brevutfallDto)) {
            grunnlag.hentForelderVerge()
        } else {
            null
        }
    }
}

class UgyldigBeregningsMetode :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGS_METODE",
        detail =
            "Kan ikke ha brukt beregningsmetode 'BEST' i en faktisk beregning, " +
                "siden best velger mellom nasjonal eller prorata når det beregnes.",
    )

class SamletTeoretiskTrygdetidMangler :
    UgyldigForespoerselException(
        code = "SAMLET_TEORETISK_TRYGDETID_MANGLER",
        detail = "Samlet teoretisk trygdetid mangler i beregningen",
    )

class BeregningsperiodeBroekMangler :
    UgyldigForespoerselException(
        code = "BEREGNINGSPERIODE_BROEK_MANGLER",
        detail = "Beregningsperioden mangler brøk",
    )
