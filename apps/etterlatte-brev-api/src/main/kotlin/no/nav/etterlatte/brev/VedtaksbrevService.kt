package no.nav.etterlatte.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType.AUTOMATISK
import no.nav.etterlatte.brev.model.BrevProsessType.MANUELL
import no.nav.etterlatte.brev.model.BrevProsessType.REDIGERBAR
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverk
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverkFerdig
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val brevDataMapper: BrevDataMapper,
    private val migreringBrevDataService: MigreringBrevDataService,
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

        return db.hentBrevForBehandling(behandlingId)
    }

    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        // TODO EY-3232 - Fjerne migreringstilpasning
    ): Brev {
        return brevoppretter.opprettVedtaksbrev(
            sakId = sakId,
            behandlingId = behandlingId,
            brukerTokenInfo = brukerTokenInfo,
            automatiskMigreringRequest = automatiskMigreringRequest,
        )
    }

    suspend fun genererPdf(
        id: BrevID,
        bruker: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): Pdf =
        pdfGenerator.genererPdf(
            id = id,
            bruker = bruker,
            automatiskMigreringRequest = automatiskMigreringRequest,
            avsenderRequest = { it.avsenderRequest() },
            brevKode = { generellBrevData, brev, migreringBrevRequest ->
                brevDataMapper.brevKode(
                    generellBrevData,
                    brev.prosessType,
                    migreringBrevRequest?.erOmregningGjenny ?: false,
                )
            },
            brevData = { req ->
                brevData(req.generellBrevData, req.automatiskMigreringRequest, req.brev, req.bruker, req.brevkodePar)
            },
        ) { generellBrevData, brev, pdf ->
            lagrePdfHvisVedtakFattet(
                brev.id,
                generellBrevData.forenkletVedtak!!,
                pdf,
                bruker,
                automatiskMigreringRequest != null,
            )
        }

    private suspend fun VedtaksbrevService.brevData(
        generellBrevData: GenerellBrevData,
        automatiskMigreringRequest: MigreringBrevRequest?,
        brev: Brev,
        brukerTokenInfo: BrukerTokenInfo,
        brevkodePar: BrevDataMapper.BrevkodePar,
    ) = when (
        generellBrevData.systemkilde == Vedtaksloesning.PESYS ||
            automatiskMigreringRequest?.erOmregningGjenny ?: false
    ) {
        false -> opprettBrevData(brev, generellBrevData, brukerTokenInfo, brevkodePar)
        true ->
            OmregnetBPNyttRegelverkFerdig(
                innhold = InnholdMedVedlegg({ hentLagretInnhold(brev) }, { hentLagretInnholdVedlegg(brev) }).innhold(),
                data = (
                    migreringBrevDataService.opprettMigreringBrevdata(
                        generellBrevData,
                        automatiskMigreringRequest,
                        brukerTokenInfo,
                    ) as OmregnetBPNyttRegelverk
                ),
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
            throw IllegalStateException("Brev med id=${brev.id} kan ikke ferdigstilles, siden det har status ${brev.status}")
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
    ): BrevService.BrevPayload = brevoppretter.hentNyttInnhold(sakId, brevId, behandlingId, brukerTokenInfo)

    private suspend fun opprettBrevData(
        brev: Brev,
        generellBrevData: GenerellBrevData,
        brukerTokenInfo: BrukerTokenInfo,
        brevkode: BrevDataMapper.BrevkodePar,
    ): BrevData =
        when (brev.prosessType) {
            REDIGERBAR ->
                brevDataMapper.brevDataFerdigstilling(
                    generellBrevData,
                    brukerTokenInfo,
                    InnholdMedVedlegg({ hentLagretInnhold(brev) }, { hentLagretInnholdVedlegg(brev) }),
                    brevkode,
                )

            AUTOMATISK -> brevDataMapper.brevData(generellBrevData, brukerTokenInfo)
            MANUELL -> ManueltBrevData(hentLagretInnhold(brev))
        }

    private fun hentLagretInnhold(brev: Brev) =
        requireNotNull(
            db.hentBrevPayload(brev.id),
        ) { "Fant ikke payload for brev ${brev.id}" }.elements

    private fun hentLagretInnholdVedlegg(brev: Brev) =
        requireNotNull(db.hentBrevPayloadVedlegg(brev.id)) {
            "Fant ikke payloadvedlegg for brev ${brev.id}"
        }

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
    val erOmregningGjenny: Boolean = false,
)

fun Vergemaal.toMottaker(): Mottaker {
    if (mottaker.adresse != null) {
        return Mottaker(
            navn = if (mottaker.navn.isNullOrBlank()) "N/A" else mottaker.navn!!,
            foedselsnummer = mottaker.foedselsnummer?.let { Foedselsnummer(it.value) },
            orgnummer = null,
            adresse =
                with(mottaker.adresse!!) {
                    Adresse(
                        adresseType,
                        adresselinje1,
                        adresselinje2,
                        adresselinje3,
                        postnummer,
                        poststed,
                        landkode,
                        land,
                    )
                },
        )
    }

    return Mottaker.tom(Folkeregisteridentifikator.of(mottaker.foedselsnummer!!.value))
        .copy(navn = mottaker.navn ?: "N/A")
}
