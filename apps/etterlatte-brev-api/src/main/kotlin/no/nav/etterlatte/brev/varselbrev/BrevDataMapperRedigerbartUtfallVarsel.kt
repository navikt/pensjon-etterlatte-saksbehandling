package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.ManueltBrevData
import no.nav.etterlatte.brev.model.bp.BarnepensjonVarselRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadAktivitetspliktVarselUtfall
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.time.YearMonth

object BrevDataMapperRedigerbartUtfallVarsel {
    fun hentBrevDataRedigerbar(
        sakType: SakType,
        bruker: BrukerTokenInfo,
        utlandstilknytningType: UtlandstilknytningType?,
        revurderingsaarsak: Revurderingaarsak? = null,
        grunnlag: Grunnlag? = null,
        detaljertBehandling: DetaljertBehandling? = null,
    ) = when (sakType) {
        SakType.BARNEPENSJON ->
            BarnepensjonVarselRedigerbartUtfall(
                automatiskBehandla = bruker is Systembruker,
                erBosattUtlandet = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
            )

        SakType.OMSTILLINGSSTOENAD ->
            if (revurderingsaarsak == Revurderingaarsak.AKTIVITETSPLIKT) {
                val erTidligereFamiliepleier = detaljertBehandling?.tidligereFamiliepleier?.svar == true

                val doedsdatoEllerOpphoertPleieforhold =
                    if (erTidligereFamiliepleier) {
                        detaljertBehandling?.tidligereFamiliepleier!!.opphoertPleieforhold!!
                    } else {
                        grunnlag
                            ?.hentAvdoede()
                            ?.singleOrNull()
                            ?.hentDoedsdato()
                            ?.verdi
                    }

                val virk = detaljertBehandling!!.virkningstidspunkt?.dato
                val er12mndvarsel = virk!!.isAfter(YearMonth.from(doedsdatoEllerOpphoertPleieforhold!!.plusMonths(12)))

                OmstillingsstoenadAktivitetspliktVarselUtfall(er12mndvarsel)
            } else {
                ManueltBrevData()
            }
    }
}
