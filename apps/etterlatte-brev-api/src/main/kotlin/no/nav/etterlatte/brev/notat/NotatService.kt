package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.brevbaker.formaterNavn
import no.nav.etterlatte.brev.dokarkiv.Bruker
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.DokumentVariant
import no.nav.etterlatte.brev.dokarkiv.JournalpostDokument
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.dokarkiv.OpprettNotatJournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.notat.Notat
import no.nav.etterlatte.brev.notat.NotatMal
import no.nav.etterlatte.brev.notat.NotatRepository
import no.nav.etterlatte.brev.notat.NyttNotat
import no.nav.etterlatte.brev.notat.PdfGenRequest
import no.nav.etterlatte.brev.notat.PdfGeneratorKlient
import no.nav.etterlatte.brev.notat.StrukturertNotat
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.slf4j.LoggerFactory
import java.util.Base64

@Deprecated("Denne servicen skal ikke lenger brukes", ReplaceWith("NyNotatService.kt"))
class NotatService(
    private val notatRepository: NotatRepository,
    private val pdfGeneratorKlient: PdfGeneratorKlient,
    private val dokarkivService: DokarkivService,
    private val grunnlagService: GrunnlagService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun journalfoerNotatISak(
        blankett: StrukturertNotat.KlageBlankett,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse {
        val sak = blankett.klage.sak

        val notat =
            notatRepository.hentForReferanse(blankett.klage.id.toString()) ?: run {
                logger.info("Oppretter nytt notat - ${NotatMal.KLAGE_OVERSENDELSE_BLANKETT.navn}")

                val notatId =
                    notatRepository.opprett(
                        NyttNotat(
                            sakId = sak.id,
                            referanse = blankett.klage.id.toString(),
                            tittel = "Klage oversendelsesblankett",
                            mal = NotatMal.KLAGE_OVERSENDELSE_BLANKETT,
                            payload = Slate(emptyList()), // Burde vi endre payload til JsonNode ?
                        ),
                        bruker,
                    )

                notatRepository.hent(notatId)
            }

        try {
            val notatPdf = genererPdf(blankett, bruker)

            notatRepository.lagreInnhold(notat.id, notatPdf)

            return journalfoerInterntNotat(notat, sak, notatPdf)
                .also { response ->
                    notatRepository.settJournalfoert(notat.id, response, bruker)
                }
        } catch (e: Exception) {
            try {
                notatRepository.slett(notat.id)
            } catch (inner: Exception) {
                logger.error(
                    "Fikk en feil i opprydding etter feil med generering / journalføring av notat i sak" +
                        "med id=${sak.id}. Kunne ikke sette notat med id=${notat.id} til slettet, så det vil " +
                        "henge igjen. Se annen feilmelding for hva som gikk galt før opprydding",
                    inner,
                )
            }
            logger.error("Kunne ikke generere og journalføre notat i sak=${sak.id} på grunn av feil: ", e)
            throw e
        }
    }

    suspend fun genererPdf(
        blankett: StrukturertNotat.KlageBlankett,
        bruker: BrukerTokenInfo,
    ): ByteArray {
        val grunnlag = grunnlagService.hentGrunnlagForSak(blankett.klage.sak.id, bruker)
        val soeker = grunnlag.mapSoeker(null)

        return pdfGeneratorKlient.genererPdf(
            PdfGenRequest(
                "Klage oversendelsesblankett",
                payload = blankett.klage.tilPdfgenDTO(soeker.fnr.value, soeker.formaterNavn()).toJsonNode(),
            ),
            NotatMal.KLAGE_OVERSENDELSE_BLANKETT,
        )
    }

    private suspend fun journalfoerInterntNotat(
        notat: Notat,
        sak: Sak,
        pdf: ByteArray,
    ): OpprettJournalpostResponse {
        val tittel = notat.tittel

        val journalpostRequest =
            OpprettNotatJournalpostRequest(
                bruker =
                    Bruker(
                        id = sak.ident,
                        idType = BrukerIdType.FNR,
                    ),
                dokumenter =
                    listOf(
                        JournalpostDokument(
                            tittel = tittel,
                            brevkode = "NOTAT",
                            dokumentvarianter =
                                listOf(
                                    DokumentVariant.ArkivPDF(
                                        Base64.getEncoder().encodeToString(pdf),
                                    ),
                                ),
                        ),
                    ),
                eksternReferanseId = "${sak.id}.${notat.id}",
                journalfoerendeEnhet = sak.enhet,
                sak =
                    JournalpostSak(
                        sakstype = Sakstype.FAGSAK,
                        fagsakId = sak.id.toString(),
                        tema = sak.sakType.tema,
                        fagsaksystem = Fagsaksystem.EY.navn,
                    ),
                tema = sak.sakType.tema,
                tittel = tittel,
            )

        return dokarkivService.journalfoer(journalpostRequest)
    }
}
