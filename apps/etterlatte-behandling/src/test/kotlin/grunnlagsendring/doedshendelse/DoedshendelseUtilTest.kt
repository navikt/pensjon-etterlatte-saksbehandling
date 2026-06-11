package grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnEktefelleSafe
import no.nav.etterlatte.grunnlagsendring.doedshendelse.under23PaaDato
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.mockDoedshendelsePerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DoedshendelseUtilTest {
    @Test
    fun `Skal finne naavaerende ektefelle`() {
        val person =
            mockDoedshendelsePerson().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2020, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = JOVIAL_LAMA.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe JOVIAL_LAMA.value
    }

    @Test
    fun `Skal finne naavaerende ektefelle selv om man tidligere er gift med en annen`() {
        val tidligereEktefelle = "30901699972"
        val person =
            mockDoedshendelsePerson().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2015, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = tidligereEktefelle,
                        ),
                        sivilstand(
                            gyldigFom = LocalDate.of(2020, 1, 1),
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = tidligereEktefelle,
                        ),
                        sivilstand(
                            gyldigFom = LocalDate.of(2023, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = JOVIAL_LAMA.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe JOVIAL_LAMA.value
    }

    @Test
    fun `Skal returnere null dersom det finnes en skilt sivilstand uten gyldig fomDato`() {
        val tidligereEktefelle = "30901699972"
        val person =
            mockDoedshendelsePerson().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2015, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = tidligereEktefelle,
                        ),
                        sivilstand(
                            gyldigFom = null,
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = null,
                        ),
                        sivilstand(
                            gyldigFom = LocalDate.of(2023, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = JOVIAL_LAMA.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe null
    }

    @Test
    fun `Skal returnere null dersom personen er skilt`() {
        val person =
            mockDoedshendelsePerson().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2020, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = JOVIAL_LAMA.value,
                        ),
                        sivilstand(
                            gyldigFom = LocalDate.of(2025, 1, 1),
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = JOVIAL_LAMA.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe null
    }

    @Test
    fun `under20PaaDato - barn paa 5 aar er under 23`() {
        val person = mockDoedshendelsePerson().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(5), null))
        person.under23PaaDato(LocalDate.now()) shouldBe true
    }

    @Test
    fun `under23PaaDato - barn paa 23 aar er ikke under 23`() {
        val person = mockDoedshendelsePerson().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(23), null))
        person.under23PaaDato(LocalDate.now()) shouldBe false
    }

    @Test
    fun `under23PaaDato - bruker 31 desember som fallback naar kun foedselsaar er kjent`() {
        val personInnenforÅr =
            mockDoedshendelsePerson().copy(
                foedselsdato = null,
                foedselsaar = OpplysningDTO(LocalDate.now().year - 23, null),
            )
        personInnenforÅr.under23PaaDato(LocalDate.now()) shouldBe true

        val personEldreEnn23 =
            mockDoedshendelsePerson().copy(
                foedselsdato = null,
                foedselsaar = OpplysningDTO(LocalDate.now().year - 24, null),
            )
        personEldreEnn23.under23PaaDato(LocalDate.now()) shouldBe false
    }

    @Test
    fun `under23PaaDato - returnerer true naar foedselsdato og foedselsaar er null`() {
        val person = mockDoedshendelsePerson().copy(foedselsdato = null, foedselsaar = null)
        person.under23PaaDato(LocalDate.now()) shouldBe true
    }

    private fun sivilstand(
        gyldigFom: LocalDate?,
        sivilstatus: Sivilstatus,
        relatertPerson: String?,
    ) = listOf(
        OpplysningDTO(
            verdi =
                Sivilstand(
                    sivilstatus = sivilstatus,
                    relatertVedSiviltilstand = relatertPerson?.let { Folkeregisteridentifikator.of(it) },
                    gyldigFraOgMed = gyldigFom,
                    bekreftelsesdato = null,
                    kilde = "",
                ),
            opplysningsid = "sivilstand",
        ),
    )
}
