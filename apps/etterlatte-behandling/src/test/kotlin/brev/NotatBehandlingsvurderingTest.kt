package no.nav.etterlatte.brev

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.grunnlag.BeregningsGrunnlag
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.brev.Slate.ElementType
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsmetodeForAvdoed
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotatBehandlingsvurderingTest {
    private val behandlingId = UUID.randomUUID()
    private val sakId = SakId(42L)
    private val vedtakId = 99L
    private val avdoedFnr = "12345678901"

    // --- Byggere for testdata ---

    private fun avdoed(
        fnr: String = avdoedFnr,
        navn: String = "Ola Nordmann",
        foedselsdato: LocalDate? = LocalDate.of(1950, 1, 1),
        doedsdato: LocalDate? = LocalDate.of(2022, 3, 12),
    ) = AvdoedPersonopplysninger(
        fnr = fnr,
        navn = navn,
        foedselsdato = foedselsdato,
        doedsdato = doedsdato,
        statsborgerskap = null,
        bostedsadresser = emptyList(),
        sivilstand = emptyList(),
        utland = null,
        avdoedesBarn = emptyList(),
    )

    private fun trygdetidDto(
        ident: String = avdoedFnr,
        grunnlag: List<TrygdetidGrunnlagDto> = emptyList(),
        beregnetTrygdetid: DetaljertBeregnetTrygdetidDto? = null,
        begrunnelse: String? = null,
    ) = TrygdetidDto(
        id = UUID.randomUUID(),
        ident = ident,
        behandlingId = behandlingId,
        beregnetTrygdetid = beregnetTrygdetid,
        trygdetidGrunnlag = grunnlag,
        opplysninger = GrunnlagOpplysningerDto.tomt(),
        overstyrtNorskPoengaar = null,
        opplysningerDifferanse = OpplysningerDifferanse(false, GrunnlagOpplysningerDto.tomt()),
        begrunnelse = begrunnelse,
    )

    private fun trygdetidGrunnlagDto(
        type: String = "NASJONAL",
        bosted: String = "NOR",
        fom: LocalDate = LocalDate.of(2000, 1, 1),
        tom: LocalDate = LocalDate.of(2022, 3, 12),
    ) = TrygdetidGrunnlagDto(
        id = UUID.randomUUID(),
        type = type,
        bosted = bosted,
        periodeFra = fom,
        periodeTil = tom,
        kilde = null,
        beregnet = BeregnetTrygdetidGrunnlagDto(dager = 0, maaneder = 2, aar = 22),
        begrunnelse = null,
        poengInnAar = false,
        poengUtAar = false,
        prorata = false,
    )

    private fun beregnetTrygdetid(samletNorsk: Int = 40) =
        DetaljertBeregnetTrygdetidDto(
            resultat =
                DetaljertBeregnetTrygdetidResultat(
                    faktiskTrygdetidNorge = null,
                    faktiskTrygdetidTeoretisk = null,
                    fremtidigTrygdetidNorge = null,
                    fremtidigTrygdetidTeoretisk = null,
                    samletTrygdetidNorge = samletNorsk,
                    samletTrygdetidTeoretisk = null,
                    prorataBroek = null,
                    overstyrt = false,
                    yrkesskade = false,
                    beregnetSamletTrygdetidNorge = null,
                ),
            tidspunkt = Tidspunkt.now(),
        )

    private fun beregningsgrunnlag(
        metode: BeregningsMetode = BeregningsMetode.NASJONAL,
        begrunnelse: String? = null,
    ) = BeregningsGrunnlag(
        behandlingId = behandlingId,
        kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
        beregningsMetode = BeregningsMetodeBeregningsgrunnlag(metode, begrunnelse),
    )

    private fun aktivitetsplikt(
        aktivitetsgrad: List<AktivitetspliktAktivitetsgradDto> = emptyList(),
        unntak: List<UnntakFraAktivitetDto> = emptyList(),
    ) = AktivitetspliktDto(
        sakId = SakId(1L),
        avdoedDoedsmaaned = YearMonth.of(2022, 3),
        aktivitetsgrad = aktivitetsgrad,
        unntak = unntak,
        brukersAktivitet = emptyList(),
    )

    private fun trygdeavtale(avtaleKode: String = "EOS") =
        Trygdeavtale(
            behandlingId = behandlingId,
            avtaleKode = avtaleKode,
            avtaleDatoKode = null,
            avtaleKriteriaKode = null,
            personKrets = JaNei.JA,
            arbInntekt1G = null,
            arbInntekt1GKommentar = null,
            beregArt50 = null,
            beregArt50Kommentar = null,
            nordiskTrygdeAvtale = null,
            nordiskTrygdeAvtaleKommentar = null,
            kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
        )

    private fun bygSlate(
        avdoede: List<AvdoedPersonopplysninger> = listOf(avdoed()),
        trygdetider: List<TrygdetidDto> = emptyList(),
        trygdeavtale: Trygdeavtale? = null,
        beregningsgrunnlag: BeregningsGrunnlag? = null,
        aktivitetsplikt: AktivitetspliktDto? = null,
    ) = byggBehandlingsvurderingSlate(
        sakId = sakId,
        vedtakId = vedtakId,
        vedtakType = VedtakType.INNVILGELSE,
        datoAttestert = LocalDate.of(2024, 6, 15),
        avdoede = avdoede,
        trygdetider = trygdetider,
        trygdeavtale = trygdeavtale,
        beregningsgrunnlag = beregningsgrunnlag,
        aktivitetsplikt = aktivitetsplikt,
    )

    // --- Hjelpefunksjoner for assertions ---

    private fun Slate.allTexts(): List<String> =
        elements
            .flatMap { it.children }
            .flatMap { listOfNotNull(it.text) + (it.children?.mapNotNull { c -> c.text } ?: emptyList()) }
            .filter { it.isNotBlank() }

    private fun Slate.headings(): List<String> =
        elements
            .filter { it.type in listOf(ElementType.HEADING_TWO, ElementType.HEADING_THREE) }
            .flatMap { it.children }
            .mapNotNull { it.text }

    // --- Tester ---

    @Test
    fun `inneholder overskrift og metadata`() {
        val slate = bygSlate()

        slate.headings() shouldContain "Behandlingsvurdering"
        slate.allTexts() shouldContain "SakId: $sakId"
        slate.allTexts() shouldContain "VedtakId: $vedtakId"
        slate.allTexts() shouldContain "Vedtaktype: Innvilgelse"
        slate.allTexts() shouldContain "Attestert: 15.06.2024"
    }

    @Test
    fun `avdoedens navn og doedsdato vises i seksjonstittel`() {
        val slate = bygSlate(avdoede = listOf(avdoed(navn = "Kari Nordmann", doedsdato = LocalDate.of(2021, 6, 15))))

        slate.headings() shouldContain "Avdød: Kari Nordmann – død 15.06.2021"
    }

    @Test
    fun `personopplysninger for avdoed vises`() {
        val avdoedMedData =
            avdoed().copy(
                statsborgerskap = "SWE",
                sivilstand =
                    listOf(
                        Sivilstand(
                            sivilstatus = Sivilstatus.GIFT,
                            relatertVedSiviltilstand = null,
                            gyldigFraOgMed = LocalDate.of(1980, 5, 10),
                            bekreftelsesdato = null,
                            historisk = false,
                            kilde = "PDL",
                        ),
                    ),
                utland =
                    Utland(
                        innflyttingTilNorge = listOf(InnflyttingTilNorge("SWE", LocalDate.of(1975, 3, 1), null, null)),
                        utflyttingFraNorge = listOf(UtflyttingFraNorge("DNK", LocalDate.of(1970, 8, 15))),
                    ),
                bostedsadresser =
                    listOf(
                        Adresse(
                            type = AdresseType.VEGADRESSE,
                            aktiv = true,
                            adresseLinje1 = "Storgata 1",
                            postnr = "0123",
                            poststed = "Oslo",
                            land = null,
                            kilde = "PDL",
                        ),
                    ),
                avdoedesBarn =
                    listOf(
                        AvdoedBarnOpplysning(
                            navn = "Petter Nordmann",
                            foedselsdato = LocalDate.of(2005, 4, 20),
                            doedsdato = null,
                        ),
                    ),
            )

        val texts = bygSlate(avdoede = listOf(avdoedMedData)).allTexts()

        texts shouldContain "Statsborgerskap: SWE"
        texts shouldContain "Fra SWE (01.03.1975)"
        texts shouldContain "Til DNK (15.08.1970)"
        texts shouldContain "Storgata 1, 0123 Oslo"
        texts shouldContain "Petter Nordmann (født 20.04.2005)"
    }

    @Test
    fun `beregningsmetode nasjonal vises`() {
        val slate = bygSlate(beregningsgrunnlag = beregningsgrunnlag(BeregningsMetode.NASJONAL))

        slate.headings() shouldContain "Beregningsmetode"
        slate.allTexts() shouldContain "Metode: Nasjonal"
    }

    @Test
    fun `beregningsmetode prorata med begrunnelse vises`() {
        val texts = bygSlate(beregningsgrunnlag = beregningsgrunnlag(BeregningsMetode.PRORATA, "EØS-avtale gjelder")).allTexts()

        texts shouldContain "Metode: Prorata"
        texts shouldContain "Begrunnelse: EØS-avtale gjelder"
    }

    @Test
    fun `beregningsmetode for flere avdoede vises med navn og periode`() {
        val fnr2 = "98765432109"
        val grunnlag =
            BeregningsGrunnlag(
                behandlingId = behandlingId,
                kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now()),
                beregningsMetode = BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.BEST),
                beregningsMetodeFlereAvdoede =
                    listOf(
                        GrunnlagMedPeriode(
                            data = BeregningsmetodeForAvdoed(avdoedFnr, BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.NASJONAL)),
                            fom = LocalDate.of(2022, 4, 1),
                        ),
                        GrunnlagMedPeriode(
                            data =
                                BeregningsmetodeForAvdoed(
                                    fnr2,
                                    BeregningsMetodeBeregningsgrunnlag(BeregningsMetode.PRORATA, "Særskilt begrunnelse"),
                                ),
                            fom = LocalDate.of(2022, 4, 1),
                        ),
                    ),
            )

        val texts =
            bygSlate(
                avdoede = listOf(avdoed(fnr = avdoedFnr, navn = "Ola"), avdoed(fnr = fnr2, navn = "Kari")),
                beregningsgrunnlag = grunnlag,
            ).allTexts()

        texts.any { it.contains("Ola") && it.contains("Nasjonal") } shouldBe true
        texts.any { it.contains("Kari") && it.contains("Prorata") } shouldBe true
        texts shouldContain "Begrunnelse: Særskilt begrunnelse"
    }

    @Test
    fun `beregningsgrunnlag mangler gir lesbar melding`() {
        bygSlate(beregningsgrunnlag = null).allTexts() shouldContain "Beregningsgrunnlag mangler"
    }

    @Test
    fun `trygdetid med perioder og beregnet resultat vises`() {
        val slate =
            bygSlate(
                trygdetider =
                    listOf(
                        trygdetidDto(
                            begrunnelse = "Lang botid",
                            grunnlag = listOf(trygdetidGrunnlagDto()),
                            beregnetTrygdetid = beregnetTrygdetid(samletNorsk = 40),
                        ),
                    ),
            )

        slate.headings().any { it.contains("Trygdetid") } shouldBe true
        val texts = slate.allTexts()
        texts shouldContain "Begrunnelse: Lang botid"
        texts.any { it.contains("NASJONAL") && it.contains("NOR") } shouldBe true
        texts.any { it.contains("Samlet norsk: 40 år") } shouldBe true
    }

    @Test
    fun `yrkesskade vises naar flagget er satt`() {
        val trygdetidMedYrkesskade =
            trygdetidDto(
                beregnetTrygdetid =
                    DetaljertBeregnetTrygdetidDto(
                        resultat =
                            DetaljertBeregnetTrygdetidResultat(
                                faktiskTrygdetidNorge = null,
                                faktiskTrygdetidTeoretisk = null,
                                fremtidigTrygdetidNorge = null,
                                fremtidigTrygdetidTeoretisk = null,
                                samletTrygdetidNorge = null,
                                samletTrygdetidTeoretisk = null,
                                prorataBroek = null,
                                overstyrt = false,
                                yrkesskade = true,
                                beregnetSamletTrygdetidNorge = null,
                            ),
                        tidspunkt = Tidspunkt.now(),
                    ),
            )

        bygSlate(
            beregningsgrunnlag = beregningsgrunnlag(),
            trygdetider = listOf(trygdetidMedYrkesskade),
        ).allTexts() shouldContain "Yrkesskade: Ja"
    }

    @Test
    fun `to avdoede gir to separate trygdetid-seksjoner med riktig navn`() {
        val fnr2 = "22334455667"
        val headings =
            bygSlate(
                avdoede = listOf(avdoed(fnr = avdoedFnr, navn = "Ola"), avdoed(fnr = fnr2, navn = "Kari")),
                trygdetider = listOf(trygdetidDto(ident = avdoedFnr), trygdetidDto(ident = fnr2)),
            ).headings()

        headings.count { it.startsWith("Trygdetid") } shouldBe 2
        headings.any { it.contains("Ola") } shouldBe true
        headings.any { it.contains("Kari") } shouldBe true
    }

    @Test
    fun `aktivitetsplikt med grad og unntak vises`() {
        val aktplikt =
            aktivitetsplikt(
                aktivitetsgrad =
                    listOf(
                        AktivitetspliktAktivitetsgradDto(
                            vurdering = VurdertAktivitetsgrad.AKTIVITET_OVER_50,
                            fom = LocalDate.of(2022, 6, 1),
                            tom = null,
                        ),
                    ),
                unntak =
                    listOf(
                        UnntakFraAktivitetDto(
                            unntak = UnntakFraAktivitetsplikt.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE,
                            fom = LocalDate.of(2022, 4, 1),
                            tom = LocalDate.of(2022, 5, 31),
                        ),
                    ),
            )

        val slate = bygSlate(aktivitetsplikt = aktplikt)

        slate.headings() shouldContain "Aktivitetsplikt"
        val texts = slate.allTexts()
        texts.any { it.contains("Aktivitet over 50 %") } shouldBe true
        texts.any { it.contains("Sykdom eller redusert arbeidsevne") } shouldBe true
    }

    @Test
    fun `aktivitetsplikt-seksjon vises ikke naar null`() {
        bygSlate(aktivitetsplikt = null).headings() shouldNotContain "Aktivitetsplikt"
    }

    @Test
    fun `trygdeavtale vises med kode og vurderinger`() {
        val avtale =
            trygdeavtale("EOS").copy(
                nordiskTrygdeAvtale = JaNei.NEI,
                nordiskTrygdeAvtaleKommentar = "Ikke aktuelt",
            )

        val slate = bygSlate(trygdeavtale = avtale)

        slate.headings() shouldContain "Trygdeavtale"
        val texts = slate.allTexts()
        texts shouldContain "Avtale: EOS"
        texts shouldContain "Personkrets: Ja"
        texts shouldContain "Nordisk trygdeavtale: Nei"
        texts shouldContain "Kommentar nordisk: Ikke aktuelt"
    }

    @Test
    fun `trygdeavtale-seksjon vises ikke naar ingen avtale`() {
        bygSlate(trygdeavtale = null).headings() shouldNotContain "Trygdeavtale"
    }
}
