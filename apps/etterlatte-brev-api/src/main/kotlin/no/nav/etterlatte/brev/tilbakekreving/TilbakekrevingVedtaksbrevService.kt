package no.nav.etterlatte.brev.tilbakekreving

import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.BrevDataFerdigstillingNy
import no.nav.etterlatte.brev.BrevInnholdData
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

data class BrevRequest(
    val spraak: Spraak, // TODO ?
    val sak: Sak,
    val personerISak: PersonerISak,
    val vedtak: VedtakDto,
    val brevInnholdData: BrevInnholdData,
)

class TilbakekrevingVedtaksbrevService(
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val db: BrevRepository,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Brev {
        // TODO valider at ikke finnes fra før etc..
        // TODO flytte henting av data til behandling?

        val avsender = utledAvsender(bruker, brevRequest.vedtak, brevRequest.sak.enhet)

        val (spraak, sak, personerISak, lok) = brevRequest

        val innhold =
            brevbaker.hentRedigerbarTekstFraBrevbakeren(
                BrevbakerRequest.fra(
                    brevKode = brevRequest.brevInnholdData.brevKode.redigering,
                    brevData = ManueltBrevData(),
                    avsender = avsender,
                    soekerOgEventuellVerge = personerISak.soekerOgEventuellVerge(),
                    sakId = sak.id,
                    spraak = spraak,
                    sakType = sak.sakType,
                ),
            )

        val brevKode = brevRequest.brevInnholdData.brevKode
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

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Pdf {
        // TODO valider

        val brev = db.hentBrev(id)

        val brevInnholdData = utledBrevInnholdData(brev, brevRequest)

        // TODO Må dette hentes på nytt ved forhåndsvisning? Det kan endre seg?
        val avsender = utledAvsender(bruker, brevRequest.vedtak, brevRequest.sak.enhet)

        return opprettPdfOgLagre(bruker, brev, brevRequest, brevInnholdData, avsender)
    }

    private fun utledBrevInnholdData(
        brev: Brev,
        brevRequest: BrevRequest,
    ): BrevDataFerdigstillingNy {
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

        return BrevDataFerdigstillingNy(
            innhold = innholdMedVedlegg.innhold(),
            data = brevRequest.brevInnholdData,
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

    private suspend fun opprettPdfOgLagre(
        bruker: BrukerTokenInfo,
        brev: Brev,
        brevRequest: BrevRequest,
        brevInnholdData: BrevData,
        avsender: Avsender,
    ): Pdf {
        val brevbakerRequest =
            BrevbakerRequest.fra(
                brevKode = brevRequest.brevInnholdData.brevKode.ferdigstilling,
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
}
