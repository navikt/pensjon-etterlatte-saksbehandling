package no.nav.etterlatte.brev.vedtaksbrev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.PDFGenerator
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataMapperFerdigstillingVedtak
import no.nav.etterlatte.brev.model.BrevDataMapperRedigerbartUtfallVedtak
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevKodeMapperVedtak
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.varselbrev.BrevDataMapperRedigerbartUtfallVarsel
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val brevKodeMappingVedtak: BrevKodeMapperVedtak,
    private val brevoppretter: Brevoppretter,
    private val pdfGenerator: PDFGenerator,
    private val brevDataMapperRedigerbartUtfallVedtak: BrevDataMapperRedigerbartUtfallVedtak,
    private val brevDataMapperFerdigstilling: BrevDataMapperFerdigstillingVedtak,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogger = sikkerlogger()

    fun hentBrev(id: BrevID): Brev {
        logger.info("Henter brev (id=$id)")

        return db.hentBrev(id)
    }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull()
    }

    suspend fun opprettVedtaksbrev(
        sakId: SakId,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        require(db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            val kanRedigeres = behandlingService.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        return brevoppretter
            .opprettBrev(
                sakId = sakId,
                behandlingId = behandlingId,
                bruker = brukerTokenInfo,
                brevKodeMapping = { brevKodeMappingVedtak.brevKode(it) },
                brevtype = Brevtype.VEDTAK,
                validerMottaker = false,
                brevDataMapping = { brevDataMapperRedigerbartUtfallVedtak.brevData(it) },
            ).first
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf =
        pdfGenerator.genererPdf(
            id = id,
            bruker = bruker,
            avsenderRequest = { brukerToken, vedtak, enhet -> opprettAvsenderRequest(brukerToken, vedtak, enhet) },
            brevKodeMapping = { brevKodeMappingVedtak.brevKode(it) },
            brevDataMapping = { brevDataMapperFerdigstilling.brevDataFerdigstilling(it) },
        ) { vedtakStatus, saksbehandler, brev, pdf ->
            brev.brevkoder?.let { db.oppdaterBrevkoder(brev.id, it) }
            lagrePdfHvisVedtakFattet(
                brev.id,
                pdf,
                bruker,
                vedtakStatus!!,
                saksbehandler!!,
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
        } else if (brev.mottaker.erGyldig().isNotEmpty()) {
            sikkerlogger.error("Ugyldig mottaker: ${brev.mottaker.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id, brev.sakId, brev.mottaker.erGyldig())
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
                throw BrevManglerPDF(brev.id)
            } else {
                db.settBrevFerdigstilt(brev.id)
            }
        } else {
            throw SaksbehandlerOgAttestantSammePerson(saksbehandlerIdent, brukerTokenInfo.ident())
        }
    }

    suspend fun hentNyttInnhold(
        sakId: SakId,
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevtype: Brevtype,
    ): BrevService.BrevPayload =
        brevoppretter.hentNyttInnhold(sakId, brevId, behandlingId, brukerTokenInfo, {
            when (brevtype) {
                Brevtype.VARSEL ->
                    if (it.sakType === SakType.BARNEPENSJON) {
                        Brevkoder.BP_VARSEL
                    } else {
                        Brevkoder.OMS_VARSEL
                    }

                Brevtype.VEDTAK -> brevKodeMappingVedtak.brevKode(it)
                else -> throw UgyldigForespoerselException(
                    "FEIL_BREVKODE_NYTT_INNHOLD",
                    "Prøvde å hente brevkode for nytt innhold for brevtype $brevtype, men per nå støtter vi bare vedtak og varsel.",
                )
            }
        }) {
            if (brevtype == Brevtype.VARSEL) {
                BrevDataMapperRedigerbartUtfallVarsel.hentBrevDataRedigerbar(
                    it.sakType,
                    brukerTokenInfo,
                    it.utlandstilknytningType,
                    it.revurderingsaarsak,
                )
            } else {
                brevDataMapperRedigerbartUtfallVedtak.brevData(it)
            }
        }

    private fun lagrePdfHvisVedtakFattet(
        brevId: BrevID,
        pdf: Pdf,
        brukerTokenInfo: BrukerTokenInfo,
        vedtakStatus: VedtakStatus,
        saksbehandlerVedtak: String,
    ) {
        if (vedtakStatus != VedtakStatus.FATTET_VEDTAK) {
            logger.info("Vedtak status er $vedtakStatus. Avventer ferdigstilling av brev (id=$brevId)")
            return
        }

        if (!brukerTokenInfo.erSammePerson(saksbehandlerVedtak)) {
            logger.info("Lagrer PDF for brev med id=$brevId")

            db.lagrePdf(brevId, pdf)
        } else {
            logger.warn(
                "Kan ikke ferdigstille/låse brev når saksbehandler ($saksbehandlerVedtak)" +
                    " og attestant (${brukerTokenInfo.ident()}) er samme person.",
            )
        }
    }

    suspend fun settVedtaksbrevTilSlettet(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev = db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() ?: return

        if (!brev.kanEndres()) {
            throw VedtaksbrevKanIkkeSlettes(brev.id, "Brevet har status (${brev.status})")
        }
        val behandlingKanEndres = behandlingService.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
        if (!behandlingKanEndres) {
            throw VedtaksbrevKanIkkeSlettes(brev.id, "Behandlingen til vedtaksbrevet kan ikke endres")
        }
        db.settBrevSlettet(brev.id, brukerTokenInfo)
    }

    fun fjernFerdigstiltStatusUnderkjentVedtak(
        id: BrevID,
        vedtak: JsonNode,
    ): Boolean {
        logger.info("Fjerner status FERDIGSTILT på vedtaksbrev (id=$id)")

        return db.fjernFerdigstiltStatusUnderkjentVedtak(id, vedtak)
    }
}

class UgyldigStatusKanIkkeFerdigstilles(
    id: BrevID,
    status: Status,
) : UgyldigForespoerselException(
        code = "UGYLDIG_STATUS_BREV",
        detail = "Brevet kan ikke ferdigstilles når status er ${status.name.lowercase()}",
        meta =
            mapOf(
                "id" to id,
                "status" to status,
            ),
    )

class VedtaksbrevKanIkkeSlettes(
    brevId: BrevID,
    detalj: String,
) : IkkeTillattException(
        code = "VEDTAKSBREV_KAN_IKKE_SLETTES",
        detail = "Vedtaksbrevet kan ikke slettes: $detalj",
        meta =
            mapOf(
                "id" to brevId,
            ),
    )

class UgyldigMottakerKanIkkeFerdigstilles(
    id: BrevID?,
    sakId: SakId,
    feil: List<String>,
) : UgyldigForespoerselException(
        code = "UGYLDIG_MOTTAKER_BREV",
        detail = "Brevet kan ikke ferdigstilles med ugyldig mottaker og/eller adresse. BrevID: $id, sakId: $sakId. $feil",
        meta = id?.let { mapOf("id" to it) },
    )

class BrevManglerPDF(
    id: BrevID,
) : UgyldigForespoerselException(
        code = "PDF_MANGLER",
        detail = "Kan ikke ferdigstille brev siden PDF ikke er ferdig generert og lagret.",
        meta = mapOf("id" to id),
    )

class SaksbehandlerOgAttestantSammePerson(
    saksbehandler: String,
    attestant: String,
) : UgyldigForespoerselException(
        code = "SAKSBEHANDLER_OG_ATTESTANT_SAMME_PERSON",
        detail = "Kan ikke ferdigstille vedtaksbrevet når saksbehandler ($saksbehandler) og attestant ($attestant) er samme person.",
        meta = mapOf("saksbehandler" to saksbehandler, "attestant" to attestant),
    )

class KanIkkeOppretteVedtaksbrev(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "KAN_IKKE_ENDRE_VEDTAKSBREV",
        detail = "Statusen til behandlingen tillater ikke at det opprettes vedtaksbrev",
        meta = mapOf("behandlingId" to behandlingId),
    )