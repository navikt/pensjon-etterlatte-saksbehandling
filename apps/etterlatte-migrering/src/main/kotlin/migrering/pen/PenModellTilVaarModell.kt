package migrering.pen

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.BeregningMeta
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.*

fun BarnepensjonGrunnlagResponse.tilVaarModell(): Pesyssak {
    val pesyssak = Pesyssak(
        id = UUID.randomUUID(),
        pesysId = PesysId(sakId.toString()),
        enhet = Enhet(enhet),
        soeker = Folkeregisteridentifikator.of(soeker),
        gjenlevendeForelder = gjenlevendeForelder?.let { Folkeregisteridentifikator.of(it) },
        avdoedForelder = avdoedForeldre.map {
            AvdoedForelder(
                ident = Folkeregisteridentifikator.of(it.ident),
                doedsdato = tilTidspunkt(it.doedsdato) // TODO tidspunkt ordentleg
            )
        },
        virkningstidspunkt = YearMonth.from(virkningsdato),
        foersteVirkningstidspunkt = YearMonth.from(virkningsdato), // TODO feil?
        beregning = Beregning(
            brutto = beregning.brutto.toBigDecimal(),
            netto = beregning.netto.toBigDecimal(),
            anvendtTrygdetid = beregning.anvendtTrygdetid.toBigDecimal(),
            datoVirkFom = tilTidspunkt(beregning.datoVirkFom),
            g = beregning.g.toBigDecimal(),
            meta = with(beregning.meta) {
                BeregningMeta(
                    resultatType = resultatType,
                    beregningsMetodeType = beregningsMetodeType ?: "Ukjent",
                    resultatKilde = resultatKilde,
                    kravVelgType = kravVelgType
                )
            }
        ),
        trygdetid = Trygdetid(
            listOf() // TODO: Parse ordentleg
        ),
        flyktningStatus = false // TODO
    )
    return pesyssak
}

private fun tilTidspunkt(dato: LocalDate) = Tidspunkt(dato.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC))