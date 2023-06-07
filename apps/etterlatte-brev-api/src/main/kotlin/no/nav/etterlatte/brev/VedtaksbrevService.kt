package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.SlateHelper.hentInitiellPayload
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class VedtaksbrevService(
    private val db: BrevRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val dokarkivService: DokarkivServiceImpl,
    private val brevbaker: BrevbakerKlient
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        logger.info("Henter brev (id=$id)")

        return db.hentBrev(id)
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId)
    }

    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): Brev {
        require(hentVedtaksbrev(behandlingId) == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)

        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)

        val vedtakType = behandling.vedtak.type

        val tittel = "Vedtak om ${vedtakType.name.lowercase()}"
        val innhold = BrevInnhold(tittel, behandling.spraak)

        val nyttBrev = OpprettNyttBrev(
            sakId = sakId,
            behandlingId = behandling.behandlingId,
            prosessType = BrevProsessType.fra(behandling.sakType, vedtakType),
            soekerFnr = behandling.persongalleri.soeker.fnr.value,
            mottaker = mottaker,
            innhold = innhold
        )

        return db.opprettBrev(nyttBrev)
    }

    suspend fun genererPdf(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): Pdf {
        val brev = requireNotNull(hentVedtaksbrev(behandlingId))

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id))
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)
        val avsender = adresseService.hentAvsender(behandling.vedtak)

        val brevData = opprettBrevData(brev, behandling)
        val brevRequest = BrevbakerRequest.fra(brevData, behandling, avsender)

        return genererPdf(brev.id, brevRequest)
            .also { pdf -> ferdigstillHvisVedtakFattet(brev, behandling, pdf, bruker) }
    }

    private fun opprettBrevData(brev: Brev, behandling: Behandling): BrevData =
        when (brev.prosessType) {
            BrevProsessType.AUTOMATISK -> BrevDataMapper.fra(behandling)
            BrevProsessType.MANUELL -> {
                val payload = requireNotNull(db.hentBrevPayload(brev.id))

                ManueltBrevData(payload.elements)
            }
        }

    private fun ferdigstillHvisVedtakFattet(brev: Brev, behandling: Behandling, pdf: Pdf, bruker: Bruker) {
        if (behandling.vedtak.status != VedtakStatus.FATTET_VEDTAK) {
            logger.info("Vedtak status er ${behandling.vedtak.status}. Avventer ferdigstilling av brev (id=${brev.id})")
            return
        }

        if (behandling.vedtak.saksbehandlerIdent != bruker.ident()) {
            logger.info("Ferdigstiller brev med id=${brev.id}")

            db.lagrePdfOgFerdigstillBrev(brev.id, pdf)
        } else {
            logger.warn(
                "Kan ikke ferdigstille/låse brev når saksbehandler (${behandling.vedtak.saksbehandlerIdent})" +
                    " og attestant (${bruker.ident()}) er samme person."
            )
        }
    }

    suspend fun hentManueltBrevPayload(behandlingId: UUID, sakId: Long, bruker: Bruker): Slate {
        val brev = requireNotNull(hentVedtaksbrev(behandlingId))

        return db.hentBrevPayload(brev.id)
            ?: initBrevPayload(brev.id, behandlingId, sakId, bruker)
    }

    private suspend fun initBrevPayload(id: BrevID, behandlingId: UUID, sakId: Long, bruker: Bruker): Slate {
        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)

        return hentInitiellPayload(behandling.sakType, behandling.vedtak.type)
            .also { slate -> lagreManueltBrevPayload(id, slate) }
    }

    fun lagreManueltBrevPayload(id: BrevID, payload: Slate) {
        db.oppdaterPayload(id, payload)
            .let { logger.info("Payload for brev (id=$id) oppdatert") }
    }

    fun journalfoerVedtaksbrev(vedtaksbrev: Brev, vedtak: VedtakTilJournalfoering): Pair<Brev, JournalpostResponse> {
        if (vedtaksbrev.status != Status.FERDIGSTILT) {
            throw IllegalArgumentException("Ugyldig status ${vedtaksbrev.status} på vedtaksbrev (id=${vedtaksbrev.id})")
        }

        val response = dokarkivService.journalfoer(vedtaksbrev, vedtak)

        db.settBrevJournalfoert(vedtaksbrev.id, response)
            .also { logger.info("Brev med id=${vedtaksbrev.id} markert som journalført") }

        return vedtaksbrev to response
    }

    fun slettVedtaksbrev(id: BrevID): Boolean {
        logger.info("Sletter vedtaksbrev (id=$id)")

        return db.slett(id)
    }

    private suspend fun genererPdf(brevID: BrevID, brevRequest: BrevbakerRequest): Pdf {
        val brevbakerResponse = brevbaker.genererPdf(brevRequest)

        return Base64.getDecoder().decode(brevbakerResponse.base64pdf)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }
}