package no.nav.etterlatte.testdata.features.soeknad

import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.ArbeidOgUtdanningOMS
import no.nav.etterlatte.libs.common.innsendtsoeknad.ForholdTilAvdoedeOMS
import no.nav.etterlatte.libs.common.innsendtsoeknad.ForholdTilAvdoedeType
import no.nav.etterlatte.libs.common.innsendtsoeknad.InntektOgPensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.JobbStatusTypeOMS
import no.nav.etterlatte.libs.common.innsendtsoeknad.Kontaktinfo
import no.nav.etterlatte.libs.common.innsendtsoeknad.SivilstatusType
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.YtelserAndre
import no.nav.etterlatte.libs.common.innsendtsoeknad.YtelserNav
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.GjenlevendeForelder
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Avdoed
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Barn
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.DatoSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.EnumSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.FritekstSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.GjenlevendeOMS
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Innsender
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Opplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.JsonMessage
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

object SoeknadMapper {
    fun opprettJsonMessage(
        type: SoeknadType,
        gjenlevendeFnr: String,
        avdoedFnr: String,
        barn: List<String> = emptyList(),
        behandlingssteg: Behandlingssteg,
    ): JsonMessage =
        when (type) {
            SoeknadType.OMSTILLINGSSTOENAD ->
                JsonMessage.newMessage(
                    mutableMapOf(
                        SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV.lagParMedEventNameKey(),
                        "@skjema_info" to
                            opprettOmstillingsstoenadSoeknad(
                                soekerFnr = gjenlevendeFnr,
                                avdoedFnr = avdoedFnr,
                                barn = barn,
                            ).toObjectNode(),
                        "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
                        "@template" to "soeknad",
                        "@fnr_soeker" to gjenlevendeFnr,
                        "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
                        Behandlingssteg.KEY to behandlingssteg.name,
                    ),
                )

            SoeknadType.BARNEPENSJON ->
                JsonMessage.newMessage(
                    mutableMapOf(
                        SoeknadInnsendtHendelseType.EVENT_NAME_BEHANDLINGBEHOV.lagParMedEventNameKey(),
                        "@skjema_info" to
                            opprettBarnepensjonSoeknad(
                                gjenlevendeFnr = gjenlevendeFnr,
                                avdoedFnr = avdoedFnr,
                                barnFnr = barn.first(),
                                soesken = barn.drop(1),
                            ).toObjectNode(),
                        "@lagret_soeknad_id" to "TEST-${UUID.randomUUID()}",
                        "@template" to "soeknad",
                        "@fnr_soeker" to barn.first(),
                        "@hendelse_gyldig_til" to OffsetDateTime.now().plusMinutes(60L),
                        Behandlingssteg.KEY to behandlingssteg.name,
                    ),
                )

            else -> {
                throw Exception("Ukjent soknad type: '$type'")
            }
        }

    private fun opprettBarnepensjonSoeknad(
        gjenlevendeFnr: String,
        barnFnr: String,
        avdoedFnr: String,
        soesken: List<String>,
    ) = Barnepensjon(
        imageTag = UUID.randomUUID().toString(),
        spraak = Spraak.NB,
        innsender =
            Innsender(
                fornavn = Opplysning(adjektiv.random()),
                etternavn = Opplysning(substantiv.random()),
                foedselsnummer = Opplysning(Foedselsnummer.of(gjenlevendeFnr)),
            ),
        harSamtykket = Opplysning(true),
        utbetalingsInformasjon = null,
        soeker = opprettBarn(barnFnr),
        foreldre =
            listOf(
                opprettGjenlevendeForelder(gjenlevendeFnr),
                opprettAvdoed(avdoedFnr),
            ),
        soesken = soesken.map(::opprettBarn),
    )

    private fun opprettOmstillingsstoenadSoeknad(
        soekerFnr: String,
        avdoedFnr: String,
        barn: List<String>,
    ) = Omstillingsstoenad(
        imageTag = UUID.randomUUID().toString(),
        spraak = Spraak.NB,
        innsender =
            Innsender(
                fornavn = Opplysning(adjektiv.random()),
                etternavn = Opplysning(substantiv.random()),
                foedselsnummer = Opplysning(Foedselsnummer.of(soekerFnr)),
            ),
        harSamtykket = Opplysning(true),
        utbetalingsInformasjon = null,
        soeker =
            GjenlevendeOMS(
                fornavn = Opplysning(adjektiv.random()),
                etternavn = Opplysning(substantiv.random()),
                foedselsnummer = Opplysning(Foedselsnummer.of(soekerFnr)),
                statsborgerskap = Opplysning("NORGE"),
                sivilstatus = Opplysning("ENKE"),
                adresse = null,
                bostedsAdresse = null,
                kontaktinfo =
                    Kontaktinfo(
                        telefonnummer = Opplysning(FritekstSvar("99 88 77 66")),
                    ),
                flyktning = null,
                oppholdUtland = BetingetOpplysning(EnumSvar(JaNeiVetIkke.NEI, ""), null, null),
                nySivilstatus = BetingetOpplysning(EnumSvar(SivilstatusType.ENSLIG, ""), null, null),
                arbeidOgUtdanning =
                    ArbeidOgUtdanningOMS(
                        dinSituasjon = Opplysning(listOf(EnumSvar(JobbStatusTypeOMS.ARBEIDSTAKER, ""))),
                        arbeidsforhold = null,
                        selvstendig = null,
                        etablererVirksomhet = null,
                        tilbud = null,
                        arbeidssoeker = null,
                        utdanning = null,
                        annenSituasjon = null,
                    ),
                fullfoertUtdanning = null,
                inntektOgPensjon =
                    InntektOgPensjon(
                        loennsinntekt = null,
                        naeringsinntekt = null,
                        pensjonEllerUfoere = null,
                        inntektViaYtelserFraNAV = null,
                        ingenInntekt = null,
                        ytelserNAV =
                            YtelserNav(
                                soektOmYtelse = Opplysning(EnumSvar(JaNeiVetIkke.NEI, "")),
                                soektYtelse = null,
                            ),
                        ytelserAndre =
                            YtelserAndre(
                                soektOmYtelse = Opplysning(EnumSvar(JaNeiVetIkke.NEI, "")),
                                soektYtelse = null,
                                pensjonsordning = null,
                            ),
                    ),
                uregistrertEllerVenterBarn = Opplysning(EnumSvar(JaNeiVetIkke.NEI, ""), null),
                forholdTilAvdoede =
                    ForholdTilAvdoedeOMS(
                        relasjon = Opplysning(EnumSvar(ForholdTilAvdoedeType.GIFT, ""), null),
                        datoForInngaattPartnerskap = null,
                        datoForInngaattSamboerskap = null,
                        datoForSkilsmisse = null,
                        datoForSamlivsbrudd = null,
                        fellesBarn = null,
                        samboereMedFellesBarnFoerGiftemaal = null,
                        tidligereGift = null,
                        mottokBidrag = null,
                    ),
                omsorgForBarn = Opplysning(EnumSvar(JaNeiVetIkke.JA, ""), null),
            ),
        avdoed = opprettAvdoed(avdoedFnr),
        barn = barn.map(::opprettBarn),
    )

    private fun opprettGjenlevendeForelder(fnr: String) =
        GjenlevendeForelder(
            fornavn = Opplysning(adjektiv.random()),
            etternavn = Opplysning(substantiv.random()),
            foedselsnummer = Opplysning(Foedselsnummer.of(fnr)),
            adresse = Opplysning("Testveien 12, 0123 Oslo"),
            statsborgerskap = Opplysning("NORGE"),
            kontaktinfo =
                Kontaktinfo(
                    telefonnummer = Opplysning(FritekstSvar("99 88 77 66")),
                ),
        )

    private fun opprettAvdoed(fnr: String) =
        Avdoed(
            fornavn = Opplysning(adjektiv.random()),
            etternavn = Opplysning(substantiv.random()),
            foedselsnummer = Opplysning(Foedselsnummer.of(fnr)),
            datoForDoedsfallet = Opplysning(DatoSvar(LocalDate.now().minusWeeks(1))),
            statsborgerskap = Opplysning(FritekstSvar("NORGE")),
            utenlandsopphold = BetingetOpplysning(EnumSvar(JaNeiVetIkke.NEI, ""), null, emptyList()),
            doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = Opplysning(EnumSvar(JaNeiVetIkke.NEI, ""), null),
            militaertjeneste = null,
        )

    private fun opprettBarn(fnr: String) =
        Barn(
            fornavn = Opplysning(adjektiv.random()),
            etternavn = Opplysning(substantiv.random()),
            foedselsnummer = Opplysning(Foedselsnummer.of(fnr)),
            statsborgerskap = Opplysning("NORGE"),
            utenlandsAdresse = null,
            bosattNorge = null,
            foreldre = emptyList(),
            ukjentForelder = null,
            verge = null,
            dagligOmsorg = null,
        )

    private val substantiv =
        listOf(
            "Bil",
            "Gitar",
            "Flaske",
            "Kopp",
            "Laptop",
            "Kis",
            "Sekk",
            "Pille",
            "Handel",
            "Oppgave",
            "Traktor",
            "Hest",
            "Fest",
            "Propp",
            "Lavo",
            "Kabel",
            "Pose",
            "Dør",
            "Sjekkliste",
        )

    private val adjektiv =
        listOf(
            "Fin",
            "Stygg",
            "Liten",
            "Stor",
            "Rask",
            "Treg",
            "Rund",
            "Ekkel",
            "Ufin",
            "Kul",
            "Digg",
            "Synlig",
            "Falsk",
            "Sinnsyk",
            "Ordknapp",
            "Taus",
            "Artig",
            "Morsom",
            "Skremmende",
        )
}
