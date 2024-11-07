package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.erForeldreloes
import no.nav.etterlatte.brev.behandling.loependeIPesys
import no.nav.etterlatte.brev.behandling.opprettAvsenderRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class InnholdTilRedigerbartBrevHenter(
    private val brevdataFacade: BrevdataFacade,
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val redigerbartVedleggHenter: RedigerbartVedleggHenter,
) {
    internal suspend fun hentInnDataForBrevMedData(
        sakId: SakId,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKode: Brevkoder,
        brevData: BrevDataRedigerbar,
    ): OpprettBrevRequest {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, null, bruker) }
        return opprettBrevRequest(generellBrevData, brevKode, brevData, bruker)
    }

    internal suspend fun hentInnData(
        sakId: SakId,
        behandlingId: UUID?,
        bruker: BrukerTokenInfo,
        brevKodeMapping: (b: BrevkodeRequest) -> Brevkoder,
        brevDataMapping: suspend (BrevDataRedigerbarRequest) -> BrevDataRedigerbar,
        overstyrSpraak: Spraak? = null,
    ): OpprettBrevRequest {
        val generellBrevData =
            retryOgPakkUt { brevdataFacade.hentGenerellBrevData(sakId, behandlingId, overstyrSpraak, bruker) }

        val brevkodeRequest =
            BrevkodeRequest(
                loependeIPesys(
                    systemkilde = generellBrevData.systemkilde,
                    behandlingId = generellBrevData.behandlingId,
                    revurderingsaarsak = generellBrevData.revurderingsaarsak,
                ),
                erForeldreloes(generellBrevData.personerISak.soeker, generellBrevData.personerISak.avdoede),
                generellBrevData.sak.sakType,
                generellBrevData.forenkletVedtak?.type,
                generellBrevData.revurderingsaarsak,
            )

        val kode = brevKodeMapping(brevkodeRequest)
        val brevDataRedigerbarRequest =
            BrevDataRedigerbarRequest(
                brukerTokenInfo = bruker,
                soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                sakType = generellBrevData.sak.sakType,
                forenkletVedtak = generellBrevData.forenkletVedtak,
                utlandstilknytningType = generellBrevData.utlandstilknytning?.type,
                revurderingsaarsak = generellBrevData.revurderingsaarsak,
                behandlingId = behandlingId,
                erForeldreloes = erForeldreloes(generellBrevData.personerISak.soeker, generellBrevData.personerISak.avdoede),
                loependeIPesys =
                    loependeIPesys(
                        generellBrevData.systemkilde,
                        generellBrevData.behandlingId,
                        generellBrevData.revurderingsaarsak,
                    ),
                systemkilde = generellBrevData.systemkilde,
                avdoede = generellBrevData.personerISak.avdoede,
            )
        val brevData: BrevDataRedigerbar = brevDataMapping(brevDataRedigerbarRequest)
        return opprettBrevRequest(generellBrevData, kode, brevData, bruker)
    }

    private suspend fun opprettBrevRequest(
        generellBrevData: GenerellBrevData,
        brevKode: Brevkoder,
        brevData: BrevData,
        bruker: BrukerTokenInfo,
    ): OpprettBrevRequest =
        coroutineScope {
            val innhold =
                async {
                    brevbaker.hentRedigerbarTekstFraBrevbakeren(
                        BrevbakerRequest.fra(
                            brevKode = brevKode.redigering,
                            brevData = brevData,
                            avsender =
                                adresseService.hentAvsender(
                                    opprettAvsenderRequest(bruker, generellBrevData.forenkletVedtak, generellBrevData.sak.enhet),
                                    bruker,
                                ),
                            soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                            sakId = generellBrevData.sak.id,
                            spraak = generellBrevData.spraak,
                            sakType = generellBrevData.sak.sakType,
                        ),
                    )
                }

            val innholdVedlegg =
                async {
                    redigerbartVedleggHenter.hentInitiellPayloadVedlegg(
                        bruker,
                        brevKode.brevtype,
                        generellBrevData.sak.sakType,
                        generellBrevData.forenkletVedtak?.type,
                        generellBrevData.behandlingId,
                        generellBrevData.revurderingsaarsak,
                        generellBrevData.personerISak.soekerOgEventuellVerge(),
                        generellBrevData.sak.id,
                        generellBrevData.forenkletVedtak,
                        generellBrevData.sak.enhet,
                        generellBrevData.spraak,
                    )
                }

            OpprettBrevRequest(
                soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                sakType = generellBrevData.sak.sakType,
                enhet = generellBrevData.sak.enhet,
                personerISak = generellBrevData.personerISak,
                innhold = BrevInnhold(brevKode.tittel, generellBrevData.spraak, innhold.await()),
                innholdVedlegg = innholdVedlegg.await(),
                brevkode = brevKode,
            )
        }
}

internal data class OpprettBrevRequest(
    val soekerFnr: String,
    val sakType: SakType,
    val enhet: Enhetsnummer,
    val personerISak: PersonerISak,
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
    val brevkode: Brevkoder,
)
