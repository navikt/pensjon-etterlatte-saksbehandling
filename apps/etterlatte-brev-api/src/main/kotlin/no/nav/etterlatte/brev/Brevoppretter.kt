package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.EtterlatteBrevKode
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevKodeMapper
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.SlateHelper
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.slf4j.LoggerFactory
import java.util.UUID

class Brevoppretter(
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val brevdataFacade: BrevdataFacade,
    private val brevbaker: BrevbakerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

        return opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
            automatiskMigreringRequest = automatiskMigreringRequest,
        ).first
    }

    suspend fun opprettBrev(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: EtterlatteBrevKode? = null,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): Pair<Brev, GenerellBrevData> =
        with(hentInnData(sakId, behandlingId, bruker, brevKode, automatiskMigreringRequest)) {
            val nyttBrev =
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    prosessType = prosessType,
                    soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                    mottaker = finnMottaker(generellBrevData.personerISak),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = innholdVedlegg,
                )
            return Pair(db.opprettBrev(nyttBrev), generellBrevData)
        }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: EtterlatteBrevKode? = null,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): BrevService.BrevPayload =
        with(hentInnData(sakId, behandlingId, bruker, brevKode, automatiskMigreringRequest)) {
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

    private suspend fun hentInnData(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: EtterlatteBrevKode?,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
    ): OpprettBrevRequest {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, bruker) }

        val prosessType = BrevProsessType.REDIGERBAR

        val brevkode: (mapper: BrevKodeMapper, g: GenerellBrevData) -> EtterlatteBrevKode =
            if (brevKode != null) {
                { _, _ -> brevKode }
            } else {
                { mapper, data ->
                    mapper.brevKode(data, BrevProsessType.REDIGERBAR).redigering
                }
            }

        val innhold =
            opprettInnhold(
                RedigerbarTekstRequest(
                    generellBrevData,
                    bruker,
                    prosessType,
                    brevkode,
                    automatiskMigreringRequest,
                ),
                brevKode?.tittel,
            )

        val innholdVedlegg = opprettInnholdVedlegg(generellBrevData, prosessType)
        return OpprettBrevRequest(generellBrevData, prosessType, innhold, innholdVedlegg)
    }

    private suspend fun finnMottaker(personerISak: PersonerISak): Mottaker =
        with(personerISak) {
            when (verge) {
                is Vergemaal -> verge.toMottaker()

                else -> {
                    val mottakerFnr =
                        innsender?.fnr?.value?.takeUnless { it == Vedtaksloesning.PESYS.name }
                            ?: soeker.fnr.value
                    adresseService.hentMottakerAdresse(mottakerFnr)
                }
            }
        }

    fun hentVedtaksbrev(behandlingId: UUID): Brev? {
        logger.info("Henter vedtaksbrev for behandling (id=$behandlingId)")

        return db.hentBrevForBehandling(behandlingId)
    }

    private suspend fun opprettInnhold(
        redigerbarTekstRequest: RedigerbarTekstRequest,
        muligTittel: String?,
    ): BrevInnhold {
        val tittel = muligTittel ?: (redigerbarTekstRequest.vedtakstype()?.let { "Vedtak om $it" } ?: "Tittel mangler")
        val payload =
            when (redigerbarTekstRequest.prosessType) {
                BrevProsessType.REDIGERBAR -> brevbaker.hentRedigerbarTekstFraBrevbakeren(redigerbarTekstRequest)
                BrevProsessType.AUTOMATISK -> null
                BrevProsessType.MANUELL -> throw IllegalStateException("Brevdata ikke relevant for ${BrevProsessType.MANUELL}")
                BrevProsessType.OPPLASTET_PDF -> throw IllegalStateException("Payload ikke relevant for ${BrevProsessType.OPPLASTET_PDF}")
            }

        return BrevInnhold(tittel, redigerbarTekstRequest.generellBrevData.spraak, payload)
    }

    private fun opprettInnholdVedlegg(
        generellBrevData: GenerellBrevData,
        prosessType: BrevProsessType,
    ): List<BrevInnholdVedlegg>? =
        when (prosessType) {
            BrevProsessType.REDIGERBAR -> SlateHelper.hentInitiellPayloadVedlegg(generellBrevData)
            BrevProsessType.AUTOMATISK -> null
            BrevProsessType.MANUELL -> null
            BrevProsessType.OPPLASTET_PDF -> throw IllegalStateException(
                "Vedlegg payload ikke relevant for ${BrevProsessType.OPPLASTET_PDF}",
            )
        }
}

private data class OpprettBrevRequest(
    val generellBrevData: GenerellBrevData,
    val prosessType: BrevProsessType,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
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
        .copy(navn = if (mottaker.navn.isNullOrBlank()) "N/A" else mottaker.navn!!)
}
