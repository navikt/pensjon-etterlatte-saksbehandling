package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth

enum class MigreringKjoringVariant {
    FULL_KJORING,
    MED_PAUSE,
    FORTSETT_ETTER_PAUSE,
}

data class MigreringRequest(
    val pesysId: PesysId,
    val enhet: Enhet,
    val soeker: Folkeregisteridentifikator,
    val gjenlevendeForelder: Folkeregisteridentifikator?,
    val avdoedForelder: List<AvdoedForelder>,
    val dodAvYrkesskade: Boolean,
    val foersteVirkningstidspunkt: YearMonth,
    val beregning: Beregning,
    val trygdetid: Trygdetid,
    val flyktningStatus: Boolean = false,
    val spraak: Spraak,
    val utlandstilknytningType: UtlandstilknytningType? = null,
) {
    fun opprettPersongalleri() =
        Persongalleri(
            soeker = this.soeker.value,
            avdoed = this.avdoedForelder.map { it.ident.value },
            gjenlevende = listOfNotNull(this.gjenlevendeForelder?.value),
            innsender = Vedtaksloesning.PESYS.name,
        )

    fun erFolketrygdberegnet(): Boolean {
        val beregningsMetode = beregning.meta?.beregningsMetodeType
        return beregningsMetode == "FOLKETRYGD" && beregning.prorataBroek == null
    }

    fun erEoesBeregnet(): Boolean {
        val beregningsMetode = beregning.meta?.beregningsMetodeType
        return beregningsMetode == "EOS" && beregning.prorataBroek != null
    }

    fun harMindreEnn40AarsTrygdetid(): Boolean {
        return beregning.anvendtTrygdetid < 40
    }

    fun anvendtFlyktningerfordel(): Boolean {
        return flyktningStatus &&
            foersteVirkningstidspunkt < YearMonth.of(2021, 1) &&
            beregning.anvendtTrygdetid == 40
    }
}

data class AvdoedForelder(
    val ident: Folkeregisteridentifikator,
    val doedsdato: Tidspunkt,
)

data class PesysId(val id: Long)

data class Enhet(val nr: String)

data class Beregning(
    val brutto: Int,
    val netto: Int,
    val anvendtTrygdetid: Int,
    val datoVirkFom: Tidspunkt,
    val g: Int,
    val prorataBroek: IntBroek?,
    val meta: BeregningMeta? = null,
)

data class Trygdetid(
    val perioder: List<Trygdetidsgrunnlag>,
)

data class BeregningMeta(
    val resultatType: String,
    val beregningsMetodeType: String,
    val resultatKilde: String,
    val kravVelgType: String,
)

data class Trygdetidsgrunnlag(
    val trygdetidGrunnlagId: Long,
    val personGrunnlagId: Long,
    val landTreBokstaver: String,
    val datoFom: Tidspunkt,
    val datoTom: Tidspunkt,
    val poengIInnAar: Boolean,
    val poengIUtAar: Boolean,
    val ikkeIProrata: Boolean,
)
