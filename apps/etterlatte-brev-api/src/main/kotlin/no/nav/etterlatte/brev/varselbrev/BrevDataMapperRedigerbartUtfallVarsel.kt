package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.model.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarselRedigerbartUtfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Systembruker

object BrevDataMapperRedigerbartUtfallVarsel {
    fun hentBrevDataRedigerbar(
        sakType: SakType,
        bruker: BrukerTokenInfo,
        utlandstilknytning: Utlandstilknytning?,
    ) = when (sakType) {
        SakType.BARNEPENSJON ->
            BarnepensjonVarselRedigerbartUtfall(
                automatiskBehandla = bruker is Systembruker,
                erBosattUtlandet = utlandstilknytning?.erBosattUtland() ?: false,
            )

        SakType.OMSTILLINGSSTOENAD -> ManueltBrevData()
    }
}
