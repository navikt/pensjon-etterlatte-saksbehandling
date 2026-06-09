package grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnEktefelleSafe
import no.nav.etterlatte.grunnlagsendring.doedshendelse.under20PaaDato
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

private val UTIL_EKTEFELLE_FNR = Folkeregisteridentifikator.of("09498230323")

internal class DoedshendelseUtilTest {
    @Test
    fun `Skal finne naavaerende ektefelle`() {
        val person =
            lagUtilPersonDto().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2020, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = UTIL_EKTEFELLE_FNR.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe UTIL_EKTEFELLE_FNR.value
    }

    @Test
    fun `Skal finne naavaerende ektefelle selv om man tidligere er gift med en annen`() {
        val tidligereEktefelle = "30901699972"
        val person =
            lagUtilPersonDto().copy(
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
                            relatertPerson = UTIL_EKTEFELLE_FNR.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe UTIL_EKTEFELLE_FNR.value
    }

    @Test
    fun `Skal returnere null dersom det finnes en skilt sivilstand uten gyldig fomDato`() {
        val tidligereEktefelle = "30901699972"
        val person =
            lagUtilPersonDto().copy(
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
                            relatertPerson = UTIL_EKTEFELLE_FNR.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe null
    }

    @Test
    fun `Skal returnere null dersom personen er skilt`() {
        val person =
            lagUtilPersonDto().copy(
                sivilstand =
                    listOf(
                        sivilstand(
                            gyldigFom = LocalDate.of(2020, 1, 1),
                            sivilstatus = Sivilstatus.GIFT,
                            relatertPerson = UTIL_EKTEFELLE_FNR.value,
                        ),
                        sivilstand(
                            gyldigFom = LocalDate.of(2025, 1, 1),
                            sivilstatus = Sivilstatus.SKILT,
                            relatertPerson = UTIL_EKTEFELLE_FNR.value,
                        ),
                    ).flatten(),
            )

        finnEktefelleSafe(person) shouldBe null
    }

    @Test
    fun `under20PaaDato - barn paa 5 aar er under 20`() {
        val person = lagUtilPersonDto().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(5), null))
        person.under20PaaDato(LocalDate.now()) shouldBe true
    }

    @Test
    fun `under20PaaDato - barn paa 20 aar er ikke under 20`() {
        val person = lagUtilPersonDto().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), null))
        person.under20PaaDato(LocalDate.now()) shouldBe false
    }

    @Test
    fun `under20PaaDato - barn paa 61 aar er ikke under 20`() {
        val person = lagUtilPersonDto().copy(foedselsdato = OpplysningDTO(LocalDate.now().minusYears(61), null))
        person.under20PaaDato(LocalDate.now()) shouldBe false
    }

    @Test
    fun `under20PaaDato - bruker 1 januar som fallback naar kun foedselsaar er kjent`() {
        val iAar = LocalDate.now().year
        val yngrePerson = lagUtilPersonDto().copy(foedselsdato = null, foedselsaar = OpplysningDTO(iAar, null))
        yngrePerson.under20PaaDato(LocalDate.now()) shouldBe true

        val eldre = lagUtilPersonDto().copy(foedselsdato = null, foedselsaar = OpplysningDTO(iAar - 20, null))
        eldre.under20PaaDato(LocalDate.now()) shouldBe false
    }

    @Test
    fun `under20PaaDato - returnerer true naar foedselsdato og foedselsaar er null`() {
        val person = lagUtilPersonDto().copy(foedselsdato = null, foedselsaar = null)
        person.under20PaaDato(LocalDate.now()) shouldBe true
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

private fun lagUtilPersonDto(): PersonDoedshendelseDto =
    PersonDoedshendelseDto(
        foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of("10418305857"), null),
        foedselsdato = OpplysningDTO(LocalDate.now().minusYears(45), null),
        foedselsaar = null,
        doedsdato = null,
        bostedsadresse = null,
        deltBostedsadresse = null,
        kontaktadresse = null,
        oppholdsadresse = null,
        sivilstand = null,
        utland = null,
        familieRelasjon = null,
        avdoedesBarn = null,
        avdoedesBarnUtenIdent = null,
    )
