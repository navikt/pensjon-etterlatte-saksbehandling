package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarselRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAktivitetspliktVarselUtfall
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker

object BrevDataMapperRedigerbartUtfallVarsel {
    fun hentBrevDataRedigerbar(
        sakType: SakType,
        bruker: BrukerTokenInfo,
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingsaarsak: Revurderingaarsak? = null,
    ) = when (sakType) {
        SakType.BARNEPENSJON ->
            BarnepensjonVarselRedigerbartUtfall(
                automatiskBehandla = bruker is Systembruker,
                erBosattUtlandet = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
            )

        SakType.OMSTILLINGSSTOENAD ->
            if (revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                OmstillingsstoenadAktivitetspliktVarselUtfall()
            } else {
                ManueltBrevData()
            }
    }
}
