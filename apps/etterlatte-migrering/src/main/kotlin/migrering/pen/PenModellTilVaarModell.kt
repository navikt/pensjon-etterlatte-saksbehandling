package no.nav.etterlatte.migrering.pen

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.BeregningMeta
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

fun BarnepensjonGrunnlagResponse.tilVaarModell(): Pesyssak {
    val pesyssak = Pesyssak(
        id = sakId,
        enhet = Enhet(enhet),
        soeker = Folkeregisteridentifikator.of(soeker),
        gjenlevendeForelder = gjenlevendeForelder?.let { Folkeregisteridentifikator.of(it) },
        avdoedForelder = avdoedForeldre.map {
            AvdoedForelder(
                ident = Folkeregisteridentifikator.of(it.ident),
                doedsdato = tilTidspunkt(it.doedsdato)
            )
        },
        virkningstidspunkt = YearMonth.from(virkningsdato),
        foersteVirkningstidspunkt = YearMonth.from(virkningsdato), // TODO er dette feil?
        beregning = with(beregning) {
            Beregning(
                brutto = brutto.toBigDecimal(),
                netto = netto.toBigDecimal(),
                anvendtTrygdetid = anvendtTrygdetid.toBigDecimal(),
                datoVirkFom = tilTidspunkt(datoVirkFom),
                g = g.toBigDecimal(),
                meta = with(meta) {
                    BeregningMeta(
                        resultatType = resultatType,
                        beregningsMetodeType = beregningsMetodeType ?: "Ukjent",
                        resultatKilde = resultatKilde,
                        kravVelgType = kravVelgType
                    )
                }
            )
        },
        trygdetid = Trygdetid(
            listOf() // TODO: Parse ordentleg
        ),
        flyktningStatus = false // TODO
    )
    return pesyssak
}

private fun tilTidspunkt(dato: LocalDate) = Tidspunkt.ofNorskTidssone(dato, LocalTime.NOON)