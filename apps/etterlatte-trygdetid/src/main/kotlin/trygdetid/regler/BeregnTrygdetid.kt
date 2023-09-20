package no.nav.etterlatte.trygdetid.regler

import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.IntBroek
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.round

// TODO dato settes riktig senere
val TRYGDETID_DATO: LocalDate = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodMedPoengAar(
    val fra: LocalDate,
    val til: LocalDate,
    val poengInnAar: Boolean,
    val poengUtAar: Boolean,
)

data class TrygdetidPeriodeGrunnlag(
    val periode: FaktumNode<TrygdetidPeriodMedPoengAar>,
)

data class TotalTrygdetidGrunnlag(
    val beregnetTrygdetidPerioder: FaktumNode<List<Period>>,
)

val periode: Regel<TrygdetidPeriodeGrunnlag, TrygdetidPeriodMedPoengAar> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Finner trygdetidsperiode fra grunnlag",
        finnFaktum = TrygdetidPeriodeGrunnlag::periode,
        finnFelt = { it },
    )

val beregnTrygdetidForPeriode =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregner trygdetid fra og med periodeFra til og med periodeTil i år, måneder og dager",
        regelReferanse = RegelReferanse(id = "REGEL-TRYGDETID-BEREGNE-PERIODE"),
    ) benytter periode med { periode ->
        fun TrygdetidPeriodMedPoengAar.erEttPoengAar() = fra.year == til.year && (poengInnAar || poengUtAar)

        fun TrygdetidPeriodMedPoengAar.poengJustertFra() =
            if (poengInnAar) {
                fra.with(MonthDay.of(Month.JANUARY, 1))
            } else {
                fra
            }

        fun TrygdetidPeriodMedPoengAar.poengJustertTil() =
            if (poengUtAar) {
                til.with(MonthDay.of(Month.DECEMBER, 31))
            } else {
                til
            }

        if (periode.erEttPoengAar()) {
            Period.ofYears(1)
        } else {
            Period.between(periode.poengJustertFra(), periode.poengJustertTil().plusDays(1))
        }
    }

val beregnetTrygdetidPerioder: Regel<TotalTrygdetidGrunnlag, List<Period>> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregnet trygdetidsperioder",
        finnFaktum = TotalTrygdetidGrunnlag::beregnetTrygdetidPerioder,
        finnFelt = { it },
    )

val maksTrygdetid =
    definerKonstant<TotalTrygdetidGrunnlag, Int>(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Full trygdetidsopptjening er 40 år",
        regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-MAKS-ANTALL-ÅR"),
        verdi = 40,
    )

val dagerPrMaanedTrygdetid =
    definerKonstant<TotalTrygdetidGrunnlag, Int>(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "En måned trygdetid tilsvarer 30 dager",
        regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-DAGER-PR-MND-TRYGDETID"),
        verdi = 30,
    )

val totalTrygdetidYrkesskade =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Yrkesskade fører altid til 40 år",
        regelReferanse = RegelReferanse(id = "REGEL-YRKESSKADE-TRYGDETID"),
    ) benytter maksTrygdetid med { it }

val totalTrygdetidFraPerioder =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregner trygdetid fra perioder",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-SLÅ-SAMMEN-PERIODER"),
    ) benytter beregnetTrygdetidPerioder og dagerPrMaanedTrygdetid med {
            trygdetidPerioder,
            antallDagerEnMaanedTrygdetid,
        ->
        trygdetidPerioder
            .reduce { acc, period -> acc.plus(period) }
            .let {
                val dagerResterende = it.days.mod(antallDagerEnMaanedTrygdetid)
                val maanederOppjustert = it.months + (it.days - dagerResterende).div(antallDagerEnMaanedTrygdetid)
                Period.of(it.years, maanederOppjustert, dagerResterende).normalized()
            }
    }

val totalTrygdetidAvrundet =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Avrunder trygdetid til nærmeste hele år basert på måneder",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-AVRUNDING"),
    ) benytter totalTrygdetidFraPerioder med { totalTrygdetidFraPerioder ->
        if (totalTrygdetidFraPerioder.months >= 6) {
            totalTrygdetidFraPerioder.years + 1
        } else {
            totalTrygdetidFraPerioder.years
        }
    }

val beregnAntallAarTotalTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregner antall år trygdetid totalt",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID"),
    ) benytter totalTrygdetidAvrundet og maksTrygdetid med { totalTrygdetidAvrundet, maksTrygdetid ->
        minOf(totalTrygdetidAvrundet, maksTrygdetid)
    }

data class TrygdetidGrunnlagMedAvdoed(
    val trygdetidGrunnlagListe: List<TrygdetidGrunnlag>,
    val foedselsDato: LocalDate,
    val doedsDato: LocalDate,
)

data class TrygdetidGrunnlagMedAvdoedGrunnlag(
    val trygdetidGrunnlagMedAvdoed: FaktumNode<TrygdetidGrunnlagMedAvdoed>,
)

val dagerPrMaanedTrygdetidGrunnlag =
    definerKonstant<TrygdetidGrunnlagMedAvdoedGrunnlag, Int>(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "En måned trygdetid tilsvarer 30 dager",
        regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-DAGER-PR-MND-TRYGDETID-XXX"),
        verdi = 30,
    )

// IDX2
val opptjeningsDatoer: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, Pair<LocalDate, LocalDate>> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Hent foedselsdato og doedsdato",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { Pair(it.foedselsDato, it.doedsDato) },
    )

val finnDatoerForOpptjeningstid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Konverter foedselsdato og doedsdato til opptjeningsdatoer",
        regelReferanse = RegelReferanse(id = "REGEL-IDX2"),
    ) benytter opptjeningsDatoer med {
        Pair(
            it.first.plusYears(16),
            it.second.withDayOfMonth(1).minusDays(1),
        )
    }

// ID10
val trygdetidGrunnlagListe: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, List<TrygdetidGrunnlag>> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Grunnlag liste med poengår",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { it.trygdetidGrunnlagListe },
    )

val sortertTrygdetidGrunnlagListe =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Sortere grunnlag liste med poengår",
        regelReferanse = RegelReferanse(id = "REGEL-IDX4"),
    ) benytter trygdetidGrunnlagListe med {
        it.sortedBy { x -> x.periode.fra }
    }

val normalisertTrygdetidGrunnlagListe =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Normaliser grunnlag liste ift poengår",
        regelReferanse = RegelReferanse(id = "REGEL-IDX3"),
    ) benytter sortertTrygdetidGrunnlagListe med {
        it.normaliser()
    }

val trygdetidGrunnlagListeFaktiskNorge =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut perioder med faktisk trygdetid i Norge",
        regelReferanse = RegelReferanse(id = "REGEL-ID8"),
    ) benytter normalisertTrygdetidGrunnlagListe med { trygdetidPerioder ->
        trygdetidPerioder.filter { it.erNasjonal() && it.type == TrygdetidType.FAKTISK }
    }

val trygdetidGrunnlagListeFaktiskTeoretisk =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut perioder for teoretisk trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-ID9"),
    ) benytter normalisertTrygdetidGrunnlagListe med { trygdetidPerioder ->
        trygdetidPerioder.filter { (it.erNasjonal() || it.prorata) && it.type == TrygdetidType.FAKTISK }
    }

val fremtidigTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut periode for fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-ID7"),
    ) benytter normalisertTrygdetidGrunnlagListe og dagerPrMaanedTrygdetidGrunnlag med {
            trygdetidPerioder,
            dagerPrMaaned,
        ->
        trygdetidPerioder.firstOrNull { it.type == TrygdetidType.FREMTIDIG }?.let {
            listOf(it).summer(dagerPrMaaned)
        }
    }

val summerFaktiskNorge =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Summer alle perioder for faktisk norge",
        regelReferanse = RegelReferanse(id = "REGEL-ID2"),
    ) benytter trygdetidGrunnlagListeFaktiskNorge og dagerPrMaanedTrygdetidGrunnlag med {
            trygdetidPerioder,
            dagerPrMaaned,
        ->

        trygdetidPerioder.takeIf { it.isNotEmpty() }?.let { perioder ->
            val summert = perioder.summer(dagerPrMaaned).oppjustertMaaneder()

            FaktiskTrygdetid(
                periode = Period.ofMonths(summert.toInt()).normalized(),
                antallMaaneder = summert,
            )
        }
    }

val summerFaktiskTeoretisk =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Summer alle perioder for faktisk teoretisk",
        regelReferanse = RegelReferanse(id = "REGEL-ID3"),
    ) benytter trygdetidGrunnlagListeFaktiskTeoretisk og dagerPrMaanedTrygdetidGrunnlag med {
            trygdetidPerioder,
            dagerPrMaaned,
        ->

        trygdetidPerioder.takeIf { it.isNotEmpty() }?.let { perioder ->
            val summert = perioder.summer(dagerPrMaaned).oppjustertMaaneder()

            FaktiskTrygdetid(
                periode = Period.ofMonths(summert.toInt()).normalized(),
                antallMaaneder = summert,
            )
        }
    }

val opptjeningsTidIMnd =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Finn ut opptjeningstid",
        regelReferanse = RegelReferanse(id = "REGEL-IDX1"),
    ) benytter finnDatoerForOpptjeningstid med { datoer ->
        ChronoUnit.MONTHS.between(datoer.first, datoer.second)
    }

val fremtidigTrygdetidForNasjonal =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Regn ut fremtidig trygdetid nasjonal",
        regelReferanse = RegelReferanse(id = "REGEL-ID5"),
    ) benytter summerFaktiskNorge og fremtidigTrygdetid og opptjeningsTidIMnd med { faktisk, fremtidig, opptjening ->

        if (fremtidig != null) {
            val mindreEnnFireFemtedelerAvOpptjeningstiden = ((faktisk?.antallMaaneder ?: 0) / opptjening) < 0.8

            val fremtidigPeriode =
                fremtidig.justertForOpptjeningstiden(opptjening, mindreEnnFireFemtedelerAvOpptjeningstiden)

            FremtidigTrygdetid(
                periode = fremtidigPeriode,
                antallMaaneder = fremtidigPeriode.toTotalMonths(),
                opptjeningstidIMaaneder = opptjening,
                mindreEnnFireFemtedelerAvOpptjeningstiden = mindreEnnFireFemtedelerAvOpptjeningstiden,
            )
        } else {
            null
        }
    }

val fremtidigTrygdetidForTeoretisk =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Regn ut fremtidig trygdetid teoretisk",
        regelReferanse = RegelReferanse(id = "REGEL-ID6"),
    ) benytter summerFaktiskTeoretisk og fremtidigTrygdetid og opptjeningsTidIMnd med {
            teoretisk,
            fremtidig,
            opptjening,
        ->
        if (fremtidig != null) {
            val mindreEnnFireFemtedelerAvOpptjeningstiden = ((teoretisk?.antallMaaneder ?: 0) / opptjening) < 0.8

            val fremtidigPeriode =
                fremtidig.justertForOpptjeningstiden(opptjening, mindreEnnFireFemtedelerAvOpptjeningstiden)

            FremtidigTrygdetid(
                periode = fremtidigPeriode,
                antallMaaneder = fremtidigPeriode.toTotalMonths(),
                opptjeningstidIMaaneder = opptjening,
                mindreEnnFireFemtedelerAvOpptjeningstiden = mindreEnnFireFemtedelerAvOpptjeningstiden,
            )
        } else {
            null
        }
    }

val beregnetFremtidigTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Gruppere fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-ID4"),
    ) benytter fremtidigTrygdetidForNasjonal og fremtidigTrygdetidForTeoretisk med { faktisk, teoretisk ->
        Pair(faktisk, teoretisk)
    }

val beregnDetaljertBeregnetTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregn detaljert trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-ID1"),
    ) benytter summerFaktiskNorge og
        summerFaktiskTeoretisk og
        beregnetFremtidigTrygdetid med { nasjonal, teoretisk, fremtidig ->

            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge = nasjonal,
                faktiskTrygdetidTeoretisk = teoretisk,
                fremtidigTrygdetidNorge = fremtidig.first,
                fremtidigTrygdetidTeoretisk = fremtidig.second,
                samletTrygdetidNorge =
                    minOf(
                        nasjonal.verdiOrZero().plus(fremtidig.first.verdiOrZero()).normalized().years,
                        40,
                    ),
                samletTrygdetidTeoretisk =
                    minOf(
                        teoretisk.verdiOrZero().plus(fremtidig.second.verdiOrZero()).normalized().years,
                        40,
                    ),
                prorataBroek =
                    if (nasjonal?.antallMaaneder != teoretisk?.antallMaaneder) {
                        IntBroek(
                            nasjonal?.antallMaaneder?.toInt() ?: 0,
                            teoretisk?.antallMaaneder?.toInt() ?: 0,
                        )
                    } else {
                        null
                    },
                overstyrt = false,
            )
        }

// Utility functions - burde vaert regler men maa kalles fra flere steder.
// Subsumsjonsverdi for regler som kalle disse skal fortsatt være korrekt.

private fun FremtidigTrygdetid?.verdiOrZero() = this?.periode ?: Period.ZERO

private fun FaktiskTrygdetid?.verdiOrZero() = this?.periode ?: Period.ZERO

private fun List<TrygdetidGrunnlag>.normaliser() =
    this.mapIndexed { idx, trygdetidGrunnlag ->
        var fra = trygdetidGrunnlag.periode.fra
        var til = trygdetidGrunnlag.periode.til

        if (trygdetidGrunnlag.poengInnAar) {
            fra = fra.with(MonthDay.of(1, 1))
        }

        if (trygdetidGrunnlag.poengUtAar) {
            til = til.with(MonthDay.of(12, 31))
        }

        // Håndtere at den forrige var et ut år - og at dette hadde en fra i samme år
        if (idx > 0) {
            val prev = this[idx - 1]

            if (prev.poengUtAar && prev.periode.til.year == fra.year) {
                fra = LocalDate.of(prev.periode.til.year + 1, 1, 1)
            }
        }

        // Håndtere at den neste var et inn år - og at dette hadde en til i samme år
        if (idx < this.size - 2) {
            val next = this[idx + 1]

            if (next.poengInnAar && next.periode.til.year == fra.year) {
                til = LocalDate.of(next.periode.til.year - 1, 12, 31)
            }
        }

        trygdetidGrunnlag.copy(periode = TrygdetidPeriode(fra = fra, til = til.plusDays(1)))
    }

private fun List<TrygdetidGrunnlag>.summer(antallDagerEnMaanedTrygdetid: Int) =
    this.map { Period.between(it.periode.fra, it.periode.til) }
        .reduce { acc, period ->
            acc.plus(period)
        }
        .let {
            val dagerResterende = it.days.mod(antallDagerEnMaanedTrygdetid)
            val maanederOppjustert = it.months + (it.days - dagerResterende).div(antallDagerEnMaanedTrygdetid)
            Period.of(it.years, maanederOppjustert, dagerResterende).normalized()
        }

private fun Period.justertForOpptjeningstiden(
    opptjening: Long,
    mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
) = Period.ofMonths(
    if (mindreEnnFireFemtedelerAvOpptjeningstiden) {
        val months = Period.ofYears(40).toTotalMonths() - round(opptjening * 0.8).toLong()

        Period.ofMonths(months.toInt()).normalized()
    } else {
        this
    }.oppjustertMaaneder().toInt(),
).normalized()

private fun Period.oppjustertMaaneder() =
    when (this.days) {
        0 -> this.toTotalMonths()
        else -> this.toTotalMonths().plus(1)
    }
