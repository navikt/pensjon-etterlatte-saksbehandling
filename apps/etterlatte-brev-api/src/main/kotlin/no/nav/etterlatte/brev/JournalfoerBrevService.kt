package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.AvsenderMottaker
import no.nav.etterlatte.brev.dokarkiv.Bruker
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.DokumentVariant
import no.nav.etterlatte.brev.dokarkiv.JournalPostType
import no.nav.etterlatte.brev.dokarkiv.JournalpostDokument
import no.nav.etterlatte.brev.dokarkiv.JournalpostKoder
import no.nav.etterlatte.brev.dokarkiv.JournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.JournalfoerVedtaksbrevResponseOgBrevid
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevService
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import org.slf4j.LoggerFactory
import java.util.Base64

class JournalfoerBrevService(
    private val db: BrevRepository,
    private val behandlingService: BehandlingService,
    private val dokarkivService: DokarkivService,
    private val service: VedtaksbrevService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun journalfoer(
        id: BrevID,
        bruker: BrukerTokenInfo,
    ): List<OpprettJournalpostResponse> {
        val brev = db.hentBrev(id)
        if (brev.brevtype == Brevtype.NOTAT) {
            throw IllegalArgumentException("Kan ikke journalføre et notat som et brev (id = $id).")
        }
        val sak = behandlingService.hentSak(brev.sakId, bruker)

        return journalfoer(brev, sak, bruker)
    }

    suspend fun journalfoerVedtaksbrev(
        vedtak: VedtakTilJournalfoering,
        bruker: Systembruker,
    ): JournalfoerVedtaksbrevResponseOgBrevid? {
        logger.info("Nytt vedtak med id ${vedtak.vedtakId} er attestert. Ferdigstiller vedtaksbrev.")
        val behandlingId = vedtak.behandlingId

        val brev =
            service.hentVedtaksbrev(behandlingId)
                ?: throw NoSuchElementException("Ingen vedtaksbrev funnet på behandlingId=$behandlingId")

        if (brev.status in listOf(Status.JOURNALFOERT, Status.DISTRIBUERT, Status.SLETTET)) {
            logger.warn("Vedtaksbrev (id=${brev.id}) er allerede ${brev.status}.")
            return null
        }

        // Spesialhandtering for gjenoppretting.
        if (vedtak.saksbehandler == "EY" && brev.status == Status.OPPRETTET) {
            logger.warn(
                "Vedtaksbrev fra migrering som er i feil tilstand: ${brev.id}. Avbryter denne, må følges opp manuelt av utviklerne.",
            )
            return null
        }

        val sak = behandlingService.hentSak(brev.sakId, bruker)

        return journalfoer(brev, sak, bruker)
            .also { logger.info("Vedtaksbrev for vedtak med id ${vedtak.vedtakId} er journalfoert OK") }
            .let { JournalfoerVedtaksbrevResponseOgBrevid(brev.id, it) }
    }

    private suspend fun journalfoer(
        brev: Brev,
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): List<OpprettJournalpostResponse> {
        logger.info("Journalfører brev ${brev.id} på ${brev.mottakere.size} mottaker(e)")

        if (brev.status != Status.FERDIGSTILT) {
            throw FeilStatusForJournalfoering(brev.id, brev.status)
        }

        return brev.mottakere
            .map { mottaker -> journalfoerPaaMottaker(brev.id, brev.soekerFnr, mottaker, sak, bruker) }
            .also { db.settBrevJournalfoert(brev.id, it, bruker) }
    }

    private suspend fun journalfoerPaaMottaker(
        brevId: BrevID,
        soekerFnr: String,
        mottaker: Mottaker,
        sak: Sak,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse {
        val request = mapTilJournalpostRequest(brevId, soekerFnr, mottaker, sak)

        val response = dokarkivService.journalfoer(request, bruker)

        if (!response.journalpostferdigstilt) {
            logger.warn("Kunne ikke ferdigstille journalpost. Forsøker på nytt...")
            dokarkivService.ferdigstillJournalpost(response.journalpostId, sak.enhet, bruker)
        }

        db.lagreJournalpostId(mottaker.id, response)
        logger.info("Brev $brevId journalført på mottaker (id=${mottaker.id})")

        return response
    }

    private fun mapTilJournalpostRequest(
        brevId: BrevID,
        soekerFnr: String,
        mottaker: Mottaker,
        sak: Sak,
    ): JournalpostRequest {
        val innhold = requireNotNull(db.hentBrevInnhold(brevId))
        val pdf = requireNotNull(db.hentPdf(brevId))

        val tittel =
            when (mottaker.type) {
                MottakerType.HOVED -> innhold.tittel
                MottakerType.KOPI -> "${innhold.tittel} (KOPI)"
            }

        val avsenderMottaker =
            with(mottaker) {
                AvsenderMottaker(
                    id = foedselsnummer?.value ?: orgnummer,
                    idType =
                        when {
                            foedselsnummer != null -> "FNR"
                            orgnummer != null -> "ORGNR"
                            else -> "UKJENT"
                        },
                    navn = navn,
                )
            }

        return JournalpostRequest(
            tittel = tittel,
            journalposttype = JournalPostType.UTGAAENDE,
            avsenderMottaker = avsenderMottaker,
            bruker = Bruker(soekerFnr),
            eksternReferanseId = "${sak.id}.$brevId.${mottaker.id}",
            sak = JournalpostSak(Sakstype.FAGSAK, sak.id.toString(), sak.sakType.tema, Fagsaksystem.EY.navn),
            dokumenter =
                listOf(
                    JournalpostDokument(
                        innhold.tittel,
                        brevkode = JournalpostKoder.BREV_KODE,
                        dokumentvarianter = listOf(DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(pdf.bytes))),
                    ),
                ),
            tema = sak.sakType.tema,
            kanal = "S",
            journalfoerendeEnhet = sak.enhet,
        )
    }
}

class FeilStatusForJournalfoering(
    brevID: BrevID,
    status: Status,
) : UgyldigForespoerselException(
        code = "FEIL_STATUS_FOR_JOURNALFOERING",
        detail = "Kan ikke journalføre brev $brevID med status ${status.name.lowercase()}",
        meta =
            mapOf(
                "brevId" to brevID,
                "status" to status,
            ),
    )
