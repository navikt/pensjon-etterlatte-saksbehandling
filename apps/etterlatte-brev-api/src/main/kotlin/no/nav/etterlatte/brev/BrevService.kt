package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.LanguageCode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.DistribusjonService
import no.nav.etterlatte.brev.distribusjon.DistribusjonsTidspunktType
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class BrevService(
    private val db: BrevRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val dokarkivService: DokarkivService,
    private val distribusjonService: DistribusjonService,
    private val brevbakerKlient: BrevbakerKlient
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        return db.hentBrev(id)
    }

    fun hentBrevForSak(sakId: Long): List<Brev> {
        return db.hentBrevForSak(sakId)
    }

    suspend fun opprettBrev(sakId: Long, bruker: BrukerTokenInfo): Brev {
        val sak = sakOgBehandlingService.hentSak(sakId, bruker)

        val mottaker = adresseService.hentMottakerAdresse(sak.ident)

        val nyttBrev = OpprettNyttBrev(
            sakId = sakId,
            behandlingId = null,
            soekerFnr = sak.ident,
            prosessType = BrevProsessType.MANUELL,
            mottaker = mottaker,
            innhold = BrevInnhold("Nytt brev", Spraak.NB, SlateHelper.opprettTomBrevmal())
        )

        return db.opprettBrev(nyttBrev)
    }

    fun hentBrevPayload(id: BrevID): Slate? =
        db.hentBrevPayload(id)
            .also { logger.info("Hentet payload for brev (id=$id)") }

    fun lagreBrevPayload(id: BrevID, payload: Slate) =
        db.oppdaterPayload(id, payload)
            .also { logger.info("Payload for brev (id=$id) oppdatert") }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo
    ): Pdf {
        val brev = hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id))
        }

        val sak = sakOgBehandlingService.hentSak(brev.sakId, bruker)
        val soeker = sakOgBehandlingService.hentSoeker(brev.sakId, bruker)
        val avsender = adresseService.hentAvsender(sak, bruker.ident())

        val (brevKode, brevData) = opprettBrevData(brev)
        val brevRequest = BrevbakerRequest(
            kode = brevKode,
            letterData = brevData,
            felles = BrevbakerHelpers.mapFelles(
                sakId = brev.sakId,
                soeker = soeker,
                avsender = avsender
            ),
            language = LanguageCode.spraakToLanguageCode(Spraak.NB) // TODO: fikse spraak
        )

        return genererPdf(brev.id, brevRequest)
    }

    suspend fun ferdigstill(id: BrevID, bruker: BrukerTokenInfo) {
        val brev = hentBrev(id)

        if (brev.status in listOf(Status.OPPRETTET, Status.OPPDATERT)) {
            val pdf = genererPdf(id, bruker)

            db.lagrePdfOgFerdigstillBrev(id, pdf)
        } else {
            throw IllegalStateException("Kan ikke ferdigstille brev (id=$id) med status ${brev.status}")
        }
    }

    suspend fun journalfoer(id: BrevID, bruker: BrukerTokenInfo): String {
        val brev = hentBrev(id)

        if (brev.status != Status.FERDIGSTILT) {
            throw IllegalStateException("Ugyldig status ${brev.status} på brev (id=${brev.id})")
        }

        val sak = sakOgBehandlingService.hentSak(brev.sakId, bruker)

        val response = dokarkivService.journalfoer(brev, sak)

        db.settBrevJournalfoert(brev.id, response)
        logger.info("Brev med id=${brev.id} markert som journalført")

        return response.journalpostId
    }

    fun distribuer(id: BrevID): String {
        val brev = hentBrev(id)

        if (brev.status != Status.JOURNALFOERT) {
            throw IllegalStateException(
                "Forventet status ${Status.JOURNALFOERT} på brev (id=${brev.id}), men fikk ${brev.status}"
            )
        }

        val journalpostId = requireNotNull(db.hentJournalpostId(id)) {
            "JournalpostID mangler på brev (id=${brev.id}, status=${brev.status})"
        }

        return distribusjonService.distribuerJournalpost(
            brevId = brev.id,
            journalpostId = journalpostId,
            type = DistribusjonsType.ANNET,
            tidspunkt = DistribusjonsTidspunktType.KJERNETID,
            adresse = brev.mottaker.adresse
        )
    }

    private fun opprettBrevData(brev: Brev): Pair<EtterlatteBrevKode, BrevData> {
        val payload = requireNotNull(db.hentBrevPayload(brev.id))

        return Pair(EtterlatteBrevKode.OMS_INNVILGELSE_MANUELL, ManueltBrevData(payload.elements))
    }

    private suspend fun genererPdf(brevID: BrevID, brevRequest: BrevbakerRequest): Pdf {
        val brevbakerResponse = brevbakerKlient.genererPdf(brevRequest)

        return Base64.getDecoder().decode(brevbakerResponse.base64pdf)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }
}