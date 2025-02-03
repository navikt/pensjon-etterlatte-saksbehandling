package no.nav.etterlatte.brev.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.etterlatte.brev.brevbaker.formaterNavn
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevDTO
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
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

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, behandlingId, sakId)
            }

        val avsender = utledAvsender(bruker, brevRequest.vedtak, brevRequest.sak.enhet)

        return opprettBrev(bruker, behandlingId, brevRequest, avsender, ManueltBrevData())
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        // TODO valider

        val brev = db.hentBrev(id)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, brev.behandlingId!!, brev.sakId)
            }

        // TODO Må dette hentes på nytt ved forhåndsvisning? Det kan endre seg?
        val avsender = utledAvsender(bruker, brevRequest.vedtak, brevRequest.sak.enhet)

        val brevInnholdData = utledBrevInnholdData(brev, brevRequest)

        val brevbakerRequest =
            BrevbakerRequest.fra(
                brevKode = brevRequest.brevKode.ferdigstilling,
                brevData = brevInnholdData,
                avsender = avsender,
                soekerOgEventuellVerge = brevRequest.personerISak.soekerOgEventuellVerge(),
                sakId = brevRequest.sak.id,
                spraak = brev.spraak, // TODO godt nok?,
                sakType = brevRequest.sak.sakType,
            )
        val pdf = brevbaker.genererPdf(brev.id, brevbakerRequest)

        // logger.info("PDF generert ok. Sjekker om den skal lagres og ferdigstilles")
        brev.brevkoder?.let { db.oppdaterBrevkoder(brev.id, it) }

        if (brevRequest.vedtak.status != VedtakStatus.FATTET_VEDTAK) {
            // logger.info("Vedtak status er $vedtakStatus. Avventer ferdigstilling av brev (id=$brevId)")
        } else {
            val saksbehandlerident: String = brevRequest.vedtak.vedtakFattet?.ansvarligSaksbehandler ?: bruker.ident()
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

    // TODO skal utledes i behandling som kaller brev-api
    private suspend fun utledBrevRequest(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
    ): BrevRequest =
        coroutineScope {
            val sak =
                async {
                    behandlingService.hentSak(sakId, bruker)
                }
            val vedtakDeferred =
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

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Kan ikke lage vedtaksbrev for tilbakekreving uten vedtak behandlingId=$behandlingId")

            val personerISak =
                grunnlag.await().let {
                    PersonerISak(
                        innsender = it.mapInnsender(),
                        soeker = it.mapSoeker(null),
                        avdoede = it.mapAvdoede(),
                        verge = verge.await(),
                    )
                }

            BrevRequest(
                brevKode = Brevkoder.TILBAKEKREVING,
                sak = sak.await(),
                personerISak = personerISak,
                vedtak = vedtak,
                tilbakekreving =
                    objectMapper.readValue(
                        (vedtak.innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving.toJson(),
                    ),
                grunnlag = grunnlag.await(),
                verge = verge.await(),
                spraak = grunnlag.await().mapSpraak(),
                utlandstilknytning = null, // TODO må hente behandling...
            )
        }

    private suspend fun utledAvsender(
        bruker: BrukerTokenInfo,
        vedtak: VedtakDto,
        enhet: Enhetsnummer,
    ): Avsender {
        val innloggetSaksbehandlerIdent = bruker.ident() // TODO bør ikke være nødvendig for kun vedtaksbrev?
        val avsender =
            adresseService.hentAvsender(
                request =
                    AvsenderRequest(
                        saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent,
                        attestantIdent = vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent,
                        sakenhet = enhet,
                    ),
                bruker = bruker,
            )
        return avsender
    }

    private fun utledBrevInnholdData(
        brev: Brev,
        brevRequest: BrevRequest,
    ): BrevData {
        val innholdMedVedlegg =
            InnholdMedVedlegg(
                {
                    krevIkkeNull(
                        db.hentBrevPayload(brev.id),
                    ) { "Fant ikke payload for brev ${brev.id}" }.elements
                },
                {
                    krevIkkeNull(db.hentBrevPayloadVedlegg(brev.id)) {
                        "Fant ikke payloadvedlegg for brev ${brev.id}"
                    }
                },
            )

        return TilbakekrevingBrevDTO.fra(
            redigerbart = innholdMedVedlegg.innhold(),
            muligTilbakekreving = brevRequest.tilbakekreving,
            sakType = brevRequest.sak.sakType,
            utlandstilknytningType = brevRequest.utlandstilknytning,
            soekerNavn = brevRequest.personerISak.soeker.formaterNavn(),
        )
    }

    private suspend fun opprettBrev(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        avsender: Avsender,
        brevInnholdData: BrevData,
    ): Brev {
        val (brevKode, spraak, sak, personerISak) = brevRequest

        val innhold =
            brevbaker.hentRedigerbarTekstFraBrevbakeren(
                BrevbakerRequest.fra(
                    brevKode = brevKode.redigering,
                    brevData = brevInnholdData,
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
                mottakere = adresseService.hentMottakere(sak.sakType, personerISak, bruker),
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

data class BrevRequest(
    val brevKode: Brevkoder,
    val spraak: Spraak, // TODO ?
    val sak: Sak,
    val personerISak: PersonerISak,
    val vedtak: VedtakDto,
    val tilbakekreving: Tilbakekreving,
    val grunnlag: Grunnlag,
    val utlandstilknytning: UtlandstilknytningType?,
    val verge: Verge?,
)
