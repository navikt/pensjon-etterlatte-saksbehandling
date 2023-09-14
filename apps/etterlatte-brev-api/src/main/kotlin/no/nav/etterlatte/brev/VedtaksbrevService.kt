package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.SakOgBehandlingService
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivServiceImpl
import no.nav.etterlatte.brev.journalpost.JournalpostResponse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevProsessType.AUTOMATISK
import no.nav.etterlatte.brev.model.BrevProsessType.MANUELL
import no.nav.etterlatte.brev.model.BrevProsessType.REDIGERBAR
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.rivers.VedtakTilJournalfoering
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class VedtaksbrevService(
    private val db: BrevRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val adresseService: AdresseService,
    private val dokarkivService: DokarkivServiceImpl,
    private val brevService: BrevService,
    private val brevbaker: BrevbakerService,
    private val brevDataMapper: BrevDataMapper,
    private val brevProsessTypeFactory: BrevProsessTypeFactory
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
        brukerTokenInfo: BrukerTokenInfo
    ): Brev {
        require(hentVedtaksbrev(behandlingId) == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, brukerTokenInfo)

        val mottaker = adresseService.hentMottakerAdresse(behandling.persongalleri.innsender.fnr.value)

        val prosessType = brevProsessTypeFactory.fra(behandling)

        val nyttBrev = OpprettNyttBrev(
            sakId = sakId,
            behandlingId = behandling.behandlingId,
            prosessType = prosessType,
            soekerFnr = behandling.persongalleri.soeker.fnr.value,
            mottaker = mottaker,
            innhold = opprettInnhold(behandling, prosessType),
            innholdVedlegg = opprettInnholdVedlegg(behandling, prosessType)
        )

        return db.opprettBrev(nyttBrev)
    }

    suspend fun genererPdf(
        id: BrevID,
        brukerTokenInfo: BrukerTokenInfo
    ): Pdf {
        val brev = hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id))
        }

        val behandling = sakOgBehandlingService.hentBehandling(brev.sakId, brev.behandlingId!!, brukerTokenInfo)
        val avsender = adresseService.hentAvsender(behandling.vedtak)

        val kode = brevDataMapper.brevKode(behandling, brev.prosessType)
        val brevData = opprettBrevData(brev, behandling, kode)
        val brevRequest = BrevbakerRequest.fra(kode.ferdigstilling, brevData, behandling, avsender)

        return brevbaker.genererPdf(brev.id, brevRequest)
            .also { pdf -> ferdigstillHvisVedtakFattet(brev, behandling, pdf, brukerTokenInfo) }
    }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): BrevService.BrevPayload {
        val behandling = sakOgBehandlingService.hentBehandling(sakId, behandlingId, brukerTokenInfo)
        val prosessType = brevProsessTypeFactory.fra(behandling)
        val innhold = opprettInnhold(behandling, prosessType)
        val innholdVedlegg = opprettInnholdVedlegg(behandling, prosessType)

        if (innhold.payload != null) {
            db.oppdaterPayload(brevId, innhold.payload)
        }

        if (innholdVedlegg != null) {
            db.oppdaterPayloadVedlegg(brevId, innholdVedlegg)
        }

        return brevService.hentBrevPayload(brevId)
    }

    private fun opprettBrevData(brev: Brev, behandling: Behandling, brevkode: BrevDataMapper.BrevkodePar): BrevData =
        when (brev.prosessType) {
            REDIGERBAR -> brevDataMapper.brevDataFerdigstilling(behandling, {
                hentLagretInnhold(brev)
            }, { hentLagretInnholdVedlegg(brev) }, brevkode)
            AUTOMATISK -> brevDataMapper.brevData(behandling)
            MANUELL -> ManueltBrevData(hentLagretInnhold(brev))
        }

    private fun hentLagretInnhold(brev: Brev) = requireNotNull(db.hentBrevPayload(brev.id)).elements

    private fun hentLagretInnholdVedlegg(brev: Brev) = requireNotNull(db.hentBrevPayloadVedlegg(brev.id))

    private suspend fun opprettInnhold(behandling: Behandling, prosessType: BrevProsessType): BrevInnhold {
        val tittel = "Vedtak om ${behandling.vedtak.type.name.lowercase()}"

        val payload = when (prosessType) {
            REDIGERBAR -> brevbaker.hentRedigerbarTekstFraBrevbakeren(behandling)
            AUTOMATISK -> null
            MANUELL -> SlateHelper.hentInitiellPayload(behandling)
        }

        return BrevInnhold(tittel, behandling.spraak, payload)
    }

    private fun opprettInnholdVedlegg(behandling: Behandling, prosessType: BrevProsessType): List<BrevInnholdVedlegg>? =
        when (prosessType) {
            REDIGERBAR -> SlateHelper.hentInitiellPayloadVedlegg(behandling)
            AUTOMATISK -> null
            MANUELL -> null
        }

    private fun ferdigstillHvisVedtakFattet(
        brev: Brev,
        behandling: Behandling,
        pdf: Pdf,
        brukerTokenInfo: BrukerTokenInfo
    ) {
        if (behandling.vedtak.status != VedtakStatus.FATTET_VEDTAK) {
            logger.info("Vedtak status er ${behandling.vedtak.status}. Avventer ferdigstilling av brev (id=${brev.id})")
            return
        }

        if (behandling.vedtak.saksbehandlerIdent != brukerTokenInfo.ident()) {
            logger.info("Ferdigstiller brev med id=${brev.id}")

            db.lagrePdfOgFerdigstillBrev(brev.id, pdf)
        } else {
            logger.warn(
                "Kan ikke ferdigstille/låse brev når saksbehandler (${behandling.vedtak.saksbehandlerIdent})" +
                    " og attestant (${brukerTokenInfo.ident()}) er samme person."
            )
        }
    }

    fun journalfoerVedtaksbrev(vedtaksbrev: Brev, vedtak: VedtakTilJournalfoering): JournalpostResponse {
        if (vedtaksbrev.status != Status.FERDIGSTILT) {
            throw IllegalArgumentException("Ugyldig status ${vedtaksbrev.status} på vedtaksbrev (id=${vedtaksbrev.id})")
        }

        return dokarkivService.journalfoer(vedtaksbrev.id, vedtak)
            .also { journalfoeringResponse ->
                db.settBrevJournalfoert(vedtaksbrev.id, journalfoeringResponse)
                logger.info("Brev med id=${vedtaksbrev.id} markert som journalført")
            }
    }

    fun slettVedtaksbrev(id: BrevID): Boolean {
        logger.info("Sletter vedtaksbrev (id=$id)")

        return db.slett(id)
    }
}