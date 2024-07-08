package no.nav.etterlatte.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
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
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val brevKodeMapperVedtak: BrevKodeMapperVedtak,
    private val brevoppretter: Brevoppretter,
    private val pdfGenerator: PDFGenerator,
    private val brevDataMapperRedigerbartUtfallVedtak: BrevDataMapperRedigerbartUtfallVedtak,
    private val brevDataMapperFerdigstilling: BrevDataMapperFerdigstillingVedtak,
    private val behandlingService: BehandlingService,
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
    ): Brev =
        brevoppretter.opprettVedtaksbrev(
            sakId = sakId,
            behandlingId = behandlingId,
            brukerTokenInfo = brukerTokenInfo,
            brevKode = { brevKodeMapperVedtak.brevKode(it).redigering },
        ) { brevDataMapperRedigerbartUtfallVedtak.brevData(it) }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf =
        pdfGenerator.genererPdf(
            id = id,
            bruker = bruker,
            avsenderRequest = { brukerToken, generellBrevData -> generellBrevData.avsenderRequest(brukerToken) },
            brevKode = { brevKodeMapperVedtak.brevKode(it) },
            brevData = { brevDataMapperFerdigstilling.brevDataFerdigstilling(it) },
        ) { generellBrevData, brev, pdf ->
            lagrePdfHvisVedtakFattet(
                brev.id,
                generellBrevData.forenkletVedtak!!,
                pdf,
                bruker,
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
            throw SaksbehandlerOgAttestantSammePerson(saksbehandlerIdent, brukerTokenInfo.ident())
        }
    }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevtype: Brevtype,
    ): BrevService.BrevPayload =
        brevoppretter.hentNyttInnhold(sakId, brevId, behandlingId, brukerTokenInfo, {
            when (brevtype) {
                Brevtype.VARSEL ->
                    if (it.sakType === SakType.BARNEPENSJON) {
                        Brevkoder.BP_VARSEL.redigering
                    } else {
                        Brevkoder.OMS_VARSEL.redigering
                    }

                Brevtype.VEDTAK -> brevKodeMapperVedtak.brevKode(it).redigering
                else -> throw UgyldigForespoerselException(
                    "FEIL_BREVKODE_NYTT_INNHOLD",
                    "Prøvde å hente brevkode for nytt innhold for brevtype $brevtype, men per nå støtter vi bare vedtak og varsel.",
                )
            }
        }) {
            if (brevtype == Brevtype.VARSEL) {
                BrevDataMapperRedigerbartUtfallVarsel.hentBrevDataRedigerbar(
                    it.generellBrevData.sak.sakType,
                    brukerTokenInfo,
                    it.generellBrevData.utlandstilknytning,
                    it.generellBrevData.revurderingsaarsak,
                )
            } else {
                brevDataMapperRedigerbartUtfallVedtak.brevData(it)
            }
        }

    private fun lagrePdfHvisVedtakFattet(
        brevId: BrevID,
        vedtak: ForenkletVedtak,
        pdf: Pdf,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (vedtak.status != VedtakStatus.FATTET_VEDTAK) {
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
    id: BrevID,
) : UgyldigForespoerselException(
        code = "UGYLDIG_MOTTAKER_BREV",
        detail = "Brevet kan ikke ferdigstilles med ugyldig mottaker og/eller adresse",
        meta =
            mapOf(
                "id" to id,
            ),
    )

class SaksbehandlerOgAttestantSammePerson(
    saksbehandler: String,
    attestant: String,
) : UgyldigForespoerselException(
        code = "SAKSBEHANDLER_OG_ATTESTANT_SAMME_PERSON",
        detail = "Kan ikke ferdigstille vedtaksbrevet når saksbehandler ($saksbehandler) og attestant ($attestant) er samme person.",
        meta = mapOf("saksbehandler" to saksbehandler, "attestant" to attestant),
    )
