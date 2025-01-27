package no.nav.etterlatte.brev.tilbakekreving

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class TilbakekrevingVedtaksbrevService(
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
) {
    suspend fun opprettVedtaksbrev(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
    ): Brev {
        // TODO valider at ikke finnes fra før etc..

        // TODO flytte henting av data til behandling?
        val brevdata =
            retryOgPakkUt {
                tilbakekrevingBrevData(bruker, behandlingId, sakId)
            }

        return opprettBrev(bruker, behandlingId, brevdata)
    }

    private suspend fun tilbakekrevingBrevData(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
    ): TilbakekrevingBrevData =
        coroutineScope {
            val sak =
                async {
                    behandlingService.hentSak(sakId, bruker)
                }
            val vedtak =
                async {
                    vedtaksvurderingService.hentVedtak(behandlingId, bruker)
                }
            val grunnlag =
                async {
                    grunnlagService.hentGrunnlag(VedtakType.TILBAKEKREVING, sakId, bruker, behandlingId)
                }
            val verge =
                async {
                    grunnlagService.hentVergeForSak(sak.await().sakType, null, grunnlag.await())
                }

            TilbakekrevingBrevData(
                sak = sak.await(),
                vedtak =
                    vedtak.await()
                        ?: throw InternfeilException("Kan ikke lage vedtaksbrev for tilbakekreving uten vedtak behandlingId=$behandlingId"),
                grunnlag = grunnlag.await(),
                verge = verge.await(),
            )
        }

    // TODO tilbakestill
    // overstyrt språk...

    private suspend fun opprettBrev(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        brevData: TilbakekrevingBrevData,
        overstyrtSpraak: Spraak? = null, // TODO skal benyttes ved tilbakestilling av brev
    ): Brev {
        val brevKode = Brevkoder.TILBAKEKREVING
        val brevinnholdData = ManueltBrevData()

        val (sak, vedtak, grunnlag, verge) = brevData

        val innloggetSaksbehandlerIdent = bruker.ident() // TODO bør ikke være nødvendig for kun vedtaksbrev?
        val avsender =
            adresseService.hentAvsender(
                request =
                    AvsenderRequest(
                        saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent,
                        attestantIdent = vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent,
                        sakenhet = sak.enhet,
                    ),
                bruker = bruker,
            )

        val personerISak =
            PersonerISak(
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = verge,
            )

        val spraak = overstyrtSpraak ?: grunnlag.mapSpraak()

        val innhold =
            brevbaker.hentRedigerbarTekstFraBrevbakeren(
                BrevbakerRequest.fra(
                    brevKode = brevKode.redigering,
                    brevData = brevinnholdData,
                    avsender = avsender,
                    soekerOgEventuellVerge = personerISak.soekerOgEventuellVerge(),
                    sakId = sak.id,
                    spraak = spraak,
                    sakType = sak.sakType,
                ),
            )

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sak.id,
                behandlingId = behandlingId,
                prosessType = BrevProsessType.REDIGERBAR,
                soekerFnr = personerISak.soeker.fnr.value,
                mottakere = adresseService.hentMottakere(sak.sakType, personerISak),
                opprettet = Tidspunkt.now(),
                innhold =
                    BrevInnhold(
                        brevKode.titlerPaaSpraak[spraak] ?: brevKode.tittel,
                        spraak,
                        innhold,
                    ),
                innholdVedlegg = emptyList(),
                brevtype = brevKode.brevtype,
                brevkoder = brevKode,
            )

        return db.opprettBrev(nyttBrev, bruker)
    }
}

private data class TilbakekrevingBrevData(
    val sak: Sak,
    val vedtak: VedtakDto,
    val grunnlag: Grunnlag,
    val verge: Verge?,
)
