package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

internal class BrevDatafetcherVedtak(
    private val brevdataFacade: BrevdataFacade,
    private val brukerTokenInfo: BrukerTokenInfo,
    private val behandlingId: UUID?,
    private val vedtakVirkningstidspunkt: YearMonth,
    private val type: VedtakType?,
    private val sak: Sak,
) {
    constructor(brevdataFacade: BrevdataFacade, brukerTokenInfo: BrukerTokenInfo, generellBrevData: GenerellBrevData) : this(
        brevdataFacade = brevdataFacade,
        brukerTokenInfo = brukerTokenInfo,
        behandlingId = generellBrevData.behandlingId,
        vedtakVirkningstidspunkt =
            requireNotNull(generellBrevData.forenkletVedtak?.virkningstidspunkt) {
                "brev for behandling=${generellBrevData.behandlingId} må ha virkningstidspunkt"
            },
        type = generellBrevData.forenkletVedtak?.type,
        sak = generellBrevData.sak,
    )

    suspend fun hentBrevutfall() = behandlingId?.let { brevdataFacade.hentBrevutfall(it, brukerTokenInfo) }

    suspend fun hentUtbetaling() =
        brevdataFacade.finnUtbetalingsinfo(
            behandlingId!!,
            vedtakVirkningstidspunkt,
            brukerTokenInfo,
            sak.sakType,
        )

    suspend fun hentVilkaarsvurdering() =
        brevdataFacade.hentVilkaarsvurdering(
            behandlingId!!,
            brukerTokenInfo
        )

    suspend fun hentForrigeUtbetaling() =
        brevdataFacade.finnForrigeUtbetalingsinfo(
            sak.id,
            vedtakVirkningstidspunkt,
            brukerTokenInfo,
            sak.sakType,
        )

    suspend fun hentGrunnbeloep() = brevdataFacade.hentGrunnbeloep(brukerTokenInfo)

    suspend fun hentEtterbetaling() =
        behandlingId?.let {
            brevdataFacade.hentEtterbetaling(
                it,
                brukerTokenInfo,
            )
        }

    suspend fun hentAvkortinginfo() =
        brevdataFacade.finnAvkortingsinfo(
            behandlingId!!,
            sak.sakType,
            vedtakVirkningstidspunkt,
            type!!,
            brukerTokenInfo,
        )

    suspend fun hentForrigeAvkortinginfo() =
        brevdataFacade.finnForrigeAvkortingsinfo(
            sak.id,
            sak.sakType,
            vedtakVirkningstidspunkt,
            type!!,
            brukerTokenInfo,
        )

    suspend fun hentTrygdetid() = behandlingId?.let { brevdataFacade.finnTrygdetid(it, brukerTokenInfo) }
}
