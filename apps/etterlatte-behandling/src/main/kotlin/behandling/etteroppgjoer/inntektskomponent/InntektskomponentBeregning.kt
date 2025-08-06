package no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import no.nav.etterlatte.libs.regler.eksekver
import java.math.BigDecimal
import java.time.YearMonth

object InntektskomponentBeregning {
    fun beregnInntekt(
        inntektskomponentData: InntektBulkResponsDto,
        etteroppgjoersAar: Int,
    ): SubsumsjonsNode<InntektSummert> {
        val grunnlag =
            KonstantGrunnlag(
                InntektGrunnlag(
                    inntekt =
                        FaktumNode(
                            inntektskomponentData,
                            kilde = "Inntektskomponenten",
                            beskrivelse = "Data for $etteroppgjoersAar med filter ${inntektskomponentData.filter}",
                        ),
                    aar =
                        FaktumNode(
                            etteroppgjoersAar,
                            kilde = "Etteroppgjørsår",
                            beskrivelse = "Året vi henter etteroppgjørsdata for",
                        ),
                ),
            )
        val resultat =
            summerInntekter.eksekver(
                grunnlag = grunnlag,
                periode =
                    RegelPeriode(
                        fraDato = YearMonth.of(etteroppgjoersAar, 1).atDay(1),
                        tilDato = null,
                    ),
            )

        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> resultat.periodiserteResultater.single().resultat
            else -> throw InternfeilException("a")
        }
    }
}

data class Inntekter(
    val afp: InntektSummert,
    val loenn: InntektSummert,
    val oms: InntektSummert,
    val regelresultat: Map<InntektskomponentenFilter, JsonNode>? = null,
) {
    init {
        // alle inntekter skal finnes, og alle inntekter skal være for samme år
        val alleAfpMaaneder = afp.inntekter.map { it.maaned }.toSet()
        val alleLoennMaaneder = loenn.inntekter.map { it.maaned }.toSet()
        val alleOmsMaaneder = oms.inntekter.map { it.maaned }.toSet()

        krev(afp.inntekter.size == 12 && loenn.inntekter.size == 12 && oms.inntekter.size == 12) {
            "Har ikke nøyaktig 12 måneder med inntekter"
        }
        krev(alleOmsMaaneder == alleAfpMaaneder) {
            "Har forskjellige måneder med inntekt mellom OMS-filter og AFP-filter"
        }
        krev(alleLoennMaaneder == alleOmsMaaneder) {
            "Har forskjellige måneder med inntekt mellom lønn-filter og OMS-filter"
        }
        krev(alleOmsMaaneder.size == 12) {
            "Har ikke 12 unike måneder med inntekter"
        }
        krev(alleOmsMaaneder.all { it.year == alleOmsMaaneder.first().year }) {
            "Har inntekter som går over forskjellige år: ${alleOmsMaaneder.map { it.year }.toSet()}"
        }
    }
}

data class InntektSummert(
    val filter: String,
    val inntekter: List<Inntektsmaaned>,
)

data class Inntektsmaaned(
    val maaned: YearMonth,
    val beloep: BigDecimal,
)
