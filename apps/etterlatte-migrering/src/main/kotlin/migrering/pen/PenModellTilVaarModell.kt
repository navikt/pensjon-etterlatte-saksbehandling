package no.nav.etterlatte.migrering.pen

import io.ktor.util.toUpperCasePreservingASCIIRules
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.migrering.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.BeregningMeta
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

fun BarnepensjonGrunnlagResponse.tilVaarModell(
    hentKontaktinformasjon: (id: Folkeregisteridentifikator) -> DigitalKontaktinformasjon?,
): Pesyssak {
    val pesyssak =
        Pesyssak(
            id = sakId,
            enhet = Enhet(enhet),
            soeker = Folkeregisteridentifikator.of(soeker),
            gjenlevendeForelder = gjenlevendeForelder?.let { Folkeregisteridentifikator.of(it) },
            avdoedForelder =
                avdoedForeldre.map {
                    AvdoedForelder(
                        ident = Folkeregisteridentifikator.of(it.ident),
                        doedsdato = tilTidspunkt(it.doedsdato),
                    )
                },
            virkningstidspunkt = YearMonth.from(virkningsdato),
            beregning =
                with(beregning) {
                    Beregning(
                        brutto = brutto,
                        netto = netto,
                        anvendtTrygdetid = anvendtTrygdetid,
                        datoVirkFom = tilTidspunkt(datoVirkFom),
                        g = g,
                        prorataBroek = proRataBrok?.let { IntBroek(it.teller, it.nevner) },
                        meta =
                            with(meta) {
                                BeregningMeta(
                                    resultatType = resultatType,
                                    beregningsMetodeType = beregningsMetodeType ?: "Ukjent",
                                    resultatKilde = resultatKilde,
                                    kravVelgType = kravVelgType,
                                )
                            },
                    )
                },
            trygdetid =
                Trygdetid(
                    perioder =
                        trygdetidsgrunnlagListe.map {
                            Trygdetidsgrunnlag(
                                trygdetidGrunnlagId = it.trygdetidGrunnlagId,
                                personGrunnlagId = it.personGrunnlagId,
                                landTreBokstaver = it.landTreBokstaver,
                                datoFom = tilTidspunkt(it.datoFom),
                                datoTom = tilTidspunkt(it.datoTom),
                                poengIInnAar = it.poengIInnAar,
                                poengIUtAar = it.poengIUtAar,
                                ikkeIProrata = it.ikkeIProrata,
                            )
                        },
                ),
            flyktningStatus = false, // TODO - Støttes ikke av Gjenny
            spraak =
                hentKontaktinformasjon(Folkeregisteridentifikator.of(soeker))
                    ?.spraak
                    ?.toUpperCasePreservingASCIIRules()
                    ?.let { Spraak.valueOf(it) }
                    ?: Spraak.NN,
        )
    return pesyssak
}

private fun tilTidspunkt(dato: LocalDate) = Tidspunkt.ofNorskTidssone(dato, LocalTime.NOON)
