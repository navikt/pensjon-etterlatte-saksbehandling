package no.nav.etterlatte.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
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

data class VirkningstidspunktRequest(
    @JsonProperty("dato") private val _dato: String,
    val begrunnelse: String?,
    val kravdato: LocalDate? = null,
) {
    val dato: YearMonth = _dato.tilYearMonth()
}

fun String.tilYearMonth(): YearMonth {
    return try {
        Tidspunkt.parse(this).toNorskTid().let {
            YearMonth.of(it.year, it.month)
        } ?: throw IllegalArgumentException("Dato $this må være definert")
    } catch (e: Exception) {
        throw RuntimeException("Kunne ikke lese dato for virkningstidspunkt: $this", e)
    }
}

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
