package no.nav.etterlatte.trygdetid.regler

import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
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
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.etterlatte.trygdetid.normaliser
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.round

// TODO dato settes riktig senere
val TRYGDETID_DATO: LocalDate = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodeMedPoengaar(
    val fra: LocalDate,
    val til: LocalDate,
    val poengInnAar: Boolean,
    val poengUtAar: Boolean,
)

data class TrygdetidPeriodeGrunnlag(
    val periode: FaktumNode<TrygdetidPeriodeMedPoengaar>,
)

data class TotalTrygdetidGrunnlag(
    val beregnetTrygdetidPerioder: FaktumNode<List<Period>>,
)

/**
 * Utility data class - brukes der vi trenger å samle nasjonal og teoretisk (generisk på faktisk eller fremtidig)
 * fordi regel DSL klarer bare tre parameter per regel.
 */
data class TrygdetidPar<T>(
    val nasjonal: T,
    val teoretisk: T,
)

data class TrygdetidGrunnlagMedAvdoed(
    val trygdetidGrunnlagListe: List<TrygdetidGrunnlag>,
    val foedselsDato: LocalDate,
    val doedsDato: LocalDate,
    val norskPoengaar: Int?,
    val yrkesskade: Boolean,
    val nordiskKonvensjon: Boolean,
)

data class TrygdetidGrunnlagMedAvdoedGrunnlag(
    val trygdetidGrunnlagMedAvdoed: FaktumNode<TrygdetidGrunnlagMedAvdoed>,
)

val nordiskKonvensjon: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Hent nordisk konvensjon",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { it.nordiskKonvensjon },
    )

val dagerPrMaanedTrygdetidGrunnlag =
    definerKonstant<TrygdetidGrunnlagMedAvdoedGrunnlag, Int>(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "En måned trygdetid tilsvarer 30 dager",
        regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-DAGER-PR-MND-TRYGDETID"),
        verdi = 30,
    )

val opptjeningsDatoer: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, Pair<LocalDate, LocalDate>> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Hent foedselsdato og doedsdato",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { Pair(it.foedselsDato, it.doedsDato) },
    )

val antallPoengaarINorge: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, Int?> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Finner antall poengår i Norge for overstyring av norske TT perioder",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { it.norskPoengaar },
    )

val erYrkesskade: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, Boolean> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Er dette yrkesskade",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { it.yrkesskade },
    )

val trygdetidGrunnlagListe: Regel<TrygdetidGrunnlagMedAvdoedGrunnlag, List<TrygdetidGrunnlag>> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Grunnlag liste med poengår",
        finnFaktum = TrygdetidGrunnlagMedAvdoedGrunnlag::trygdetidGrunnlagMedAvdoed,
        finnFelt = { it.trygdetidGrunnlagListe },
    )

/**
 * Sortere alle perioder ut ifra fra dato. Normalisering krever at listen er i datorekkefølge for å kunne finne
 * forrige/neste period.
 */
val sortertTrygdetidGrunnlagListe =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Sortere grunnlag liste med poengår",
        regelReferanse = RegelReferanse(id = "REGEL-SORTERE-TRYGDETIDSPERIODER"),
    ) benytter trygdetidGrunnlagListe med {
        it.sortedBy { x -> x.periode.fra }
    }

/**
 * Tar periodene og justere for poeng inn år/ut år. Dvs endre inn år til 1.1. og ut år til 31.12. - med
 * tilhørende flytting av forrige/neste til å unngå overlappende perioder.
 */
val normalisertTrygdetidGrunnlagListe =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Normaliser grunnlag liste ift poengår",
        regelReferanse = RegelReferanse(id = "REGEL-NORMALISER-TRYGDETIDSPERIODER"),
    ) benytter sortertTrygdetidGrunnlagListe med {
        it.normaliser()
    }

/**
 * Finn alle perioder som går mot faktisk nasjonal (norsk).
 */
val trygdetidGrunnlagListeFaktiskNorge =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut perioder med faktisk trygdetid i Norge",
        regelReferanse = RegelReferanse(id = "REGEL-FINN-FAKTISK-NASJONAL-TRYGDETIDSPERIODER"),
    ) benytter normalisertTrygdetidGrunnlagListe med { trygdetidPerioder ->
        trygdetidPerioder.filter { it.erNasjonal() && it.type == TrygdetidType.FAKTISK }
    }

/**
 * Finn alle perioder som går mot faktisk teoretisk (norsk og de som er merkert for prorata).
 */
val trygdetidGrunnlagListeFaktiskNorgeOgProrata =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut Norge og prorataperioder for teoretisk trygdetid",
        regelReferanse =
            RegelReferanse(
                id = "REGEL-FINN-FAKTISK-NASJONAL-OG-PRORATA-TRYGDETIDSPERIODER",
                versjon = "2",
            ),
    ) benytter normalisertTrygdetidGrunnlagListe med { trygdetidPerioder ->
        trygdetidPerioder.filter { (it.erNasjonal() || it.prorata) && it.type == TrygdetidType.FAKTISK }
    }

/**
 * Finn første fremtidig trygdetid (skal være enten ingen eller bare en) og juster det mtp antall dager i en måned.
 */
val fremtidigTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut periode for fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-FINN-FREMTIDIG-TRYGDETIDSPERIODE"),
    ) benytter normalisertTrygdetidGrunnlagListe og dagerPrMaanedTrygdetidGrunnlag med {
        trygdetidPerioder,
        dagerPrMaaned,
        ->
        trygdetidPerioder.firstOrNull { it.type == TrygdetidType.FREMTIDIG }.let {
            listOfNotNull(it).summer(dagerPrMaaned)
        }
    }

/**
 * Finn første dato for fremtidig trygdetid
 */
val fremtidigTrygdetidFra =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Henter ut periode for fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-FINN-START-FREMTIDIG-TRYGDETIDSPERIODE"),
    ) benytter normalisertTrygdetidGrunnlagListe med { trygdetidPerioder ->
        trygdetidPerioder.singleOrNull { it.type == TrygdetidType.FREMTIDIG }?.periode?.fra
    }

/**
 * Opptjeningstid er fra 16 år frem til siste dag i den måned som er før dødsfall. Input er fødselsdato og dødsdato.
 * Hvis fremtidig trygdetid er satt, skal opptjeningstid beregnes frem til fremtidig trygdetid
 */
val finnDatoerForOpptjeningstid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Konverter foedselsdato og doedsdato eller start fremtidig trygdetid til opptjeningsdatoer",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-OPPTJENINGSDATOER", versjon = "2"),
    ) benytter opptjeningsDatoer og fremtidigTrygdetidFra med {
        opptjeningsDatoer,
        fremtidigFra,
        ->
        val opptjeningTil =
            if (fremtidigFra != null) {
                fremtidigFra.withDayOfMonth(1).minusDays(1)
            } else {
                opptjeningsDatoer.second.withDayOfMonth(1).minusDays(1)
            }
        Pair(
            opptjeningsDatoer.first.plusYears(16),
            opptjeningTil,
        )
    }

/**
 * Summer faktisk nasjonal trygdetid.
 */
val summerFaktiskNorge =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Summer alle perioder for faktisk norge",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-FAKTISK-NASJONAL-TRYGDETID"),
    ) benytter trygdetidGrunnlagListeFaktiskNorge og dagerPrMaanedTrygdetidGrunnlag med {
        trygdetidPerioder,
        dagerPrMaaned,
        ->

        trygdetidPerioder.let { perioder ->
            val summert = perioder.summer(dagerPrMaaned).oppjustertMaaneder()

            FaktiskTrygdetid(
                periode = Period.ofMonths(summert.toInt()).normalized(),
                antallMaaneder = summert,
            )
        }
    }

/**
 * Finn ut faktisk norsk tid - enten summert eller fra overstyrt poengår.
 */
val faktiskNorge =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Velg mellom beregnet faktisk nasjonal og overstyrt poengår",
        regelReferanse = RegelReferanse(id = "REGEL-FAKTISK-NASJONAL-TRYGDETID"),
    ) benytter summerFaktiskNorge og antallPoengaarINorge med { faktiskNorge, norskPoengaar ->
        if (norskPoengaar != null) {
            FaktiskTrygdetid(Period.ofYears(norskPoengaar), norskPoengaar * 12L)
        } else {
            faktiskNorge
        }
    }

/**
 * Summer faktisk teoretisk trygdetid. Tar høyde for overstyrt poengår (som erstatter alle norske perioder).
 */
val summerFaktiskTeoretisk =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Summer alle perioder for faktisk teoretisk",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-FAKTISK-TEORETISK-TRYGDETID", versjon = "2"),
    ) benytter trygdetidGrunnlagListeFaktiskNorgeOgProrata og antallPoengaarINorge og dagerPrMaanedTrygdetidGrunnlag med {
        trygdetidPerioder,
        norskPoengaar,
        dagerPrMaaned,
        ->

        val beregningsPerioder =
            if (norskPoengaar != null) {
                trygdetidPerioder.filter { !it.erNasjonal() }
            } else {
                trygdetidPerioder
            }

        val summert =
            beregningsPerioder
                .summer(dagerPrMaaned)
                .plusYears(norskPoengaar?.toLong() ?: 0)
                .oppjustertMaaneder()

        FaktiskTrygdetid(
            periode = Period.ofMonths(summert.toInt()).normalized(),
            antallMaaneder = summert,
        )
    }

/**
 * Beregn hvor langt opptjeningstid er mellom to datoer.
 */
val opptjeningsTidIMnd =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Finn ut opptjeningstid",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-OPPTJENINGSPERIODE"),
    ) benytter finnDatoerForOpptjeningstid med { datoer ->
        maxOf(
            0L,
            ChronoUnit.MONTHS.between(datoer.first, datoer.second),
        )
    }

/**
 * Finne ut om valg av nordisk konvensjon art 9 skal overstyre bruk av 4/5 regel
 */
val nordiskEllerFireFemtedeler =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Gruppere fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-NORDISK-ELLER-FIREFEMTEDELER"),
    ) benytter faktiskNorge og opptjeningsTidIMnd og nordiskKonvensjon med {
        faktisk,
        opptjening,
        nordisk,
        ->
        val mindreEnnFireFemtedelerAvOpptjeningstiden = (faktisk.antallMaaneder * 5) < opptjening * 4
        mindreEnnFireFemtedelerAvOpptjeningstiden && !nordisk
    }

/**
 * Beregn fremtidig nasjonal trygdetid justert etter opptjeningsregel om at verdi er mindre enn 4/5 opptjeningstid.
 */
val fremtidigTrygdetidForNasjonal =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Regn ut fremtidig trygdetid nasjonal",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-FREMTIDIG-NASJONAL-TRYGDETID", versjon = "1.1"),
    ) benytter nordiskEllerFireFemtedeler og fremtidigTrygdetid og opptjeningsTidIMnd med
        { nordiskEllerFireFemtedeler, fremtidig, opptjening ->
            if (fremtidig != Period.ZERO) {
                val fremtidigPeriode =
                    fremtidig.justertForOpptjeningstiden(opptjening, nordiskEllerFireFemtedeler)

                FremtidigTrygdetid(
                    periode = fremtidigPeriode,
                    antallMaaneder = fremtidigPeriode.toTotalMonths(),
                    opptjeningstidIMaaneder = opptjening,
                    mindreEnnFireFemtedelerAvOpptjeningstiden = nordiskEllerFireFemtedeler,
                )
            } else {
                FremtidigTrygdetid.ZERO
            }
        }

/**
 * Beregn fremtidig teoretisk trygdetid justert etter opptjeningsregel om at verdi er mindre enn 4/5 opptjeningstid.
 */
val fremtidigTrygdetidForTeoretisk =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Regn ut fremtidig trygdetid teoretisk",
        regelReferanse = RegelReferanse(id = "REGEL-BEREGN-FREMTIDIG-TEORETISK-TRYGDETID"),
    ) benytter summerFaktiskTeoretisk og fremtidigTrygdetid og opptjeningsTidIMnd med {
        teoretisk,
        fremtidig,
        opptjening,
        ->
        if (fremtidig != Period.ZERO) {
            val mindreEnnFireFemtedelerAvOpptjeningstiden = teoretisk.antallMaaneder * 5 < opptjening * 4

            val fremtidigPeriode =
                fremtidig.justertForOpptjeningstiden(opptjening, mindreEnnFireFemtedelerAvOpptjeningstiden)

            FremtidigTrygdetid(
                periode = fremtidigPeriode,
                antallMaaneder = fremtidigPeriode.toTotalMonths(),
                opptjeningstidIMaaneder = opptjening,
                mindreEnnFireFemtedelerAvOpptjeningstiden = mindreEnnFireFemtedelerAvOpptjeningstiden,
            )
        } else {
            FremtidigTrygdetid.ZERO
        }
    }

/**
 * Samler faktisk trygdetid til et par for å gjøre DSL bruk (maks antall parameter) lettere.
 */
val beregnetFaktiskTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Gruppere faktisk trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-FAKTISK-TRYGDETID"),
    ) benytter faktiskNorge og summerFaktiskTeoretisk med {
        nasjonal,
        teoretisk,
        ->
        TrygdetidPar(nasjonal, teoretisk)
    }

/**
 * Samler fremtidig trygdetid (kun norsk) til et par for å gjøre DSL bruk (maks antall parameter) lettere men bare
 * hvis det ikke er overstyrt poengår. Hvis det er overstyrt så erstatter den alle norske perioder og det er tatt med
 * i faktisk trygdetid allerede.
 */
val beregnetFremtidigTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Gruppere fremtidig trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-FREMTIDIG-TRYGDETID"),
    ) benytter fremtidigTrygdetidForNasjonal og fremtidigTrygdetidForTeoretisk og antallPoengaarINorge med {
        nasjonal,
        teoretisk,
        norskPoengaar,
        ->
        if (norskPoengaar == null) {
            TrygdetidPar(nasjonal, teoretisk)
        } else {
            TrygdetidPar(FremtidigTrygdetid.ZERO, FremtidigTrygdetid.ZERO)
        }
    }

/**
 * Både nasjonal og teoretisk må avrundes til nærmest hele år. Mindre enn 6 måned rundes ned. Mer eller lik 6 måned
 * rundes opp.
 */
val avrundetTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Avrunder trygdetid til nærmeste hele år basert på måneder",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-AVRUNDING"),
    ) benytter beregnetFaktiskTrygdetid og
        beregnetFremtidigTrygdetid med { faktisk, fremtidig ->

            TrygdetidPar(
                nasjonal =
                    minOf(
                        faktisk.nasjonal.periode.plus(fremtidig.nasjonal.periode).normalized().let {
                            if (it.months >= 6) {
                                it.years + 1
                            } else {
                                it.years
                            }
                        },
                        40,
                    ),
                teoretisk =
                    minOf(
                        faktisk.teoretisk.periode.plus(fremtidig.teoretisk.periode).normalized().let {
                            if (it.months >= 6) {
                                it.years + 1
                            } else {
                                it.years
                            }
                        },
                        40,
                    ),
            )
        }

/**
 * Lag proratabrøk. Ingen verdier kan overstige 480 måned (40 år).
 * Brøk av 1/1 brukes ikke - hvis verdier er det samme - så har vi ingen brøk.
 */
val avrundetBroek =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Avrunder trygdetid broek basert på måneder - maks 40 år",
        regelReferanse = RegelReferanse(id = "REGEL-AVRUNDET-PRORATA-BROEK"),
    ) benytter beregnetFaktiskTrygdetid med { faktisk ->
        if (faktisk.teoretisk.antallMaaneder.toInt() != 0 &&
            faktisk.nasjonal.antallMaaneder != faktisk.teoretisk.antallMaaneder
        ) {
            IntBroek.fra(
                Pair(
                    minOf(faktisk.nasjonal.antallMaaneder.toInt(), 480),
                    minOf(faktisk.teoretisk.antallMaaneder.toInt(), 480),
                ),
            )
        } else {
            null
        }
    }

/**
 * Utility regel - gruppere 2 verdier ned til 1 - pga maks 3 klausul i regel DSL
 */
val avrundetTrygdetidOgBroek =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Samle avrundet trygdetid og avrundet broek",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-OG-BROEK-AVRUNDING"),
    ) benytter avrundetTrygdetid og avrundetBroek med { trygdetid, broek ->
        Pair(trygdetid, broek)
    }

/**
 * Beregn detaljert trygdetid
 */
val beregnDetaljertBeregnetTrygdetid =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregn detaljert trygdetid",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-DETALJERT-TRYGDETID"),
    ) benytter beregnetFaktiskTrygdetid og
        beregnetFremtidigTrygdetid og avrundetTrygdetidOgBroek med { faktisk, fremtidig, avrundet ->

            DetaljertBeregnetTrygdetidResultat(
                faktiskTrygdetidNorge = faktisk.nasjonal,
                faktiskTrygdetidTeoretisk = faktisk.teoretisk,
                fremtidigTrygdetidNorge = fremtidig.nasjonal,
                fremtidigTrygdetidTeoretisk = fremtidig.teoretisk,
                samletTrygdetidNorge = avrundet.first.nasjonal,
                samletTrygdetidTeoretisk = avrundet.first.teoretisk,
                prorataBroek = avrundet.second,
                overstyrt = false,
                yrkesskade = false,
                beregnetSamletTrygdetidNorge = null,
            )
        }

/**
 * Inngangspunkt til hele regeltreet.
 * Etter beregning - tar høyde for at yrkesskade gir altid 40 år.
 */
val beregnDetaljertBeregnetTrygdetidMedYrkesskade =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Sjekk om detaljert beregnet trygdetid skal endres pga yrkesskade",
        regelReferanse = RegelReferanse(id = "REGEL-TOTAL-DETALJERT-TRYGDETID-YS"),
    ) benytter beregnDetaljertBeregnetTrygdetid og erYrkesskade med { trygdetid, erYrkesskade ->
        when (erYrkesskade) {
            false -> trygdetid
            true ->
                trygdetid.copy(
                    yrkesskade = true,
                    beregnetSamletTrygdetidNorge = trygdetid.samletTrygdetidNorge,
                    samletTrygdetidNorge = 40,
                )
        }
    }

// Utility functions - burde vaert regler men må kalles fra flere steder.
// Subsumsjonsverdi for regler som kalle disse skal fortsatt være korrekt.

/**
 * Summer periodene fra alle trygdetid grunnlag i listen med riktig oppjustering av dager il slutt.
 */
private fun List<TrygdetidGrunnlag>.summer(antallDagerEnMaanedTrygdetid: Int) =
    this
        .map { Period.between(it.periode.fra, it.periode.til) }
        .reduceOrNull { acc, period ->
            acc.plus(period)
        }?.let {
            val dagerResterende = it.days.mod(antallDagerEnMaanedTrygdetid)
            val maanederOppjustert = it.months + (it.days - dagerResterende).div(antallDagerEnMaanedTrygdetid)
            Period.of(it.years, maanederOppjustert, dagerResterende).normalized()
        } ?: Period.ZERO

/**
 * Juster en periode mtp regel om redusering av fremtidig trygdetid for de som har ikke nok opptjeningstid.
 */
private fun Period.justertForOpptjeningstiden(
    opptjening: Long,
    mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
) = Period
    .ofMonths(
        if (mindreEnnFireFemtedelerAvOpptjeningstiden) {
            val months = Period.ofYears(40).toTotalMonths() - round(opptjening * 0.8).toLong()

            Period.ofMonths(months.toInt()).normalized()
        } else {
            this
        }.oppjustertMaaneder().toInt(),
    ).normalized()

/**
 * Resterende dager oppjusteres til hel måned.
 */
private fun Period.oppjustertMaaneder() =
    when (this.days) {
        0 -> this.toTotalMonths()
        else -> this.toTotalMonths().plus(1)
    }
