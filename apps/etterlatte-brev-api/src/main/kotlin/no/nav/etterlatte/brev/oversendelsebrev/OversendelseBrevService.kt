package no.nav.etterlatte.brev.oversendelsebrev

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.AvsenderRequest
import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataFerdigstillingRequest
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.pdf.PDFGenerator
import no.nav.etterlatte.brev.vedtaksbrev.VedtaksbrevKanIkkeSlettes
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.util.UUID

interface OversendelseBrevService {
    fun hentOversendelseBrev(behandlingId: UUID): Brev?

    suspend fun opprettOversendelseBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun pdf(
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf

    fun ferdigstillOversendelseBrev(
        brevId: Long,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf

    suspend fun slettOversendelseBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class OversendelseBrevServiceImpl(
    private val brevRepository: BrevRepository,
    private val pdfGenerator: PDFGenerator,
    private val adresseService: AdresseService,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
) : OversendelseBrevService {
    override fun hentOversendelseBrev(behandlingId: UUID): Brev? =
        brevRepository.hentBrevForBehandling(behandlingId, Brevtype.OVERSENDELSE_KLAGE).singleOrNull()

    override suspend fun opprettOversendelseBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        val eksisterendeBrev =
            brevRepository
                .hentBrevForBehandling(behandlingId, Brevtype.OVERSENDELSE_KLAGE)
                .singleOrNull()
        if (eksisterendeBrev != null) {
            return eksisterendeBrev
        }
        val klage = behandlingService.hentKlage(behandlingId, brukerTokenInfo)
        val (spraak, personerISak) = hentSpraakOgPersonerISak(klage, brukerTokenInfo)

        val mottakere = adresseService.hentMottakere(klage.sak.sakType, personerISak)

        val brev =
            brevRepository.opprettBrev(
                OpprettNyttBrev(
                    sakId = klage.sak.id,
                    behandlingId = behandlingId,
                    soekerFnr = klage.sak.ident,
                    prosessType = BrevProsessType.AUTOMATISK,
                    mottakere = mottakere,
                    opprettet = Tidspunkt.now(),
                    innhold =
                        BrevInnhold(
                            tittel = Brevkoder.OVERSENDELSE_KLAGE.tittel,
                            spraak = spraak,
                        ),
                    innholdVedlegg = listOf(),
                    brevtype = Brevtype.OVERSENDELSE_KLAGE,
                    brevkoder = Brevkoder.OVERSENDELSE_KLAGE,
                ),
                brukerTokenInfo,
            )
        return brev
    }

    suspend fun hentSpraakOgPersonerISak(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pair<Spraak, PersonerISak> {
        val grunnlag =
            grunnlagService.hentGrunnlag(
                vedtakType = null,
                sakId = klage.sak.id,
                bruker = brukerTokenInfo,
                // Setter behandlingId som null for å unngå å hente en behandling med klageId'en
                behandlingId = null,
            )
        val verge =
            grunnlagService.hentVergeForSak(
                sakType = behandlingService.hentSak(klage.sak.id, brukerTokenInfo).sakType,
                brevutfallDto = null,
                grunnlag = grunnlag,
            )
        val personerISak =
            PersonerISak(
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = verge,
            )
        return Pair(grunnlag.mapSpraak(), personerISak)
    }

    override suspend fun pdf(
        brevId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val brev = brevRepository.hentBrev(brevId)
        if (!brev.kanEndres()) {
            return brevRepository.hentPdf(
                brevId,
            ) ?: throw InternfeilException("Har et brev som ikke kan endres men ikke har en pdf, brevId=$brevId")
        }
        if (brev.brevtype != Brevtype.OVERSENDELSE_KLAGE) {
            throw UgyldigForespoerselException(
                "FEIL_BREVTYPE",
                "Brevet med id=$brevId er ikke av typen oversendelse klage, og kan ikke hentes som et oversendelse klage-brev",
            )
        }
        if (brev.behandlingId != behandlingId) {
            throw UgyldigForespoerselException(
                "FEIL_BREV_ID",
                "Brevet med id=$brevId er ikke koblet på behandlingen med id=$behandlingId",
            )
        }

        val klage = behandlingService.hentKlage(requireNotNull(brev.behandlingId), brukerTokenInfo)
        return pdfGenerator.genererPdf(
            id = brev.id,
            bruker = brukerTokenInfo,
            avsenderRequest = { bruker, _, enhet -> AvsenderRequest(bruker.ident(), enhet) },
            brevKodeMapping = { Brevkoder.OVERSENDELSE_KLAGE },
            brevDataMapping = { req -> OversendelseBrevFerdigstillingData.fra(req, klage) },
        )
    }

    override fun ferdigstillOversendelseBrev(
        brevId: Long,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        val brev = brevRepository.hentBrev(brevId)
        if (brev.sakId != sakId) {
            throw MismatchSakOgBrevException(brevId, sakId)
        }
        if (!brev.kanEndres()) {
            return brevRepository.hentPdf(brevId) ?: throw GenerellIkkeFunnetException()
        }
        if (brev.brevtype != Brevtype.OVERSENDELSE_KLAGE) {
            throw UgyldigForespoerselException(
                "FEIL_BREVTYPE",
                "Kan ikke ferdigstille brevet med id=$brevId som oversendelsesbrev, " +
                    "siden det har feil brevtype",
            )
        }
        val behandlingId = checkNotNull(brev.behandlingId)

        val klage =
            runBlocking {
                behandlingService.hentKlage(klageId = behandlingId, brukerTokenInfo)
            }

        return runBlocking {
            pdfGenerator.ferdigstillOgGenererPDF(
                id = brev.id,
                bruker = brukerTokenInfo,
                avsenderRequest = { bruker, _, enhet -> AvsenderRequest(bruker.ident(), enhet) },
                brevKodeMapping = { Brevkoder.OVERSENDELSE_KLAGE },
                brevDataMapping = { req -> OversendelseBrevFerdigstillingData.fra(req, klage) },
            )
        }
    }

    override suspend fun slettOversendelseBrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val brev =
            brevRepository
                .hentBrevForBehandling(behandlingId, Brevtype.OVERSENDELSE_KLAGE)
                .singleOrNull() ?: return
        if (!brev.kanEndres()) {
            throw VedtaksbrevKanIkkeSlettes(brev.id, "Brevet har status (${brev.status})")
        }
        val behandlingKanEndres = behandlingService.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
        if (!behandlingKanEndres) {
            throw VedtaksbrevKanIkkeSlettes(brev.id, "Behandlingen til vedtaksbrevet kan ikke endres")
        }
        brevRepository.settBrevSlettet(brev.id, brukerTokenInfo)
    }
}

data class OversendelseBrevFerdigstillingData(
    val sakType: SakType,
    val klageDato: LocalDate,
    val vedtakDato: LocalDate,
    val innstillingTekst: String,
    val under18Aar: Boolean,
    val harVerge: Boolean,
    val bosattIUtlandet: Boolean,
) : BrevDataFerdigstilling {
    override val innhold: List<Slate.Element> = emptyList()

    companion object {
        fun fra(
            request: BrevDataFerdigstillingRequest,
            klage: Klage,
        ): BrevDataFerdigstilling {
            val innstilling =
                when (val utfall = klage.utfall) {
                    is KlageUtfallMedData.DelvisOmgjoering -> {
                        utfall.innstilling
                    }

                    is KlageUtfallMedData.StadfesteVedtak -> {
                        utfall.innstilling
                    }

                    else -> throw UgyldigForespoerselException(
                        "FEIL_DATA_KLAGE",
                        "Klagen med id=${klage.id} har ikke en innstilling, så kan ikke lage et oversendelsesbrev",
                    )
                }

            return OversendelseBrevFerdigstillingData(
                sakType = request.sakType,
                klageDato = klage.innkommendeDokument?.mottattDato ?: klage.opprettet.toLocalDate(),
                vedtakDato =
                    checkNotNull(
                        klage.formkrav
                            ?.formkrav
                            ?.vedtaketKlagenGjelder
                            ?.datoAttestert
                            ?.toLocalDate(),
                    ) {
                        "Klagen har en ugyldig referanse til når originalt vedtak ble attestert, klageId=${klage.id}"
                    },
                innstillingTekst = innstilling.innstillingTekst,
                under18Aar = request.soekerUnder18 ?: false,
                harVerge = request.harVerge,
                // TODO: støtte bosatt utland klage
                bosattIUtlandet = false,
            )
        }
    }
}

class MismatchSakOgBrevException(
    brevId: BrevID,
    sakId: SakId,
) : UgyldigForespoerselException(
        code = "SAKID_MATCHER_IKKE",
        detail = "Brevet med id=$brevId har ikke angitt sakId=$sakId",
    )
