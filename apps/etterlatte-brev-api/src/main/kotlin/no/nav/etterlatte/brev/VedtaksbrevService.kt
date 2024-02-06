package no.nav.etterlatte.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val brevKodeMapperVedtak: BrevKodeMapperVedtak,
    private val brevoppretter: Brevoppretter,
    private val pdfGenerator: PDFGenerator,
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

    fun hentBrev(id: BrevID): Brev {
        logger.info("Henter brev (id=$id)")

        return db.hentBrev(id)
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull()
    }

    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        // TODO EY-3232 - Fjerne migreringstilpasning
    ): Brev =
        brevoppretter.opprettVedtaksbrev(
            sakId = sakId,
            behandlingId = behandlingId,
            brukerTokenInfo = brukerTokenInfo,
            automatiskMigreringRequest = automatiskMigreringRequest,
            brevKode = { brevKodeMapperVedtak.brevKode(it).redigering },
        )

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): Pdf =
        pdfGenerator.genererPdf(
            id = id,
            bruker = bruker,
            automatiskMigreringRequest = automatiskMigreringRequest,
            avsenderRequest = { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
            brevKode = { brevKodeMapperVedtak.brevKode(it) },
        ) { generellBrevData, brev, pdf ->
            lagrePdfHvisVedtakFattet(
                brev.id,
                generellBrevData.forenkletVedtak!!,
                pdf,
                bruker,
                automatiskMigreringRequest != null,
            )
        }

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        migrering: Boolean = false,
    ) {
        val brev =
            requireNotNull(hentVedtaksbrev(behandlingId)) {
                "Fant ingen brev for behandling (id=$behandlingId)"
            }

        if (brev.status == Status.FERDIGSTILT) {
            logger.warn("Brev (id=${brev.id}) er allerede ferdigstilt. Avbryter ferdigstilling...")
            return
        } else if (!brev.kanEndres()) {
            throw UgyldigStatusKanIkkeFerdigstilles(brev.id, brev.status)
        } else if (!brev.mottaker.erGyldig()) {
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id)
        }

        val (saksbehandlerIdent, vedtakStatus) =
            vedtaksvurderingService.hentVedtakSaksbehandlerOgStatus(
                brev.behandlingId!!,
                brukerTokenInfo,
            )

        if (vedtakStatus != VedtakStatus.FATTET_VEDTAK && !migrering) {
            throw IllegalStateException(
                "Vedtak status er $vedtakStatus. Avventer ferdigstilling av brev (id=${brev.id})",
            )
        }

        if (!brukerTokenInfo.erSammePerson(saksbehandlerIdent)) {
            logger.info("Ferdigstiller brev med id=${brev.id}")

            if (db.hentPdf(brev.id) == null) {
                throw IllegalStateException("Kan ikke ferdigstille brev (id=${brev.id}) siden PDF er null!")
            } else {
                db.settBrevFerdigstilt(brev.id)
            }
        } else {
            throw IllegalStateException(
                "Kan ikke ferdigstille/låse brev når saksbehandler ($saksbehandlerIdent)" +
                    " og attestant (${brukerTokenInfo.ident()}) er samme person.",
            )
        }
    }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevService.BrevPayload =
        brevoppretter.hentNyttInnhold(sakId, brevId, behandlingId, brukerTokenInfo, {
            brevKodeMapperVedtak.brevKode(it).redigering
        })

    private fun lagrePdfHvisVedtakFattet(
        brevId: BrevID,
        vedtak: ForenkletVedtak,
        pdf: Pdf,
        brukerTokenInfo: BrukerTokenInfo,
        migrering: Boolean = false,
    ) {
        if (vedtak.status != VedtakStatus.FATTET_VEDTAK && !migrering) {
            logger.info("Vedtak status er ${vedtak.status}. Avventer ferdigstilling av brev (id=$brevId)")
            return
        }

        if (!brukerTokenInfo.erSammePerson(vedtak.saksbehandlerIdent)) {
            logger.info("Lagrer PDF for brev med id=$brevId")

            db.lagrePdf(brevId, pdf)
        } else {
            logger.warn(
                "Kan ikke ferdigstille/låse brev når saksbehandler (${vedtak.saksbehandlerIdent})" +
                    " og attestant (${brukerTokenInfo.ident()}) er samme person.",
            )
        }
    }

    fun fjernFerdigstiltStatusUnderkjentVedtak(
        id: BrevID,
        vedtak: JsonNode,
    ): Boolean {
        logger.info("Fjerner status FERDIGSTILT på vedtaksbrev (id=$id)")

        return db.fjernFerdigstiltStatusUnderkjentVedtak(id, vedtak)
    }
}

// TODO EY-3232 - Fjerne
data class MigreringBrevRequest(
    val brutto: Int,
    val yrkesskade: Boolean,
    val utlandstilknytningType: UtlandstilknytningType?,
)

class UgyldigStatusKanIkkeFerdigstilles(id: BrevID, status: Status) : UgyldigForespoerselException(
    code = "UGYLDIG_STATUS_BREV",
    detail = "Brevet kan ikke ferdigstilles når status er ${status.name.lowercase()}",
    meta =
        mapOf(
            "id" to id,
            "status" to status,
        ),
)

class UgyldigMottakerKanIkkeFerdigstilles(id: BrevID) : UgyldigForespoerselException(
    code = "UGYLDIG_MOTTAKER_BREV",
    detail = "Brevet har ugyldig mottaker og kan ikke ferdigstilles",
    meta =
        mapOf(
            "id" to id,
        ),
)
