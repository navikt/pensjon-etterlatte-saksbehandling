package no.nav.etterlatte.brev.tilbakekreving

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
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
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
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
        sakId: SakId,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev {
        // TODO valider at ikke finnes fra før etc..
        // TODO flytte henting av data til behandling?
        val brevSakData =
            retryOgPakkUt {
                utledBrevSakData(bruker, behandlingId, sakId)
            }

        val brevSendeData = utledBrevSendeData(bruker, brevSakData)

        return opprettBrev(bruker, behandlingId, brevSendeData, brevSakData.sak)
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        // TODO valider

        val brev = db.hentBrev(id)

        val brevSakData =
            retryOgPakkUt {
                utledBrevSakData(bruker, brev.behandlingId!!, brev.sakId)
            }

        val brevSendeData = utledBrevSendeData(bruker, brevSakData)

        val pdf =
            genererPdf(
                sak = brevSakData.sak,
                brev,
                brevSendeData,
            )

        // logger.info("PDF generert ok. Sjekker om den skal lagres og ferdigstilles")
        brev.brevkoder?.let { db.oppdaterBrevkoder(brev.id, it) }

        if (brevSakData.vedtak.status != VedtakStatus.FATTET_VEDTAK) {
            // logger.info("Vedtak status er $vedtakStatus. Avventer ferdigstilling av brev (id=$brevId)")
        } else {
            val saksbehandlerident: String = brevSakData.vedtak.vedtakFattet?.ansvarligSaksbehandler ?: bruker.ident()
            if (!bruker.erSammePerson(saksbehandlerident)) {
                // logger.info("Lagrer PDF for brev med id=$brevId")
                db.lagrePdf(brev.id, pdf)
            } else {
                /*
                logger.warn(
                    "Kan ikke ferdigstille/låse brev når saksbehandler ($saksbehandlerVedtak)" +
                        " og attestant (${brukerTokenInfo.ident()}) er samme person.",
                )
                 */
            }
        }
        return pdf
    }

    private suspend fun utledBrevSakData(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
    ): BrevSakData =
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

            BrevSakData(
                sak = sak.await(),
                vedtak =
                    vedtak.await()
                        ?: throw InternfeilException("Kan ikke lage vedtaksbrev for tilbakekreving uten vedtak behandlingId=$behandlingId"),
                grunnlag = grunnlag.await(),
                verge = verge.await(),
            )
        }

    private suspend fun utledBrevSendeData(
        bruker: BrukerTokenInfo,
        brevdata: BrevSakData,
        brevInnholdData: BrevData = ManueltBrevData(),
        overstyrtSpraak: Spraak? = null, // TODO skal benyttes ved tilbakestilling av brev
    ): BrevSendeData {
        val (sak, vedtak, grunnlag, verge) = brevdata

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

        return BrevSendeData(
            brevKode = Brevkoder.TILBAKEKREVING,
            brevinnholdData = brevInnholdData,
            avsender = avsender,
            personerISak = personerISak,
            spraak = spraak,
        )
    }

    private suspend fun opprettBrev(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        brevSendeData: BrevSendeData,
        sak: Sak,
    ): Brev {
        val (brevKode, brevinnholdData, personerISak, spraak, avsender) = brevSendeData

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

    private suspend fun genererPdf(
        sak: Sak,
        brev: Brev,
        brevSendeData: BrevSendeData,
    ): Pdf {
        val (brevKode, brevinnholdData, personerISak, spraak, avsender) = brevSendeData

        val brevRequest =
            BrevbakerRequest.fra(
                brevKode = brevKode.ferdigstilling,
                brevData = brevinnholdData,
                avsender = avsender,
                soekerOgEventuellVerge = personerISak.soekerOgEventuellVerge(),
                sakId = sak.id,
                spraak = brev.spraak, // TODO godt nok?,
                sakType = sak.sakType,
            )

        return brevbaker.genererPdf(brev.id, brevRequest)
    }
}

data class BrevSakData(
    val sak: Sak,
    val vedtak: VedtakDto,
    val grunnlag: Grunnlag,
    val verge: Verge?,
)

data class BrevSendeData(
    val brevKode: Brevkoder,
    val brevinnholdData: BrevData,
    val personerISak: PersonerISak,
    val spraak: Spraak,
    val avsender: Avsender,
)
