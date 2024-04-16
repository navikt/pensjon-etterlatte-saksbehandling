package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.LITE_BARN
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktEktefelleService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DoedshendelseKontrollpunktEktefelleServiceTest {
    private val kontrollpunktService = DoedshendelseKontrollpunktEktefelleService()

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
                DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(doedsdato, avdoed.foedselsnummer.verdi.value),
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
        val gjenlevende = avdoed.copy(familieRelasjon = familieRelasjonMedBarn())
        val avdoed =
            gjenlevende.copy(
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
    fun `Skal returnere AvdoedHarIkkeVaertGift hvis avdoed ikke har vaert registert som gift eller partner`() {
        val kontrollpunkter = kontrollpunktService.identifiser(gjenlevende, avdoed)

        kontrollpunkter shouldContainExactly
            listOf(
                DoedshendelseKontrollpunkt.AvdoedHarIkkeVaertGift,
            )
    }

    companion object {
        private val doedsdato: LocalDate = LocalDate.now()
        private val doedshendelse =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = doedsdato,
                beroertFnr = JOVIAL_LAMA.value,
                relasjon = Relasjon.EKTEFELLE,
                endringstype = Endringstype.OPPRETTET,
            )
        private val avdoed =
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
                doedsdato = OpplysningDTO(doedsdato, null),
            )
        private val gjenlevende =
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
            )

        fun sivilstand(
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

        fun familieRelasjonMedBarn() =
            OpplysningDTO(
                FamilieRelasjon(
                    ansvarligeForeldre = emptyList(),
                    foreldre = emptyList(),
                    barn = listOf(LITE_BARN),
                ),
                "familierelasjon",
            )
    }
}
