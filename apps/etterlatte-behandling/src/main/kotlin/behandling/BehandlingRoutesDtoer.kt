package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDate
import java.time.YearMonth

data class BoddEllerArbeidetUtlandetRequest(
    val boddEllerArbeidetUtlandet: Boolean,
    val begrunnelse: String,
    val boddArbeidetIkkeEosEllerAvtaleland: Boolean? = false,
    val boddArbeidetEosNordiskKonvensjon: Boolean? = false,
    val boddArbeidetAvtaleland: Boolean? = false,
    val vurdereAvoededsTrygdeavtale: Boolean? = false,
    val skalSendeKravpakke: Boolean? = false,
)

internal data class FastsettVirkningstidspunktResponse(
    val dato: YearMonth,
    val kilde: Grunnlagsopplysning.Saksbehandler,
    val begrunnelse: String,
    val kravdato: LocalDate?,
) {
    companion object {
        fun from(virkningstidspunkt: Virkningstidspunkt) =
            FastsettVirkningstidspunktResponse(
                virkningstidspunkt.dato,
                virkningstidspunkt.kilde,
                virkningstidspunkt.begrunnelse,
                virkningstidspunkt.kravdato,
            )
    }
}
