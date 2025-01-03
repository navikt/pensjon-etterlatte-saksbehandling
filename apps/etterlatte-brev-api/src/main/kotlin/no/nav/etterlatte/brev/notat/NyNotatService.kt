package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.dokarkiv.Bruker
import no.nav.etterlatte.brev.dokarkiv.BrukerIdType
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.DokumentVariant
import no.nav.etterlatte.brev.dokarkiv.JournalpostDokument
import no.nav.etterlatte.brev.dokarkiv.JournalpostSak
import no.nav.etterlatte.brev.dokarkiv.OpprettNotatJournalpostRequest
import no.nav.etterlatte.brev.dokarkiv.Sakstype
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.model.OpprettJournalpostResponse
import no.nav.etterlatte.brev.notat.Notat
import no.nav.etterlatte.brev.notat.NotatID
import no.nav.etterlatte.brev.notat.NotatMal
import no.nav.etterlatte.brev.notat.NotatRepository
import no.nav.etterlatte.brev.notat.NyttNotat
import no.nav.etterlatte.brev.notat.opprettSamordningsnotatPayload
import no.nav.etterlatte.brev.pdfgen.PdfGeneratorKlient
import no.nav.etterlatte.brev.pdfgen.SlatePDFMal
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import org.slf4j.LoggerFactory
import java.util.Base64

class NyNotatService(
    private val notatRepository: NotatRepository,
    private val pdfGeneratorKlient: PdfGeneratorKlient,
    private val dokarkivService: DokarkivService,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hent(id: NotatID): Notat = notatRepository.hent(id)

    fun hentForSak(sakId: SakId): List<Notat> {
        logger.info("Henter notater for sak $sakId")

        return notatRepository.hentForSak(sakId)
    }

    fun hentForReferanse(referanse: String): List<Notat> {
        logger.info("Henter notat for referanse $referanse")

        return notatRepository.hentForReferanse(referanse)
    }

    fun hentPayload(id: NotatID): Slate = notatRepository.hentPayload(id)

    fun oppdaterPayload(
        id: NotatID,
        payload: Slate,
        bruker: BrukerTokenInfo,
    ) {
        val notat = hent(id)
        if (!notat.kanRedigeres()) {
            throw KanIkkeEndreJournalfoertNotat()
        }

        notatRepository.oppdaterPayload(id, payload, bruker)
    }

    suspend fun genererPdf(id: NotatID): ByteArray {
        val notat = hent(id)

        return if (notat.kanRedigeres()) {
            val payload = SlatePDFMal(hentPayload(id))

            pdfGeneratorKlient.genererPdf(
                notat.tittel,
                payload,
                NotatMal.TOM_MAL,
            )
        } else {
            notatRepository.hentPdf(id)
        }
    }

    internal suspend fun opprettOgJournalfoer(
        sakId: SakId,
        mal: NotatMal,
        tittel: String = "Mangler tittel",
        referanse: String? = null,
        params: NotatParametre? = null,
        bruker: BrukerTokenInfo,
    ): Notat {
        val notat = opprett(sakId, mal, tittel, referanse, params, bruker)
        journalfoer(notat.id, bruker)
        return notat
    }

    suspend fun opprett(
        sakId: SakId,
        mal: NotatMal,
        tittel: String = "Mangler tittel",
        referanse: String? = null,
        params: NotatParametre? = null,
        bruker: BrukerTokenInfo,
    ): Notat {
        val sak = behandlingService.hentSak(sakId, bruker)

        logger.info("Oppretter notat for sak $sakId og referanse '$referanse'")

        val id =
            notatRepository.opprett(
                NyttNotat(
                    sakId = sak.id,
                    referanse = referanse,
                    tittel = tittel,
                    payload =
                        when (mal) {
                            NotatMal.TOM_MAL ->
                                Slate(
                                    listOf(
                                        Slate.Element(
                                            Slate.ElementType.PARAGRAPH,
                                            listOf(Slate.InnerElement(Slate.ElementType.PARAGRAPH, "Tomt notat")),
                                        ),
                                    ),
                                )

                            NotatMal.NORDISK_VEDLEGG ->
                                deserialize(
                                    javaClass.getResource("/notat/nordisk_vedlegg.json")!!.readText(),
                                )

                            NotatMal.MANUELL_SAMORDNING -> {
                                opprettSamordningsnotatPayload(params)
                            }

                            NotatMal.KLAGE_OVERSENDELSE_BLANKETT -> TODO("Foreløpig ikke støttet i ny service")
                        },
                    mal = mal,
                ),
                bruker,
            )

        return notatRepository.hent(id)
    }

    fun oppdaterTittel(
        id: NotatID,
        tittel: String,
        bruker: BrukerTokenInfo,
    ) {
        logger.info("Oppdaterer notat $id med ny tittel '$tittel'")

        val notat = hent(id)
        if (!notat.kanRedigeres()) {
            throw NotatAlleredeJournalfoert()
        }

        notatRepository.oppdaterTittel(id, tittel, bruker)
    }

    fun slett(id: NotatID) {
        val notat = hent(id)

        if (!notat.kanRedigeres()) {
            throw KanIkkeEndreJournalfoertNotat()
        }

        logger.info("Sletter notat id=$id")
        notatRepository.slett(id)
    }

    suspend fun journalfoer(
        id: NotatID,
        bruker: BrukerTokenInfo,
    ): OpprettJournalpostResponse {
        val notat = hent(id)

        if (!notat.kanRedigeres()) {
            throw NotatAlleredeJournalfoert()
        }

        val pdf = genererPdf(id)

        notatRepository.lagreInnhold(id, pdf)

        val sak = behandlingService.hentSak(notat.sakId, bruker)

        return dokarkivService
            .journalfoer(mapTilJournalpostRequest(sak, notat, pdf), bruker)
            .also {
                notatRepository.settJournalfoert(id, it, bruker)
            }
    }

    private fun mapTilJournalpostRequest(
        sak: Sak,
        notat: Notat,
        pdf: ByteArray,
    ): OpprettNotatJournalpostRequest {
        val dokumenter =
            listOf(
                JournalpostDokument(
                    tittel = notat.tittel,
                    dokumentvarianter =
                        listOf(
                            DokumentVariant.ArkivPDF(Base64.getEncoder().encodeToString(pdf)),
                        ),
                ),
            )

        val journalpostSak =
            JournalpostSak(
                sakstype = Sakstype.FAGSAK,
                fagsakId = sak.id.toString(),
                tema = sak.sakType.tema,
                fagsaksystem = Fagsaksystem.EY.navn,
            )

        return OpprettNotatJournalpostRequest(
            bruker = Bruker(sak.ident, BrukerIdType.FNR),
            dokumenter = dokumenter,
            eksternReferanseId = "${sak.id}.${notat.id}",
            journalfoerendeEnhet = sak.enhet,
            sak = journalpostSak,
            tema = sak.sakType.tema,
            tittel = notat.tittel,
        )
    }
}

class KanIkkeEndreJournalfoertNotat :
    UgyldigForespoerselException(
        code = "KAN_IKKE_ENDRE_JOURNALFOERT_NOTAT",
        detail = "Notatet er journalført og kan ikke endres!",
    )

class NotatAlleredeJournalfoert :
    UgyldigForespoerselException(
        code = "NOTAT_ALLEREDE_JOURNALFOERT",
        detail = "Notatet er allerede journalført!",
    )

interface NotatParametre
