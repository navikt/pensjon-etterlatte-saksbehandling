package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.Bruker
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.DokumentVariant
import no.nav.etterlatte.brev.dokarkiv.JournalpostDokument
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.dokarkiv.OpprettNotatJournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.notat.StrukturertBrev
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.pensjon.brevbaker.api.model.Felles
import org.slf4j.LoggerFactory
import java.util.Base64

@Deprecated("Denne servicen skal ikke lenger brukes", ReplaceWith("NyNotatService.kt"))
class NotatService(
    private val brevRepository: BrevRepository,
    private val adresseService: AdresseService,
    private val brevbakerService: BrevbakerService,
    private val grunnlagService: GrunnlagService,
    private val dokarkivKlient: DokarkivKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun journalfoerNotatISak(
        sakId: Long,
        notatData: StrukturertBrev,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse {
        val notat =
            brevRepository.opprettBrev(
                ulagretBrev =
                    OpprettNyttBrev(
                        sakId = sakId,
                        behandlingId = notatData.behandlingId,
                        soekerFnr = notatData.soekerFnr,
                        prosessType = BrevProsessType.AUTOMATISK,
                        mottaker =
                            Mottaker(
                                navn = "NAV",
                                foedselsnummer = null,
                                orgnummer = null,
                                adresse =
                                    Adresse(
                                        adresseType = "",
                                        landkode = "",
                                        land = "",
                                    ),
                            ),
                        opprettet = Tidspunkt.now(),
                        innhold =
                            BrevInnhold(
                                tittel = notatData.brevkode.tittel ?: "Internt notat",
                                spraak = Spraak.NB,
                                payload = null,
                            ),
                        innholdVedlegg = listOf(),
                        brevtype = Brevtype.NOTAT,
                        brevkoder = notatData.brevkode,
                    ),
            )
        try {
            val notatPdf = genererPdfBrevbaker(notatData, notat.id, bruker)
            brevRepository.lagrePdfOgFerdigstillBrev(notat.id, notatPdf)
            return journalfoerInterntNotat(notat, notatData.sak)
        } catch (e: Exception) {
            try {
                brevRepository.settBrevSlettet(notat.id, bruker)
            } catch (inner: Exception) {
                logger.error(
                    "Fikk en feil i opprydding etter feil med generering / journalføring av notat i sak" +
                        "med id=$sakId. Kunne ikke sette notat med id=${notat.id} til slettet, så det vil henge " +
                        "igjen. Se annen feilmelding for hva som gikk galt før opprydding",
                    inner,
                )
            }
            logger.error("Kunne ikke generere og journalføre notat i sak=$sakId på grunn av feil: ", e)
            throw e
        }
    }

    private suspend fun genererPdfBrevbaker(
        notatData: StrukturertBrev,
        notatId: BrevID?,
        bruker: BrukerTokenInfo,
    ): Pdf =
        brevbakerService.genererPdf(
            notatId,
            brevRequest =
                BrevbakerRequest.fraStrukturertBrev(
                    strukturertBrev = notatData,
                    felles = mapFelles(notatData.sak.id, notatData.sak.enhet, bruker),
                ),
        )

    private suspend fun journalfoerInterntNotat(
        notat: Brev,
        sak: Sak,
    ): OpprettJournalpostResponse {
        val pdf =
            checkNotNull(brevRepository.hentPdf(notat.id)) {
                "Kan ikke journalføre hvis vi ikke har en pdf"
            }
        val tittel = notat.tittel ?: "Internt notat"

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
                                        Base64.getEncoder().encodeToString(pdf.bytes),
                                    ),
                                ),
                        ),
                    ),
                eksternReferanseId = notat.id.toString(),
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
        val opprettJournalpostResponse =
            dokarkivKlient.opprettJournalpost(request = journalpostRequest, ferdigstill = true)
        brevRepository.settBrevJournalfoert(notat.id, opprettJournalpostResponse)
        return opprettJournalpostResponse
    }

    private suspend fun mapFelles(
        sakId: Long,
        enhet: String,
        saksbehandler: BrukerTokenInfo,
    ): Felles {
        val avsender =
            adresseService.hentAvsender(
                request =
                    AvsenderRequest(
                        saksbehandlerIdent = saksbehandler.ident(),
                        sakenhet = enhet,
                        attestantIdent = null,
                    ),
            )
        val grunnlag = grunnlagService.hentGrunnlagForSak(sakId, saksbehandler)
        val soeker = grunnlag.mapSoeker(null)
        return BrevbakerHelpers.mapFelles(sakId, soeker, avsender)
    }

    suspend fun forhaandsvisNotat(
        notatData: StrukturertBrev,
        bruker: BrukerTokenInfo,
    ): Pdf = genererPdfBrevbaker(notatData, null, bruker)

    fun hentPdf(notatId: BrevID): Pdf =
        checkNotNull(brevRepository.hentPdf(notatId)) {
            "Fant ikke generert pdf for notat med id=$notatId"
        }
}
