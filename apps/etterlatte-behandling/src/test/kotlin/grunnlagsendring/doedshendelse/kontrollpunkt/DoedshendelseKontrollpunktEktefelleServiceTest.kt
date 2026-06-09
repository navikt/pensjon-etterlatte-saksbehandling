package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktEktefelleService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

private val AVDOED_FNR = Folkeregisteridentifikator.of("10418305857")
private val GJENLEVENDE_FNR = Folkeregisteridentifikator.of("09498230323")
private val BARN_FNR = Folkeregisteridentifikator.of("22511075258")

internal class DoedshendelseKontrollpunktEktefelleServiceTest {
    private val kontrollpunktService = DoedshendelseKontrollpunktEktefelleService()

    private val doedsdato: LocalDate = LocalDate.now()
    private val doedshendelse =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = AVDOED_FNR.value,
            avdoedDoedsdato = doedsdato,
            beroertFnr = GJENLEVENDE_FNR.value,
            relasjon = Relasjon.EKTEFELLE,
            endringstype = Endringstype.OPPRETTET,
        )
    private val avdoed =
        lagEktefellePersonDto(foedselsnummer = AVDOED_FNR, doedsdato = doedsdato)
    private val gjenlevende =
        lagEktefellePersonDto(foedselsnummer = GJENLEVENDE_FNR)

    @Test
    fun `Skal ikke opprette kontrollpunkt for ektefeller som har vaert gift siste 5 aar`() {
        val avdoed = avdoed.copy(sivilstand = sivilstand(5L))

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldBe emptyList()
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt for ektefeller med felles barn`() {
        val avdoed = avdoed.copy(sivilstand = sivilstand(3L), familieRelasjon = familieRelasjonMedBarn())
        val gjenlevende = gjenlevende.copy(familieRelasjon = familieRelasjonMedBarn())

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldBe emptyList()
    }

    @Test
    fun `Skal opprette kontrollpunkt for ektefeller uten felles barn under 18 aar`() {
        val avdoed = avdoed.copy(sivilstand = sivilstand(3L))
        val gjenlevende = gjenlevende.copy(familieRelasjon = familieRelasjonMedBarn())
        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenFellesBarn)
    }

    @Test
    fun `Skal opprette kontrollpunkt for ektefeller uten barn`() {
        val avdoed = avdoed.copy(sivilstand = sivilstand(3L))

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenBarn)
    }

    @Test
    fun `Skal opprette kontrollpunkt for ektefeller hvor vi ikke klarer aa utlede lengden`() {
        val giftUkjentDato =
            OpplysningDTO(
                verdi =
                    Sivilstand(
                        sivilstatus = Sivilstatus.GIFT,
                        relatertVedSiviltilstand = Folkeregisteridentifikator.of(doedshendelse.beroertFnr),
                        gyldigFraOgMed = null,
                        bekreftelsesdato = null,
                        kilde = "",
                    ),
                opplysningsid = "sivilstand",
            )
        val avdoed = avdoed.copy(sivilstand = listOf(giftUkjentDato))

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(doedsdato, gjenlevende.foedselsnummer.verdi.value),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift i mer enn 25 aar`() {
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            antallAarSiden = 30,
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                        sivilstand(
                            antallAarSiden = 3,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
            )
        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn25Aar(
                    doedsdato,
                    avdoed.foedselsnummer.verdi.value,
                ),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift i mer enn 15 aar og felles barn`() {
        val gjenlevende = gjenlevende.copy(familieRelasjon = familieRelasjonMedBarn())
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            antallAarSiden = 20,
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                        sivilstand(
                            antallAarSiden = 3,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
                familieRelasjon = familieRelasjonMedBarn(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn15AarFellesBarn(
                    doedsdato,
                    avdoed.foedselsnummer.verdi.value,
                ),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har giftet seg paa nytt`() {
        val nyEktefelle = "30901699972"
        val gjenlevende =
            gjenlevende.copy(
                familieRelasjon = familieRelasjonMedBarn(),
                sivilstand =
                    listOf(
                        sivilstand(antallAarSiden = 1, sivilstatus = Sivilstatus.GIFT, nyEktefelle),
                    ).flatten(),
            )
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            antallAarSiden = 20,
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                        sivilstand(
                            antallAarSiden = 3,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
                familieRelasjon = familieRelasjonMedBarn(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EpsErGiftPaaNytt(
                    doedsdato = doedsdato,
                    fnr = avdoed.foedselsnummer.verdi.value,
                    nyEktefelleFnr = nyEktefelle,
                ),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift i under 25 aar og uten felles barn`() {
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            antallAarSiden = 30,
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                        sivilstand(
                            antallAarSiden = 10,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
            )
        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.TidligereEpsGiftUnder25AarUtenFellesBarn,
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift i under 15 aar og med felles barn`() {
        val gjenlevende = avdoed.copy(familieRelasjon = familieRelasjonMedBarn())
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            antallAarSiden = 10,
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                        sivilstand(
                            antallAarSiden = 3,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
                familieRelasjon = familieRelasjonMedBarn(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.TidligereEpsGiftMindreEnn15AarFellesBarn,
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift naar status SKILT manger fom dato`() {
        val skiltUtenDatoOgRelasjon =
            OpplysningDTO(
                verdi =
                    Sivilstand(
                        sivilstatus = Sivilstatus.SKILT,
                        relatertVedSiviltilstand = null,
                        gyldigFraOgMed = null,
                        bekreftelsesdato = null,
                        kilde = "",
                    ),
                opplysningsid = "sivilstand",
            )

        val avdoed =
            avdoed.copy(
                sivilstand =
                    sivilstand(
                        antallAarSiden = 10,
                        sivilstatus = Sivilstatus.GIFT,
                        relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                    ) + skiltUtenDatoOgRelasjon,
                familieRelasjon = familieRelasjonMedBarn(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato,
                    avdoed.foedselsnummer.verdi.value,
                ),
            )
    }

    @Test
    fun `Skal opprette kontrollpunkt for tidligere ektefeller som har vaert gift med ukjent lengde paa ekteskapet`() {
        val giftUkjentDato =
            OpplysningDTO(
                verdi =
                    Sivilstand(
                        sivilstatus = Sivilstatus.GIFT,
                        relatertVedSiviltilstand = Folkeregisteridentifikator.of(doedshendelse.beroertFnr),
                        gyldigFraOgMed = null,
                        bekreftelsesdato = null,
                        kilde = "",
                    ),
                opplysningsid = "sivilstand",
            )
        val avdoed =
            avdoed.copy(
                sivilstand =
                    listOf(
                        listOf(giftUkjentDato),
                        sivilstand(
                            antallAarSiden = 3,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = gjenlevende.foedselsnummer.verdi.value,
                        ),
                    ).flatten(),
                familieRelasjon = familieRelasjonMedBarn(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato,
                    avdoed.foedselsnummer.verdi.value,
                ),
            )
    }

    @Test
    fun `Skal returnere EktefelleMedUkjentGiftemaalLengde dersom sivilstandstatus for naar en person ble gift mangler`() {
        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato,
                    avdoed.foedselsnummer.verdi.value,
                ),
            )
    }

    private fun sivilstand(
        antallAarSiden: Long,
        sivilstatus: Sivilstatus = Sivilstatus.GIFT,
        relatertPerson: String? = doedshendelse.beroertFnr,
    ) = listOf(
        OpplysningDTO(
            verdi =
                Sivilstand(
                    sivilstatus = sivilstatus,
                    relatertVedSiviltilstand = relatertPerson?.let { Folkeregisteridentifikator.of(it) },
                    gyldigFraOgMed = doedsdato.minusYears(antallAarSiden),
                    bekreftelsesdato = null,
                    kilde = "",
                ),
            opplysningsid = "sivilstand",
        ),
    )

    private fun familieRelasjonMedBarn() =
        OpplysningDTO(
            FamilieRelasjon(
                ansvarligeForeldre = emptyList(),
                foreldre = emptyList(),
                barn = listOf(BARN_FNR),
            ),
            "familierelasjon",
        )
}

private fun lagEktefellePersonDto(
    foedselsnummer: Folkeregisteridentifikator,
    foedselsdato: LocalDate? = LocalDate.now().minusYears(45),
    doedsdato: LocalDate? = null,
    familieRelasjon: OpplysningDTO<FamilieRelasjon>? = null,
): PersonDoedshendelseDto =
    PersonDoedshendelseDto(
        foedselsnummer = OpplysningDTO(foedselsnummer, null),
        foedselsdato = foedselsdato?.let { OpplysningDTO(it, null) },
        foedselsaar = null,
        doedsdato = doedsdato?.let { OpplysningDTO(it, null) },
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        sivilstand = null,
        utland = null,
        familieRelasjon = familieRelasjon,
        avdoedesBarn = null,
        avdoedesBarnUtenIdent = null,
    )
