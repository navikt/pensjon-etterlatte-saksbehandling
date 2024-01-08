package no.nav.etterlatte.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevProsessType.AUTOMATISK
import no.nav.etterlatte.brev.model.BrevProsessType.MANUELL
import no.nav.etterlatte.brev.model.BrevProsessType.REDIGERBAR
import no.nav.etterlatte.brev.model.BrevProsessTypeFactory
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverk
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverkFerdig
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksbrevService(
    private val db: BrevRepository,
    private val brevdataFacade: BrevdataFacade,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val adresseService: AdresseService,
    private val brevbaker: BrevbakerService,
    private val brevDataMapper: BrevDataMapper,
    private val brevProsessTypeFactory: BrevProsessTypeFactory,
    private val migreringBrevDataService: MigreringBrevDataService,
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
        require(hentVedtaksbrev(behandlingId) == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            brevdataFacade.hentBehandling(behandlingId, brukerTokenInfo).status.let { status ->
                require(status.kanEndres()) {
                    "Behandling (id=$behandlingId) har status $status og kan ikke opprette vedtaksbrev"
                }
            }
        }

        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, brukerTokenInfo) }

        val mottaker =
            with(generellBrevData.personerISak) {
                when (verge) {
                    is Vergemaal ->
                        verge.toMottaker()

                    else -> {
                        val mottakerFnr =
                            innsender?.fnr?.value?.takeUnless { it == Vedtaksloesning.PESYS.name } ?: soeker.fnr.value
                        adresseService.hentMottakerAdresse(mottakerFnr)
                    }
                }
            }

        val prosessType =
            brevProsessTypeFactory.fra(
                generellBrevData,
                erOmregningNyRegel = automatiskMigreringRequest?.erOmregningGjenny ?: false,
            )

        val nyttBrev =
            OpprettNyttBrev(
                sakId = sakId,
                behandlingId = behandlingId,
                prosessType = prosessType,
                soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                mottaker = mottaker,
                opprettet = Tidspunkt.now(),
                innhold =
                    opprettInnhold(
                        RedigerbarTekstRequest(
                            generellBrevData,
                            brukerTokenInfo,
                            prosessType,
                            automatiskMigreringRequest,
                        ),
                    ),
                innholdVedlegg = opprettInnholdVedlegg(generellBrevData, prosessType),
            )

        return db.opprettBrev(nyttBrev)
    }

    suspend fun genererPdf(
        id: BrevID,
        brukerTokenInfo: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): Pdf {
        val brev = hentBrev(id)

        if (!brev.kanEndres()) {
            logger.info("Brev har status ${brev.status} - returnerer lagret innhold")
            return requireNotNull(db.hentPdf(brev.id)) { "Fant ikke brev med id ${brev.id}" }
        }

        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(brev.sakId, brev.behandlingId!!, brukerTokenInfo) }
        val avsender = adresseService.hentAvsender(generellBrevData.avsenderRequest())

        val brevkodePar =
            brevDataMapper.brevKode(
                generellBrevData,
                brev.prosessType,
                erOmregningNyRegel = automatiskMigreringRequest?.erOmregningGjenny ?: false,
            )

        val brevData =
            when (
                generellBrevData.systemkilde == Vedtaksloesning.PESYS ||
                    automatiskMigreringRequest?.erOmregningGjenny ?: false
            ) {
                false -> opprettBrevData(brev, generellBrevData, brukerTokenInfo, brevkodePar)
                true ->
                    OmregnetBPNyttRegelverkFerdig(
                        innhold =
                            InnholdMedVedlegg(
                                { hentLagretInnhold(brev) },
                                { hentLagretInnholdVedlegg(brev) },
                            ).innhold(),
                        data = (
                            migreringBrevDataService.opprettMigreringBrevdata(
                                generellBrevData,
                                automatiskMigreringRequest,
                                brukerTokenInfo,
                            ) as OmregnetBPNyttRegelverk
                        ),
                    )
            }

        val brevRequest =
            BrevbakerRequest.fra(
                brevkodePar.ferdigstilling,
                brevData,
                avsender,
                generellBrevData.personerISak.soekerOgEventuellVerge(),
                generellBrevData.sak.id,
                generellBrevData.spraak,
                generellBrevData.sak.sakType,
            )

        return brevbaker.genererPdf(brev.id, brevRequest)
            .let {
                when (brevData) {
                    is OmregnetBPNyttRegelverkFerdig -> {
                        val forhaandsvarsel =
                            brevbaker.genererPdf(
                                brev.id,
                                BrevbakerRequest.fra(
                                    EtterlatteBrevKode.BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
                                    brevData.data,
                                    avsender,
                                    generellBrevData.personerISak.soekerOgEventuellVerge(),
                                    generellBrevData.sak.id,
                                    generellBrevData.spraak,
                                    generellBrevData.sak.sakType,
                                ),
                            )
                        forhaandsvarsel.medPdfAppended(it)
                    }

                    else -> it
                }
            }
            .also { pdf ->
                lagrePdfHvisVedtakFattet(
                    brev.id,
                    generellBrevData.forenkletVedtak,
                    pdf,
                    brukerTokenInfo,
                    automatiskMigreringRequest != null,
                )
            }
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
    ): BrevService.BrevPayload {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, brukerTokenInfo) }
        val prosessType = brevProsessTypeFactory.fra(generellBrevData)
        val innhold = opprettInnhold(RedigerbarTekstRequest(generellBrevData, brukerTokenInfo, prosessType))
        val innholdVedlegg = opprettInnholdVedlegg(generellBrevData, prosessType)

        if (innhold.payload != null) {
            db.oppdaterPayload(brevId, innhold.payload)
        }

        if (innholdVedlegg != null) {
            db.oppdaterPayloadVedlegg(brevId, innholdVedlegg)
        }

        return BrevService.BrevPayload(
            innhold.payload ?: db.hentBrevPayload(brevId),
            innholdVedlegg ?: db.hentBrevPayloadVedlegg(brevId),
        )
    }

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

    private suspend fun opprettInnhold(redigerbarTekstRequest: RedigerbarTekstRequest): BrevInnhold {
        val tittel = "Vedtak om ${redigerbarTekstRequest.vedtakstype()}"

        val payload =
            when (redigerbarTekstRequest.prosessType) {
                REDIGERBAR -> brevbaker.hentRedigerbarTekstFraBrevbakeren(redigerbarTekstRequest)
                AUTOMATISK -> null
                MANUELL -> SlateHelper.hentInitiellPayload(redigerbarTekstRequest.generellBrevData)
            }

        return BrevInnhold(tittel, redigerbarTekstRequest.generellBrevData.spraak, payload)
    }

    private fun opprettInnholdVedlegg(
        generellBrevData: GenerellBrevData,
        prosessType: BrevProsessType,
    ): List<BrevInnholdVedlegg>? =
        when (prosessType) {
            REDIGERBAR -> SlateHelper.hentInitiellPayloadVedlegg(generellBrevData)
            AUTOMATISK -> null
            MANUELL -> null
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
    return Mottaker(
        navn = mottaker.navn!!,
        foedselsnummer = mottaker.foedselsnummer?.let { Foedselsnummer(it.value) },
        orgnummer = null,
        adresse =
            with(mottaker.adresse) {
                Adresse(adresseType, adresselinje1, adresselinje2, adresselinje3, postnummer, poststed, landkode, land)
            },
    )
}
