package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerKlient
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.LanguageCode
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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

        val nyttBrev = OpprettNyttBrev(
            behandlingId = behandling.behandlingId,
            prosessType = hentProsessType(behandling.sakType, vedtakType),
            soekerFnr = behandling.persongalleri.soeker.fnr.value,
            tittel = "Vedtak om ${vedtakType.name.lowercase()}",
            mottaker = mottaker
        )

        return db.opprettBrev(nyttBrev)
    }

    suspend fun genererPdf(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): ByteArray {
        val brev = requireNotNull(hentVedtaksbrev(behandlingId))

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentBrevInnhold(brev.id)?.data)
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, bruker)

        return when (brev.prosessType) {
            BrevProsessType.AUTOMATISK -> genererPdfForAutomatiskBrev(brev, behandling, bruker)
            BrevProsessType.MANUELL -> genererPdfForManueltBrev(brev, behandling, bruker)
        }
    }

    private suspend fun genererPdfForAutomatiskBrev(
        brev: Brev,
        behandling: Behandling,
        bruker: Bruker
    ): ByteArray {
        val avsender = adresseService.hentAvsender(behandling.vedtak)

        val brevRequest = BrevbakerRequest.fra(behandling, avsender, brev.mottaker)
        val pdf = genererPdf(brev.id, brevRequest)

        hvisInnholdKanFerdigstilles(brev.id, behandling, bruker) {
            db.opprettInnholdOgFerdigstill(brev.id, BrevInnhold(behandling.spraak, data = pdf))
        }

        return pdf
    }

    private suspend fun genererPdfForManueltBrev(
        brev: Brev,
        behandling: Behandling,
        bruker: Bruker
    ): ByteArray {
        val avsender = adresseService.hentAvsender(behandling.vedtak)

        val innhold = requireNotNull(db.hentBrevInnhold(brev.id))

        val brevRequest = BrevbakerRequest(
            kode = EtterlatteBrevKode.OMS_INNVILGELSE_MANUELL,
            letterData = ManueltBrevData(innhold.payload?.elements ?: emptyList()),
            felles = BrevbakerHelpers.mapFelles(behandling, avsender, brev.mottaker),
            LanguageCode.spraakToLanguageCode(Spraak.NB)
        )
        val pdf = genererPdf(brev.id, brevRequest)

        hvisInnholdKanFerdigstilles(brev.id, behandling, bruker) {
            db.ferdigstillManueltBrevInnhold(brev.id, pdf)
        }

        return pdf
    }

    private fun hvisInnholdKanFerdigstilles(brevID: BrevID, behandling: Behandling, bruker: Bruker, block: () -> Unit) {
        if (behandling.vedtak.status != VedtakStatus.FATTET_VEDTAK) {
            logger.info("Vedtak status er ${behandling.vedtak.status}. Avventer ferdigstilling av brev (id=$brevID)")
            return
        }

        logger.info(
            "Vedtak fattet på behandling (id=${behandling.behandlingId}). " +
                "Sjekker om brev (id=$brevID) kan ferdigstilles"
        )

        if (behandling.vedtak.saksbehandlerIdent != bruker.ident()) {
            logger.info("Ferdigstiller brev med id=$brevID")
            block()
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
        db.opprettEllerOppdaterPayload(id, payload)
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

    private suspend fun genererPdf(brevID: BrevID, brevRequest: BrevbakerRequest): ByteArray {
        val brevbakerResponse = brevbaker.genererPdf(brevRequest)
        val brev = Base64.getDecoder().decode(brevbakerResponse.base64pdf)

        logger.info("Generert brev (id=$brevID) med størrelse: ${brev.size}")

        return brev
    }

    private fun getJsonFile(url: String) = javaClass.getResource(url)!!.readText()

    private fun hentInitiellPayload(sakType: SakType, vedtakType: VedtakType): Slate {
        return when (sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> getJsonFile("/maler/oms-nasjonal-innvilget.json")
                    VedtakType.OPPHOER,
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING -> getJsonFile("/maler/tom-brevmal.json")
                }
            }

            SakType.BARNEPENSJON -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE,
                    VedtakType.OPPHOER,
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING -> getJsonFile("/maler/tom-brevmal.json")
                }
            }
        }.let { deserialize(it) }
    }

    private fun hentProsessType(sakType: SakType, vedtakType: VedtakType): BrevProsessType {
        return when (sakType) {
            SakType.OMSTILLINGSSTOENAD -> BrevProsessType.MANUELL
            SakType.BARNEPENSJON -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> BrevProsessType.AUTOMATISK
                    VedtakType.OPPHOER,
                    VedtakType.AVSLAG,
                    VedtakType.ENDRING -> BrevProsessType.MANUELL
                }
            }
        }
    }
}