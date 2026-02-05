package no.nav.etterlatte.brev.vedtaksbrev

import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.BrevDataFerdigstillingNy
import no.nav.etterlatte.brev.BrevDataRedigerbarNy
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.BrevService
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class StrukturertBrevService(
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val journalfoerBrevService: JournalfoerBrevService,
    private val distribuerBrev: Brevdistribuerer,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogger = sikkerlogger()

    private fun hentBrevAvTypeForBehandling(
        behandlingId: UUID,
        brevType: Brevtype,
    ): Brev? {
        logger.info("Henter brev av type=$brevType for behandling (id=$behandlingId)")
        return db.hentBrevForBehandling(behandlingId, brevType).firstOrNull()
    }

    suspend fun opprettStrukturertBrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Brev {
        val typeForBrev = brevRequest.brevFastInnholdData.brevKode.brevtype
        val brev = hentBrevAvTypeForBehandling(behandlingId, typeForBrev)

        if (brev != null) {
            logger.info(
                "Strukturert brev av type $typeForBrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt. Returnerer brev (id=${brev.id})",
            )
            return brev
        }

        val (spraak, sak, innsender, soeker, avdoede, verge, saksbehandlerIdent, attestantIdent) = brevRequest
        val brevKode = brevRequest.brevFastInnholdData.brevKode
        val avsender = utledAvsender(bruker, saksbehandlerIdent, attestantIdent, sak.enhet)
        val soekerOgEventuellVerge = SoekerOgEventuellVerge(soeker, verge)

        val innhold =
            brevbaker.hentRedigerbarTekstFraBrevbakeren(
                BrevbakerRequest.fra(
                    brevKode = brevKode.redigering,
                    brevData = utledBrevRedigerbartInnholdData(brevRequest) ?: ManueltBrevData(),
                    avsender = avsender,
                    soekerOgEventuellVerge = soekerOgEventuellVerge,
                    sakId = sak.id,
                    spraak = spraak,
                    sakType = sak.sakType,
                ),
            )

        val innholdVedlegg = hentInnholdForVedlegg(brevRequest.brevVedleggData, avsender, soekerOgEventuellVerge, spraak, sak)

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
                            emptyList(), // TODO her burde det slenges på hvis BP i requesten..
                        ),
                        bruker,
                    ),
                opprettet = Tidspunkt.now(),
                innhold =
                    BrevInnhold(
                        brevKode.tittel(spraak),
                        spraak,
                        innhold,
                    ),
                innholdVedlegg = innholdVedlegg,
                brevtype = brevKode.brevtype,
                brevkoder = brevKode,
            )

        return db.opprettBrev(nyttBrev, bruker, true)
    }

    suspend fun genererEllerHentPdf(
        brevId: BrevID,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Pdf {
        val brev = db.hentBrev(brevId)

        if (!brev.kanEndres()) {
            logger.info("Brev (id=${brev.id}) har status=${brev.status} og kan ikke endres. Henter PDF...")
            return db.hentPdf(brevId) ?: throw BrevManglerPDF(brev.id)
        }

        logger.info("Genererer PDF for brev med id=$brevId, skalLagres=${brevRequest.skalLagre}")
        val brevInnholdData = utledBrevInnholdData(brev, brevRequest)
        val avsender = utledAvsender(bruker, brevRequest.saksbehandlerIdent, brevRequest.attestantIdent, brevRequest.sak.enhet)
        val pdf = opprettPdf(brev, brevRequest, brevInnholdData, avsender)

        brev.brevkoder?.let { db.oppdaterBrevkoder(brev.id, it) }

        if (brevRequest.skalLagre) {
            logger.info("Lagrer PDF for brev med id=$brevId")
            db.lagrePdf(brevId, pdf)
        }

        return pdf
    }

    suspend fun ferdigstillJournalfoerOgDistribuerStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev =
            krevIkkeNull(hentBrevAvTypeForBehandling(behandlingId, brevType)) {
                "Fant ingen brev for behandling (id=$behandlingId)"
            }

        try {
            if (brev.status.ikkeFerdigstilt()) {
                ferdigstillStrukturertBrev(behandlingId, brevType, brukerTokenInfo)
            } else {
                logger.info("Behandling=$behandlingId med strukturert brev=${brev.id} er allerede ferdigstilt")
            }

            if (brev.status.ikkeJournalfoert()) {
                journalfoerBrevService.journalfoer(brev.id, brukerTokenInfo)
            } else {
                logger.info("Behandling=$behandlingId med strukturert brev=${brev.id} er allerede journalfoert")
            }

            if (brev.status.ikkeDistribuert()) {
                distribuerBrev.distribuer(brev.id, DistribusjonsType.ANNET, brukerTokenInfo)
            } else {
                logger.info("Behandling=$behandlingId med strukturert brev=${brev.id} er allerede distribuert")
            }
        } catch (e: Exception) {
            logger.error(
                "Det oppstod en feil under ferdigstilling, journalføring eller distribusjon av strukturert brev med brevID=${brev.id}, status: ${brev.status}",
                e,
            )
            throw e
        }
    }

    fun ferdigstillStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev =
            krevIkkeNull(hentBrevAvTypeForBehandling(behandlingId, brevType)) {
                "Fant ingen brev for behandling (id=$behandlingId)"
            }

        if (brev.erFerdigstilt()) {
            logger.warn("Brev (id=${brev.id}) er allerede ferdigstilt. Avbryter ferdigstilling...")
            return
        }

        if (!brev.kanEndres()) {
            throw UgyldigStatusKanIkkeFerdigstilles(brev.id, brev.status)
        }

        if (brev.mottakere.size !in 1..2) {
            logger.error("Brev ${brev.id} har ${brev.mottakere.size} mottakere. Dette skal ikke være mulig...")
            throw UgyldigAntallMottakere()
        } else if (brev.mottakere.any { it.erGyldig().isNotEmpty() }) {
            sikkerlogger.error("Ugyldig mottaker(e): ${brev.mottakere.toJson()}")
            throw UgyldigMottakerKanIkkeFerdigstilles(brev.id, brev.sakId, brev.mottakere.flatMap { it.erGyldig() })
        }

        logger.info("Ferdigstiller brev med id=${brev.id}")
        db.hentPdf(brev.id) ?: throw BrevManglerPDF(brev.id)
        db.settBrevFerdigstilt(brev.id, brukerTokenInfo)
    }

    suspend fun tilbakestillStrukturertBrev(
        brevId: Long,
        bruker: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): BrevService.BrevPayload {
        val brev = db.hentBrev(brevId)
        if (!brev.kanEndres()) {
            throw UgyldigForespoerselException(
                "BREVET_KAN_IKKE_ENDRES",
                "Kan ikke oppdatere brevet med id=$brevId, fordi brevet har status (${brev.status})",
            )
        }

        val (spraak, sak, _, soeker, _, verge, saksbehandlerIdent, attestantIdent, _, brevInnholdData) = brevRequest

        val brevKode = brevInnholdData.brevKode
        val avsender = utledAvsender(bruker, saksbehandlerIdent, attestantIdent, sak.enhet)
        val soekerOgEventuellVerge = SoekerOgEventuellVerge(soeker, verge)

        val spraakIBrev = db.hentBrevInnhold(brevId)?.spraak ?: spraak

        val brevinnhold =
            BrevInnhold(
                brevKode.tittel(spraak),
                spraakIBrev,
                brevbaker.hentRedigerbarTekstFraBrevbakeren(
                    BrevbakerRequest.fra(
                        brevKode = brevKode.redigering,
                        brevData = utledBrevRedigerbartInnholdData(brevRequest) ?: ManueltBrevData(),
                        avsender = avsender,
                        soekerOgEventuellVerge = soekerOgEventuellVerge,
                        sakId = sak.id,
                        spraak = spraakIBrev,
                        sakType = sak.sakType,
                    ),
                ),
            )

        if (brevinnhold.payload != null) {
            db.oppdaterPayload(brevId, brevinnhold.payload, bruker)
        }

        val innholdVedlegg = hentInnholdForVedlegg(brevRequest.brevVedleggData, avsender, soekerOgEventuellVerge, spraakIBrev, sak)
        if (innholdVedlegg.isNotEmpty()) {
            db.oppdaterPayloadVedlegg(brevId, innholdVedlegg, bruker)
        }

        if (brev.brevkoder != brevInnholdData.brevKode) {
            db.oppdaterBrevkoder(brevId, brevInnholdData.brevKode)
            db.oppdaterTittel(brevId, brevinnhold.tittel, bruker)
        }

        return BrevService.BrevPayload(
            brevinnhold.payload ?: db.hentBrevPayload(brevId),
            innholdVedlegg,
        )
    }

    private fun utledBrevRedigerbartInnholdData(brevRequest: BrevRequest): BrevDataRedigerbarNy? =
        brevRequest.brevRedigerbarInnholdData?.let {
            BrevDataRedigerbarNy(data = it)
        }

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
            data = brevRequest.brevFastInnholdData.medVedleggInnhold(innholdMedVedlegg.innholdVedlegg),
        )
    }

    private suspend fun utledAvsender(
        bruker: BrukerTokenInfo,
        saksbehandlerIdent: String,
        attestantIdent: String?,
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
                brevKode = brevRequest.brevFastInnholdData.brevKode.ferdigstilling,
                brevData = brevInnholdData,
                avsender = avsender,
                soekerOgEventuellVerge = SoekerOgEventuellVerge(brevRequest.soeker, brevRequest.verge),
                sakId = brevRequest.sak.id,
                spraak = brev.spraak,
                sakType = brevRequest.sak.sakType,
            )
        return brevbaker.genererPdf(brev.id, brevbakerRequest)
    }

    private suspend fun hentInnholdForVedlegg(
        brevVedleggData: List<BrevVedleggRedigerbarNy>,
        avsender: Avsender,
        soekerOgEventuellVerge: SoekerOgEventuellVerge,
        spraak: Spraak,
        sak: Sak,
    ): List<BrevInnholdVedlegg> =
        brevVedleggData
            .map {
                BrevInnholdVedlegg(
                    tittel = it.vedlegg.tittel,
                    key = it.vedleggId,
                    payload =
                        brevbaker.hentRedigerbarTekstFraBrevbakeren(
                            BrevbakerRequest.fra(
                                brevKode = it.vedlegg,
                                brevData = it,
                                avsender = avsender,
                                soekerOgEventuellVerge = soekerOgEventuellVerge,
                                sakId = sak.id,
                                spraak = spraak,
                                sakType = sak.sakType,
                            ),
                        ),
                )
            }.toList()
}
