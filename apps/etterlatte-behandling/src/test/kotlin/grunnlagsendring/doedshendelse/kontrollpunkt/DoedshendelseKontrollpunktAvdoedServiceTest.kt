package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktAvdoedService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseKontrollpunktAvdoedServiceTest {
    private val kontrollpunktService = DoedshendelseKontrollpunktAvdoedService()

    private fun lagAvdoedPersonDto(
        utland: Utland? = null,
        foedselsnummer: Folkeregisteridentifikator = Folkeregisteridentifikator.of("10418305857"),
        doedsdato: LocalDate? = null,
    ): PersonDoedshendelseDto =
        PersonDoedshendelseDto(
            foedselsnummer = OpplysningDTO(foedselsnummer, null),
            foedselsdato = OpplysningDTO(LocalDate.now().minusYears(70), null),
            foedselsaar = null,
            doedsdato = doedsdato?.let { OpplysningDTO(it, null) },
            bostedsadresse = null,
            deltBostedsadresse = null,
            kontaktadresse = null,
            oppholdsadresse = null,
            sivilstand = null,
            utland = utland?.let { OpplysningDTO(it, null) },
            familieRelasjon = null,
            avdoedesBarn = null,
            avdoedesBarnUtenIdent = null,
        )

    @Test
    fun `Skal returnere kontrollpunkt hvis avdoed ikke har doedsdato i PDL`() {
        val avdoed = lagAvdoedPersonDto(doedsdato = null)

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede har D-nummer`() {
        val avdoed =
            lagAvdoedPersonDto(
                doedsdato = LocalDate.now(),
                foedselsnummer = Folkeregisteridentifikator.of("69057949961"),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarDNummer)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede hadde utvandring`() {
        val avdoed =
            lagAvdoedPersonDto(
                utland =
                    Utland(
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = listOf(UtflyttingFraNorge("Sverige", LocalDate.now().minusMonths(2))),
                    ),
                doedsdato = LocalDate.now(),
            )

        val kontrollpunkter = kontrollpunktService.identifiser(avdoed)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarUtvandret)
    }
}
