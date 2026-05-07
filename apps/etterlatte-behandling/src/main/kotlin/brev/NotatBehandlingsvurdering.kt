package no.nav.etterlatte.brev

import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.brev.Slate.Element
import no.nav.etterlatte.brev.Slate.ElementType
import no.nav.etterlatte.brev.Slate.InnerElement
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

data class AvdoedPersonopplysninger(
    val fnr: String,
    val navn: String,
    val foedselsdato: LocalDate?,
    val doedsdato: LocalDate?,
    val statsborgerskap: String?,
    val bostedsadresser: List<Adresse>,
    val sivilstand: List<Sivilstand>,
    val utland: Utland?,
    val avdoedesBarn: List<AvdoedBarnOpplysning>,
)

data class AvdoedBarnOpplysning(
    val navn: String,
    val foedselsdato: LocalDate?,
    val doedsdato: LocalDate?,
)

fun Person.tilAvdoedPersonopplysninger() =
    AvdoedPersonopplysninger(
        fnr = foedselsnummer.value,
        navn = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" "),
        foedselsdato = foedselsdato,
        doedsdato = doedsdato,
        statsborgerskap = statsborgerskap,
        bostedsadresser = bostedsadresse ?: emptyList(),
        sivilstand = sivilstand ?: emptyList(),
        utland = utland,
        avdoedesBarn =
            avdoedesBarn?.map { barn ->
                AvdoedBarnOpplysning(
                    navn = listOfNotNull(barn.fornavn, barn.mellomnavn, barn.etternavn).joinToString(" "),
                    foedselsdato = barn.foedselsdato,
                    doedsdato = barn.doedsdato,
                )
            } ?: emptyList(),
    )

fun byggBehandlingsvurderingSlate(
    sakId: SakId,
    vedtakId: Long,
    vedtakType: VedtakType,
    datoAttestert: LocalDate?,
    avdoede: List<AvdoedPersonopplysninger>,
    trygdetider: List<TrygdetidDto>,
    trygdeavtale: Trygdeavtale?,
    beregningsgrunnlag: BeregningsGrunnlag?,
    aktivitetsplikt: AktivitetspliktDto?,
): Slate =
    Slate(
        buildList {
            add(overskrift("Behandlingsvurdering"))
            add(avsnitt("SakId: $sakId"))
            add(avsnitt("VedtakId: $vedtakId"))
            add(avsnitt("Vedtaktype: ${vedtakType.tilLesbarString()}"))
            if (datoAttestert != null) {
                add(avsnitt("Attestert: ${datoAttestert.format(DATO_FORMAT)}"))
            }

            for (avdoed in avdoede) {
                addAll(avdoedeSeksjon(avdoed))
            }

            addAll(beregningsmetodeSeksjon(beregningsgrunnlag, avdoede, trygdetider))

            if (trygdeavtale != null) {
                addAll(trygdeavtaleSeksjon(trygdeavtale))
            }

            for (trygdetid in trygdetider) {
                val avdoed = avdoede.find { it.fnr == trygdetid.ident }
                addAll(trygdetidSeksjon(trygdetid, avdoed))
            }

            if (aktivitetsplikt != null) {
                addAll(aktivitetspliktSeksjon(aktivitetsplikt))
            }
        },
    )

private fun avdoedeSeksjon(avdoed: AvdoedPersonopplysninger): List<Element> =
    buildList {
        val doedsdatoTekst = avdoed.doedsdato?.let { " – død ${dato(it)}" } ?: ""
        add(underoverskrift("Avdød: ${avdoed.navn}$doedsdatoTekst"))

        avdoed.foedselsdato?.let { add(avsnitt("Født: ${dato(it)}")) }
        avdoed.statsborgerskap?.let { add(avsnitt("Statsborgerskap: $it")) }

        val gjeldendeSivilstand = avdoed.sivilstand.filter { !it.historisk }
        val historiskeSivilstand = avdoed.sivilstand.filter { it.historisk }
        if (gjeldendeSivilstand.isNotEmpty() || historiskeSivilstand.isNotEmpty()) {
            add(avsnitt("Sivilstand:"))
            add(
                liste(
                    gjeldendeSivilstand.map { formaterSivilstand(it) } +
                        historiskeSivilstand.map { formaterSivilstand(it) + " (historisk)" },
                ),
            )
        }

        val aktiveAdresser = avdoed.bostedsadresser.filter { it.aktiv }
        val historiskeAdresser = avdoed.bostedsadresser.filter { !it.aktiv }
        if (aktiveAdresser.isNotEmpty() || historiskeAdresser.isNotEmpty()) {
            add(avsnitt("Bostedsadresser:"))
            add(
                liste(
                    aktiveAdresser.map { formaterAdresse(it) } +
                        historiskeAdresser.map { formaterAdresse(it) + " (historisk)" },
                ),
            )
        }

        avdoed.utland?.innflyttingTilNorge?.takeIf { it.isNotEmpty() }?.let { innflyttinger ->
            add(avsnitt("Innflytting til Norge:"))
            add(
                liste(
                    innflyttinger.map { innfl ->
                        val fra = innfl.fraflyttingsland ?: "ukjent land"
                        val datoTekst = innfl.dato?.let { " (${dato(it)})" } ?: ""
                        "Fra $fra$datoTekst"
                    },
                ),
            )
        }

        avdoed.utland?.utflyttingFraNorge?.takeIf { it.isNotEmpty() }?.let { utflyttinger ->
            add(avsnitt("Utflytting fra Norge:"))
            add(
                liste(
                    utflyttinger.map { utfl ->
                        val til = utfl.tilflyttingsland ?: "ukjent land"
                        val datoTekst = utfl.dato?.let { " (${dato(it)})" } ?: ""
                        "Til $til$datoTekst"
                    },
                ),
            )
        }

        if (avdoed.avdoedesBarn.isNotEmpty()) {
            add(avsnitt("Barn (${avdoed.avdoedesBarn.size}):"))
            add(
                liste(
                    avdoed.avdoedesBarn.map { barn ->
                        val foedtTekst = barn.foedselsdato?.let { " (født ${dato(it)})" } ?: ""
                        val doedTekst = barn.doedsdato?.let { ", død ${dato(it)}" } ?: ""
                        "${barn.navn}$foedtTekst$doedTekst"
                    },
                ),
            )
        }
    }

private fun beregningsmetodeSeksjon(
    beregningsgrunnlag: BeregningsGrunnlag?,
    avdoede: List<AvdoedPersonopplysninger>,
    trygdetider: List<TrygdetidDto>,
): List<Element> =
    buildList {
        add(underoverskrift("Beregningsmetode"))

        if (beregningsgrunnlag == null) {
            add(avsnitt("Beregningsgrunnlag mangler"))
            return@buildList
        }

        if (beregningsgrunnlag.beregningsMetodeFlereAvdoede.isNotEmpty()) {
            for (metodePeriode in beregningsgrunnlag.beregningsMetodeFlereAvdoede) {
                val metodeData = metodePeriode.data
                val navn = avdoede.find { it.fnr == metodeData.avdoed }?.navn ?: metodeData.avdoed
                add(
                    avsnitt(
                        "Avdød $navn: ${formaterMetode(metodeData.beregningsMetode.beregningsMetode)}" +
                            periode(metodePeriode.fom, metodePeriode.tom),
                    ),
                )
                metodeData.beregningsMetode.begrunnelse?.let { add(avsnitt("Begrunnelse: $it")) }
            }
        } else {
            val metode = beregningsgrunnlag.beregningsMetode
            add(avsnitt("Metode: ${formaterMetode(metode.beregningsMetode)}"))
            metode.begrunnelse?.let { add(avsnitt("Begrunnelse: $it")) }
        }

        if (trygdetider.any { it.beregnetTrygdetid?.resultat?.yrkesskade == true }) {
            add(avsnitt("Yrkesskade: Ja"))
        }
    }

private fun trygdetidSeksjon(
    trygdetid: TrygdetidDto,
    avdoed: AvdoedPersonopplysninger?,
): List<Element> =
    buildList {
        val overskriftTekst = avdoed?.navn ?: trygdetid.ident
        add(underoverskrift("Trygdetid – $overskriftTekst"))

        trygdetid.begrunnelse?.let { add(avsnitt("Begrunnelse: $it")) }

        trygdetid.overstyrtNorskPoengaar?.let {
            add(avsnitt("Overstyrt norsk poengår: $it"))
        }

        if (trygdetid.trygdetidGrunnlag.isNotEmpty()) {
            add(underoverskrift("Perioder"))
            add(
                liste(
                    trygdetid.trygdetidGrunnlag.map { grunnlag ->
                        val beregnetTekst =
                            grunnlag.beregnet
                                ?.let { "${it.aar} år ${it.maaneder} mnd ${it.dager} dager" }
                                ?: "ikke beregnet"

                        val flagg =
                            listOfNotNull(
                                "poeng inn".takeIf { grunnlag.poengInnAar },
                                "poeng ut".takeIf { grunnlag.poengUtAar },
                                "prorata".takeIf { grunnlag.prorata },
                            )

                        val flaggTekst = if (flagg.isNotEmpty()) " [${flagg.joinToString(", ")}]" else ""

                        "${grunnlag.type} – ${grunnlag.bosted}: " +
                            "${dato(grunnlag.periodeFra)} – ${dato(grunnlag.periodeTil)}" +
                            " → $beregnetTekst$flaggTekst" +
                            grunnlag.begrunnelse?.let { " | $it" }.orEmpty()
                    },
                ),
            )
        }

        val resultat = trygdetid.beregnetTrygdetid?.resultat
        if (resultat != null) {
            add(underoverskrift("Beregnet trygdetid"))

            val linjer =
                buildList {
                    resultat.faktiskTrygdetidNorge?.let {
                        add("Faktisk norsk: ${it.avrundet().periode.years} år ${it.avrundet().periode.months} mnd")
                    }
                    resultat.faktiskTrygdetidTeoretisk?.let {
                        add("Faktisk teoretisk: ${it.avrundet().periode.years} år ${it.avrundet().periode.months} mnd")
                    }
                    resultat.fremtidigTrygdetidNorge?.let {
                        add("Fremtidig norsk: ${it.periode.years} år ${it.periode.months} mnd")
                    }
                    resultat.fremtidigTrygdetidTeoretisk?.let {
                        add("Fremtidig teoretisk: ${it.periode.years} år ${it.periode.months} mnd")
                    }
                    resultat.samletTrygdetidNorge?.let { add("Samlet norsk: $it år") }
                    resultat.samletTrygdetidTeoretisk?.let { add("Samlet teoretisk: $it år") }
                    resultat.beregnetSamletTrygdetidNorge?.let { add("Beregnet samlet norsk: $it år") }
                    resultat.prorataBroek?.let { add("Prorata-brøk: ${it.teller}/${it.nevner}") }
                    if (resultat.overstyrt) add("Overstyrt: Ja")
                    resultat.overstyrtBegrunnelse?.let { add("Overstyrings-begrunnelse: $it") }
                }

            if (linjer.isNotEmpty()) add(liste(linjer))
        }
    }

private fun trygdeavtaleSeksjon(trygdeavtale: Trygdeavtale): List<Element> =
    buildList {
        add(underoverskrift("Trygdeavtale"))

        val linjer =
            buildList {
                add("Avtale: ${trygdeavtale.avtaleKode}")
                trygdeavtale.avtaleDatoKode?.let { add("Avtaledato: $it") }
                trygdeavtale.avtaleKriteriaKode?.let { add("Kriterium: $it") }
                trygdeavtale.personKrets?.let { add("Personkrets: ${jaNei(it)}") }
                trygdeavtale.arbInntekt1G?.let {
                    add("Arbeidsinntekt minst 1G: ${jaNei(it)}")
                }
                trygdeavtale.arbInntekt1GKommentar?.let { add("Kommentar arbeidsinntekt: $it") }
                trygdeavtale.beregArt50?.let { add("Beregning art. 50: ${jaNei(it)}") }
                trygdeavtale.beregArt50Kommentar?.let { add("Kommentar art. 50: $it") }
                trygdeavtale.nordiskTrygdeAvtale?.let { add("Nordisk trygdeavtale: ${jaNei(it)}") }
                trygdeavtale.nordiskTrygdeAvtaleKommentar?.let { add("Kommentar nordisk: $it") }
            }

        if (linjer.isNotEmpty()) add(liste(linjer))
    }

private fun aktivitetspliktSeksjon(aktivitetsplikt: AktivitetspliktDto): List<Element> =
    buildList {
        add(underoverskrift("Aktivitetsplikt"))

        if (aktivitetsplikt.aktivitetsgrad.isNotEmpty()) {
            add(avsnitt("Aktivitetsgrad"))
            add(
                liste(
                    aktivitetsplikt.aktivitetsgrad.map { grad ->
                        "${formaterAktivitetsgrad(grad.vurdering)}" +
                            periode(grad.fom, grad.tom)
                    },
                ),
            )
        }

        if (aktivitetsplikt.unntak.isNotEmpty()) {
            add(avsnitt("Unntak"))
            add(
                liste(
                    aktivitetsplikt.unntak.map { unntak ->
                        "${formaterUnntak(unntak.unntak)}" +
                            periode(unntak.fom, unntak.tom)
                    },
                ),
            )
        }
    }

// --- Hjelpefunksjoner ---

private fun overskrift(tekst: String) =
    Element(
        ElementType.HEADING_TWO,
        listOf(InnerElement(type = ElementType.PARAGRAPH, text = tekst)),
    )

private fun underoverskrift(tekst: String) =
    Element(
        ElementType.HEADING_THREE,
        listOf(InnerElement(type = ElementType.PARAGRAPH, text = tekst)),
    )

private fun avsnitt(tekst: String) =
    Element(
        ElementType.PARAGRAPH,
        listOf(InnerElement(type = ElementType.PARAGRAPH, text = tekst)),
    )

private fun liste(elementer: List<String>) =
    Element(
        ElementType.BULLETED_LIST,
        elementer.map { tekst ->
            InnerElement(
                type = ElementType.LIST_ITEM,
                children = listOf(InnerElement(type = ElementType.PARAGRAPH, text = tekst)),
            )
        },
    )

private fun dato(d: LocalDate): String = d.format(DATO_FORMAT)

private fun periode(
    fom: LocalDate?,
    tom: LocalDate?,
): String =
    when {
        fom != null && tom != null -> " (${dato(fom)} – ${dato(tom)})"
        fom != null -> " (fra ${dato(fom)})"
        tom != null -> " (til ${dato(tom)})"
        else -> ""
    }

private fun formaterMetode(metode: BeregningsMetode): String =
    when (metode) {
        BeregningsMetode.BEST -> "Best"
        BeregningsMetode.NASJONAL -> "Nasjonal"
        BeregningsMetode.PRORATA -> "Prorata"
    }

private fun formaterAktivitetsgrad(grad: VurdertAktivitetsgrad): String =
    when (grad) {
        VurdertAktivitetsgrad.AKTIVITET_UNDER_50 -> "Aktivitet under 50 %"
        VurdertAktivitetsgrad.AKTIVITET_OVER_50 -> "Aktivitet over 50 %"
        VurdertAktivitetsgrad.AKTIVITET_100 -> "Aktivitet 100 %"
    }

private fun formaterUnntak(unntak: UnntakFraAktivitetsplikt): String =
    when (unntak) {
        UnntakFraAktivitetsplikt.OMSORG_BARN_UNDER_ETT_AAR -> "Omsorg for barn under ett år"
        UnntakFraAktivitetsplikt.OMSORG_BARN_SYKDOM -> "Omsorg for barn – sykdom"
        UnntakFraAktivitetsplikt.MANGLENDE_TILSYNSORDNING_SYKDOM -> "Manglende tilsynsordning – sykdom"
        UnntakFraAktivitetsplikt.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE -> "Sykdom eller redusert arbeidsevne"
        UnntakFraAktivitetsplikt.GRADERT_UFOERETRYGD -> "Gradert uføretrygd"
        UnntakFraAktivitetsplikt.MIDLERTIDIG_SYKDOM -> "Midlertidig sykdom"
        UnntakFraAktivitetsplikt.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT -> "Født 1963 eller tidligere og lav inntekt"
    }

private fun formaterSivilstand(sivilstand: Sivilstand): String {
    val status =
        sivilstand.sivilstatus.name
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    val fraTekst = sivilstand.gyldigFraOgMed?.let { " fra ${dato(it)}" } ?: ""
    return "$status$fraTekst"
}

private fun formaterAdresse(adresse: Adresse): String {
    val deler =
        listOfNotNull(
            adresse.adresseLinje1,
            adresse.adresseLinje2,
            adresse.adresseLinje3,
            listOfNotNull(adresse.postnr, adresse.poststed).joinToString(" ").takeIf { it.isNotBlank() },
            adresse.land?.takeIf { it.uppercase() != "NOR" },
        )
    val adresseTekst = if (deler.isNotEmpty()) deler.joinToString(", ") else adresse.type.name
    val periodeTekst =
        when {
            adresse.gyldigFraOgMed != null && adresse.gyldigTilOgMed != null -> {
                " (${datotid(adresse.gyldigFraOgMed!!)} – ${datotid(adresse.gyldigTilOgMed!!)})"
            }

            adresse.gyldigFraOgMed != null -> {
                " (fra ${datotid(adresse.gyldigFraOgMed!!)})"
            }

            else -> {
                ""
            }
        }
    return "$adresseTekst$periodeTekst"
}

private fun datotid(d: LocalDateTime): String = d.toLocalDate().format(DATO_FORMAT)

private fun jaNei(verdi: JaNei): String =
    when (verdi) {
        JaNei.JA -> "Ja"
        JaNei.NEI -> "Nei"
    }
