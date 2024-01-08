package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.LanguageCode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.SakService
import no.nav.etterlatte.brev.hentinformasjon.SoekerService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.ManueltBrevMedTittelData
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class BrevService(
    private val db: BrevRepository,
    private val sakService: SakService,
    private val soekerService: SoekerService,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
    private val brevDataFacade: BrevdataFacade,
    private val journalfoerBrevService: JournalfoerBrevService,
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        return db.hentBrev(id)
    }

    fun hentBrevForSak(sakId: Long): List<Brev> {
        return db.hentBrevForSak(sakId)
    }

    suspend fun opprettBrev(
        sakId: Long,
        bruker: BrukerTokenInfo,
    ): Brev {
        val sak = sakService.hentSak(sakId, bruker)

        val mottaker = adresseService.hentMottakerAdresse(sak.ident)

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sakId,
                behandlingId = null,
                soekerFnr = sak.ident,
                prosessType = BrevProsessType.MANUELL,
                mottaker = mottaker,
                opprettet = Tidspunkt.now(),
                innhold = BrevInnhold("Tittel mangler", Spraak.NB, SlateHelper.opprettTomBrevmal()),
                innholdVedlegg = null,
            )

        return db.opprettBrev(nyttBrev)
    }

    data class BrevPayload(
        val hoveddel: Slate?,
        val vedlegg: List<BrevInnholdVedlegg>?,
    )

    fun hentBrevPayload(id: BrevID): BrevPayload {
        val hoveddel =
            db.hentBrevPayload(id)
                .also { logger.info("Hentet payload for brev (id=$id)") }

        val vedlegg =
            db.hentBrevPayloadVedlegg(id)
                .also { logger.info("Hentet payload til vedlegg for brev (id=$id)") }

        return BrevPayload(hoveddel, vedlegg)
    }

    fun lagreBrevPayload(
        id: BrevID,
        payload: Slate,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterPayload(id, payload)
            .also { logger.info("Payload for brev (id=$id) oppdatert") }
    }

    fun lagreBrevPayloadVedlegg(
        id: BrevID,
        payload: List<BrevInnholdVedlegg>,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterPayloadVedlegg(id, payload)
            .also { logger.info("Vedlegg payload for brev (id=$id) oppdatert") }
    }

    fun oppdaterMottaker(
        id: BrevID,
        mottaker: Mottaker,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterMottaker(id, mottaker)
            .also { logger.info("Mottaker på brev (id=$id) oppdatert") }
    }

    fun oppdaterTittel(
        id: BrevID,
        tittel: String,
    ): Int {
        sjekkOmBrevKanEndres(id)
        return db.oppdaterTittel(id, tittel)
            .also { logger.info("Tittel på brev (id=$id) oppdatert") }
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val brev = hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id))
        }

        val brevutfall =
            brev.behandlingId?.let {
                brevDataFacade.hentBrevutfall(brev.behandlingId, bruker)
            }
        val sak = sakService.hentSak(brev.sakId, bruker)
        val soeker = soekerService.hentSoeker(brev.sakId, bruker, brevutfall)
        val avsender =
            adresseService.hentAvsender(AvsenderRequest(saksbehandlerIdent = bruker.ident(), sakenhet = sak.enhet))

        val (brevKode, brevData) = opprettBrevData(brev)
        val brevRequest =
            BrevbakerRequest(
                kode = brevKode,
                letterData = brevData,
                felles =
                    BrevbakerHelpers.mapFelles(
                        sakId = brev.sakId,
                        soeker = soeker,
                        avsender = avsender,
                    ),
                language = LanguageCode.spraakToLanguageCode(Spraak.NB), // TODO: fikse spraak
            )

        return brevbakerService.genererPdf(brev.id, brevRequest)
    }

    suspend fun ferdigstill(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) {
        sjekkOmBrevKanEndres(id)
        val pdf = genererPdf(id, bruker)
        db.lagrePdfOgFerdigstillBrev(id, pdf)
    }

    suspend fun journalfoer(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ) = journalfoerBrevService.journalfoer(id, bruker)

    private fun sjekkOmBrevKanEndres(brevID: BrevID) {
        val brev = db.hentBrev(brevID)
        if (!brev.kanEndres()) {
            throw IllegalStateException("Brev med id=$brevID kan ikke endres, siden det har status ${brev.status}")
        }
    }

    private fun opprettBrevData(brev: Brev): Pair<EtterlatteBrevKode, BrevData> {
        val payload = requireNotNull(db.hentBrevPayload(brev.id))

        return Pair(EtterlatteBrevKode.TOM_MAL_INFORMASJONSBREV, ManueltBrevMedTittelData(payload.elements, brev.tittel))
    }
}
