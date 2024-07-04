package no.nav.etterlatte.brev

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.avsender
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevkodeRequest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class DatainnhenterForBrevoppretting(
    private val brevdataFacade: BrevdataFacade,
    private val brevbaker: BrevbakerService,
    private val adresseService: AdresseService,
    private val redigerbartVedleggHenter: RedigerbartVedleggHenter,
) {
    internal suspend fun hentInnData(
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
            val redigerbarTekstRequest =
                RedigerbarTekstRequest(
                    brukerTokenInfo = bruker,
                    soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                    sakType = generellBrevData.sak.sakType,
                    forenkletVedtak = generellBrevData.forenkletVedtak,
                    utlandstilknytningType = generellBrevData.utlandstilknytning?.type,
                    revurderingaarsak = generellBrevData.revurderingsaarsak,
                    behandlingId = behandlingId,
                    erForeldreloes = generellBrevData.erForeldreloes(),
                    loependeIPesys = generellBrevData.loependeIPesys(),
                    systemkilde = generellBrevData.systemkilde,
                    avdoede = generellBrevData.personerISak.avdoede,
                )
            val brevData: BrevDataRedigerbar = brevDataMapping(redigerbarTekstRequest)
            val innhold =
                async {
                    brevbaker.hentRedigerbarTekstFraBrevbakeren(
                        BrevbakerRequest.fra(
                            brevKode = kode,
                            brevData = brevData,
                            avsender =
                                adresseService.hentAvsender(
                                    avsender(bruker, generellBrevData.forenkletVedtak, generellBrevData.sak.enhet),
                                ),
                            soekerOgEventuellVerge = generellBrevData.personerISak.soekerOgEventuellVerge(),
                            sakId = sakId,
                            spraak = generellBrevData.spraak,
                            sakType = generellBrevData.sak.sakType,
                        ),
                    )
                }

            val innholdVedlegg =
                async {
                    redigerbartVedleggHenter.hentInitiellPayloadVedlegg(
                        bruker,
                        kode.brevtype,
                        generellBrevData.sak.sakType,
                        generellBrevData.behandlingId,
                        generellBrevData.revurderingsaarsak,
                        generellBrevData.forenkletVedtak?.type,
                        generellBrevData.sak.id,
                        generellBrevData.spraak,
                        generellBrevData.forenkletVedtak,
                        generellBrevData.sak.enhet,
                        generellBrevData.personerISak.soekerOgEventuellVerge(),
                    )
                }

            OpprettBrevRequest(
                innhold = BrevInnhold(tittel, generellBrevData.spraak, innhold.await()),
                innholdVedlegg = innholdVedlegg.await(),
                soekerFnr = generellBrevData.personerISak.soeker.fnr.value,
                sakType = generellBrevData.sak.sakType,
                enhet = generellBrevData.sak.enhet,
                personerISak = generellBrevData.personerISak,
            )
        }
    }
}

internal data class OpprettBrevRequest(
    val innhold: BrevInnhold,
    val innholdVedlegg: List<BrevInnholdVedlegg>?,
    val soekerFnr: String,
    val sakType: SakType,
    val enhet: String,
    val personerISak: PersonerISak,
)
