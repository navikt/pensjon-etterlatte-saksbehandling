package no.nav.etterlatte.brev.vedtaksbrev

import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.BrevDataFerdigstillingNy
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevServiceNy(
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val db: BrevRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogger = sikkerlogger()

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull()
    }

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Brev {
        krev(db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        val (spraak, sak, innsender, soeker, avdoede, verge, saksbehandlerIdent, attestantIdent) = brevRequest

        val avsender = utledAvsender(bruker, saksbehandlerIdent, attestantIdent, sak.enhet)

        val brevKode = brevRequest.brevInnholdData.brevKode

        val innhold =
            brevbaker.hentRedigerbarTekstFraBrevbakeren(
                BrevbakerRequest.fra(
                    brevKode = brevKode.redigering,
                    brevData = ManueltBrevData(),
                    avsender = avsender,
                    soekerOgEventuellVerge = SoekerOgEventuellVerge(soeker, verge),
                    sakId = sak.id,
                    spraak = spraak,
                    sakType = sak.sakType,
                ),
            )

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sak.id,
                behandlingId = behandlingId,
                prosessType = BrevProsessType.REDIGERBAR,
                soekerFnr = soeker.fnr.value,
                mottakere =
                    adresseService.hentMottakere(
                        sak.sakType,
                        PersonerISak(
                            innsender,
                            soeker,
                            avdoede,
                            verge,
                        ),
                        bruker,
                    ),
                opprettet = Tidspunkt.now(),
                innhold =
                    BrevInnhold(
                        brevKode.titlerPaaSpraak[spraak] ?: brevKode.tittel,
                        spraak,
                        innhold,
                    ),
                innholdVedlegg = emptyList(),
                brevtype = brevKode.brevtype,
                brevkoder = brevKode,
            )

        return db.opprettBrev(nyttBrev, bruker)
    }

    suspend fun genererPdf(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Pdf {
        val brev = db.hentBrev(brevId)

        val brevInnholdData = utledBrevInnholdData(brev, brevRequest)

        // TODO Må dette hentes på nytt ved forhåndsvisning? Det kan endre seg?
        val avsender =
            utledAvsender(bruker, brevRequest.saksbehandlerIdent, brevRequest.attestantIdent, brevRequest.sak.enhet)

        val pdf = opprettPdf(brev, brevRequest, brevInnholdData, avsender)

        // TODO ferdigstilles vel aldri her?
        logger.info("PDF generert ok. Sjekker om den skal lagres og ferdigstilles")
        brev.brevkoder?.let { db.oppdaterBrevkoder(brev.id, it) }
        if (brevRequest.skalLagre) {
            logger.info("Lagrer PDF for brev med id=$brevId")
            db.lagrePdf(brevId, pdf)
        }

        return pdf
    }

    fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev =
            krevIkkeNull(hentVedtaksbrev(behandlingId)) {
                "Fant ingen brev for behandling (id=$behandlingId)"
            }

        if (brev.status == Status.FERDIGSTILT) {
            logger.warn("Brev (id=${brev.id}) er allerede ferdigstilt. Avbryter ferdigstilling...")
            return
        } else if (!brev.kanEndres()) {
            throw UgyldigStatusKanIkkeFerdigstilles(brev.id, brev.status)
        } else if (brev.mottakere.size !in 1..2) {
            logger.error("Brev ${brev.id} har ${brev.mottakere.size} mottakere. Dette skal ikke være mulig...")
            throw UgyldigAntallMottakere()
        } else if (brev.mottakere.any { it.erGyldig().isNotEmpty() }) {
            sikkerlogger.error("Ugyldig mottaker(e): ${brev.mottakere.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id, brev.sakId, brev.mottakere.flatMap { it.erGyldig() })
        }

        logger.info("Ferdigstiller brev med id=${brev.id}")
        if (db.hentPdf(brev.id) == null) {
            throw BrevManglerPDF(brev.id)
        } else {
            db.settBrevFerdigstilt(brev.id, brukerTokenInfo)
        }
    }

    // TODO tilbakestill

    private fun utledBrevInnholdData(
        brev: Brev,
        brevRequest: BrevRequest,
    ): BrevDataFerdigstillingNy {
        val innholdMedVedlegg =
            InnholdMedVedlegg(
                {
                    krevIkkeNull(
                        db.hentBrevPayload(brev.id),
                    ) { "Fant ikke payload for brev ${brev.id}" }.elements
                },
                {
                    krevIkkeNull(db.hentBrevPayloadVedlegg(brev.id)) {
                        "Fant ikke payloadvedlegg for brev ${brev.id}"
                    }
                },
            )

        return BrevDataFerdigstillingNy(
            innhold = innholdMedVedlegg.innhold(),
            data = brevRequest.brevInnholdData,
        )
    }

    private suspend fun utledAvsender(
        bruker: BrukerTokenInfo,
        saksbehandlerIdent: String,
        attestantIdent: String,
        enhet: Enhetsnummer,
    ): Avsender {
        val avsender =
            adresseService.hentAvsender(
                request =
                    AvsenderRequest(
                        saksbehandlerIdent = saksbehandlerIdent,
                        attestantIdent = attestantIdent,
                        sakenhet = enhet,
                    ),
                bruker = bruker,
            )
        return avsender
    }

    private suspend fun opprettPdf(
        brev: Brev,
        brevRequest: BrevRequest,
        brevInnholdData: BrevData,
        avsender: Avsender,
    ): Pdf {
        val brevbakerRequest =
            BrevbakerRequest.fra(
                brevKode = brevRequest.brevInnholdData.brevKode.ferdigstilling,
                brevData = brevInnholdData,
                avsender = avsender,
                soekerOgEventuellVerge = SoekerOgEventuellVerge(brevRequest.soeker, brevRequest.verge),
                sakId = brevRequest.sak.id,
                spraak = brev.spraak, // TODO godt nok?,
                sakType = brevRequest.sak.sakType,
            )
        return brevbaker.genererPdf(brev.id, brevbakerRequest)
    }
}
