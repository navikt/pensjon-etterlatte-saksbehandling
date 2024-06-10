package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.RedigerbarTekstRequest
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
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
        brevKode: (b: BrevkodeRequest) -> EtterlatteBrevKode,
        brevDataMapper: suspend (RedigerbarTekstRequest) -> BrevDataRedigerbar,
    ): Brev {
        require(db.hentBrevForBehandling(behandlingId, Brevtype.VEDTAK).firstOrNull() == null) {
            "Vedtaksbrev finnes allerede på behandling (id=$behandlingId) og kan ikke opprettes på nytt"
        }

        if (brukerTokenInfo is Saksbehandler) {
            val kanRedigeres = brevdataFacade.hentVedtaksbehandlingKanRedigeres(behandlingId, brukerTokenInfo)
            if (!kanRedigeres) {
                throw KanIkkeOppretteVedtaksbrev(behandlingId)
            }
        }

        return opprettBrev(
            sakId = sakId,
            behandlingId = behandlingId,
            bruker = brukerTokenInfo,
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
        brevtype: Brevtype,
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevDataRedigerbar,
    ): Pair<Brev, GenerellBrevData> =
        with(
            hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
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
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevDataRedigerbar,
    ): BrevService.BrevPayload {
        val spraak = db.hentBrevInnhold(brevId)?.spraak

        with(
            hentInnData(
                sakId,
                behandlingId,
                bruker,
                brevKode,
                brevDataMapping,
                spraak,
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
        brevDataMapping: suspend (RedigerbarTekstRequest) -> BrevDataRedigerbar,
        overstyrSpraak: Spraak? = null,
    ): OpprettBrevRequest {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, overstyrSpraak, bruker) }

        val brevkodeRequest =
            BrevkodeRequest(
                generellBrevData.loependeIPesys(),
                generellBrevData.erForeldreloes(),
                generellBrevData.sak.sakType,
                generellBrevData.forenkletVedtak?.type,
            )

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
                            brevDataMapping,
                        ),
                    )
                }

            val innholdVedlegg = async { redigerbartVedleggHenter.hentInitiellPayloadVedlegg(bruker, generellBrevData, kode.brevtype) }

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
            val mottakerFnr: String =
                when (verge) {
                    is Vergemaal -> verge.foedselsnummer.value

                    else ->
                        innsender?.fnr?.value?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?: soeker.fnr.value
                }
            adresseService.hentMottakerAdresse(sakType, mottakerFnr)
        }
}

private data class OpprettBrevRequest(
    val generellBrevData: GenerellBrevData,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
)

class KanIkkeOppretteVedtaksbrev(behandlingId: UUID) : UgyldigForespoerselException(
    code = "KAN_IKKE_ENDRE_VEDTAKSBREV",
    detail = "Statusen til behandlingen tillater ikke at det opprettes vedtaksbrev",
    meta = mapOf("behandlingId" to behandlingId),
)
