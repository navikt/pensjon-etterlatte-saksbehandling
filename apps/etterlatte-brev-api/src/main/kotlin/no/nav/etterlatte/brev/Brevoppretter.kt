package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import java.util.UUID

class Brevoppretter(
    private val adresseService: AdresseService,
    private val db: BrevRepository,
    private val brevdataFacade: BrevdataFacade,
    private val brevbaker: BrevbakerService,
    private val redigerbartVedleggHenter: RedigerbartVedleggHenter,
) {
    suspend fun opprettVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        // TODO EY-3232 - Fjerne migreringstilpasning
        brevKode: (b: BrevkodeRequest) -> EtterlatteBrevKode,
        brevDataMapper: suspend (RedigerbarTekstRequest) -> BrevData,
    ): Brev {
        require(db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            brevdataFacade.hentBehandling(behandlingId, brukerTokenInfo).status.let { status ->
                if (!status.kanEndres()) {
                    throw KanIkkeOppretteVedtaksbrev(behandlingId, status)
                }
            }
        }

        return opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
            automatiskMigreringRequest = automatiskMigreringRequest,
            brevKode = brevKode,
            brevtype = Brevtype.VEDTAK,
            brevDataMapping = brevDataMapper,
        ).first
    }

    // TODO: Trur denne heller enn EtterlatteBrevKode bør ta inn Brevkoder
    suspend fun opprettBrev(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: (b: BrevkodeRequest) -> EtterlatteBrevKode,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        brevtype: Brevtype,
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevData,
    ): Pair<Brev, GenerellBrevData> =
        with(
            hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
                automatiskMigreringRequest,
                brevDataMapping,
            ),
        ) {
            val nyttBrev =
                OpprettNyttBrev(
                    sakId = sakId,
                    behandlingId = behandlingId,
                    prosessType = BrevProsessType.REDIGERBAR,
                    soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                    mottaker = finnMottaker(generellBrevData.sak.sakType, generellBrevData.personerISak),
                    opprettet = Tidspunkt.now(),
                    innhold = innhold,
                    innholdVedlegg = innholdVedlegg,
                    brevtype = brevtype,
                )
            return Pair(db.opprettBrev(nyttBrev), generellBrevData)
        }

    suspend fun hentNyttInnhold(
        sakId: Long,
        brevId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: (b: BrevkodeRequest) -> EtterlatteBrevKode,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevData,
    ): BrevService.BrevPayload {
        with(
            hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
                automatiskMigreringRequest,
                brevDataMapping,
            ),
        ) {
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
    }

    private suspend fun hentInnData(
        sakId: Long,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: (b: BrevkodeRequest) -> EtterlatteBrevKode,
        automatiskMigreringRequest: MigreringBrevRequest? = null,
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevData,
    ): OpprettBrevRequest {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, bruker) }

        val brevkodeRequest =
            BrevkodeRequest(generellBrevData.erMigrering(), generellBrevData.sak.sakType, generellBrevData.forenkletVedtak?.type)

        val kode = brevKode(brevkodeRequest)
        val tittel = kode.tittel ?: (generellBrevData.vedtakstype()?.let { "Vedtak om $it" } ?: "Tittel mangler")
        return coroutineScope {
            val innhold =
                async {
                    brevbaker.hentRedigerbarTekstFraBrevbakeren(
                        RedigerbarTekstRequest(
                            generellBrevData,
                            bruker,
                            kode,
                            automatiskMigreringRequest,
                            brevDataMapping,
                        ),
                    )
                }

            val innholdVedlegg = async { redigerbartVedleggHenter.hentInitiellPayloadVedlegg(bruker, generellBrevData) }
            OpprettBrevRequest(
                generellBrevData,
                BrevInnhold(tittel, generellBrevData.spraak, innhold.await()),
                innholdVedlegg.await(),
            )
        }
    }

    private suspend fun finnMottaker(
        sakType: SakType,
        personerISak: PersonerISak,
    ): Mottaker =
        with(personerISak) {
            when (verge) {
                is Vergemaal -> verge.toMottaker()

                else -> {
                    val mottakerFnr =
                        innsender?.fnr?.value?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?: soeker.fnr.value
                    adresseService.hentMottakerAdresse(sakType, mottakerFnr)
                }
            }
        }
}

private data class OpprettBrevRequest(
    val generellBrevData: GenerellBrevData,
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

class KanIkkeOppretteVedtaksbrev(behandlingId: UUID, status: BehandlingStatus) : UgyldigForespoerselException(
    code = "KAN_IKKE_ENDRE_VEDTAKSBREV",
    detail = "Kan ikke opprette vedtaksbrev når behandlingen har status $status",
    meta = mapOf("behandlingId" to behandlingId),
)
