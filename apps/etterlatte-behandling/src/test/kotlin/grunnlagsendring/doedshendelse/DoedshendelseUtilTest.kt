package grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnEktefelleSafe
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DoedshendelseUtilTest {
    @Test
    fun `Skal finne naavaerende ektefelle`() {
        val person =
            mockPerson().copy(
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
            mockPerson().copy(
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
            mockPerson().copy(
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
            mockPerson().copy(
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
