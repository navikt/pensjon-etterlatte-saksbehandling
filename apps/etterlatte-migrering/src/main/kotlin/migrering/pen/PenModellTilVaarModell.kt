package no.nav.etterlatte.migrering.pen

import io.ktor.util.toLowerCasePreservingASCIIRules
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

val standarspraak = Spraak.NN

fun BarnepensjonGrunnlagResponse.tilVaarModell(
    hentKontaktinformasjon: (id: Folkeregisteridentifikator) -> DigitalKontaktinformasjon?,
): Pesyssak =
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
        spraak = finnSpraak(hentKontaktinformasjon),
    )

private fun BarnepensjonGrunnlagResponse.finnSpraak(
    hentKontaktinformasjon: (id: Folkeregisteridentifikator) -> DigitalKontaktinformasjon?,
): Spraak {
    val logger = LoggerFactory.getLogger(this::class.java)
    return hentKontaktinformasjon(Folkeregisteridentifikator.of(soeker))
        ?.spraak
        ?.toLowerCasePreservingASCIIRules()
        ?.let { tilVaarSpraakmodell(logger, it) }
        ?: standarspraak.also { logger.info("Fant ikke kontaktinformasjon i KRR, bruker $it som fallback.") }
}

private fun tilVaarSpraakmodell(
    logger: Logger,
    spraakFraKRR: String,
) = when (spraakFraKRR) {
    "nb" -> Spraak.NB
    "nn" -> Spraak.NN
    "en" -> Spraak.EN
    "se" -> standarspraak.also { logger.info("Fikk nordsamisk fra KRR, som vi ikke støtter, bruker $it som fallback.") }
    else -> standarspraak.also { logger.warn("Fikk $spraakFraKRR fra KRR, som vi ikke støtter, bruker $it som fallback.") }
}

private fun tilTidspunkt(dato: LocalDate) = Tidspunkt.ofNorskTidssone(dato, LocalTime.NOON)
