package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseInternalPdlKontrollpunktServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val pesysKlient = mockk<PesysKlient>()
    private val kontrollpunktService = DoedshendelseKontrollpunktService(pdlTjenesterKlient, pesysKlient)
    private val doedshendelseInternal =
        DoedshendelseInternal.nyHendelse(
            avdoedFnr = KONTANT_FOT.value,
            avdoedDoedsdato = LocalDate.now(),
            beroertFnr = JOVIAL_LAMA.value,
            relasjon = Relasjon.BARN,
            endringstype = Endringstype.OPPRETTET,
        )

    @BeforeEach
    fun oppsett() {
        coEvery { pesysKlient.hentSaker(doedshendelseInternal.beroertFnr) } returns emptyList()

        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternal.avdoedFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternal.avdoedDoedsdato, null),
            )
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = any(),
                rolle = PersonRolle.BARN,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KONTANT_FOT, JOVIAL_LAMA),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.beroertFnr,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternal.beroertFnr), null),
            )
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis avdoed ikke har doedsdato i PDL`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns mockPerson()

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den beroerte har ufoeretrygd`() {
        coEvery { pesysKlient.hentSaker(doedshendelseInternal.beroertFnr) } returns
            listOf(
                SakSammendragResponse(
                    sakType = SakSammendragResponse.UFORE_SAKTYPE,
                    sakStatus = SakSammendragResponse.Status.LOPENDE,
                    fomDato = LocalDate.now().minusMonths(2),
                    tomDate = null,
                ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.KryssendeYtelseIPesys)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede hadde utvandring`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson(
                Utland(
                    innflyttingTilNorge = emptyList(),
                    utflyttingFraNorge = listOf(UtflyttingFraNorge("Sverige", LocalDate.now().minusMonths(2))),
                ),
            ).copy(
                doedsdato = OpplysningDTO(doedshendelseInternal.avdoedDoedsdato, null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarUtvandret)
    }

    @Test
    fun `Skal returnere kontrollpunkt hvis den avdoede har D-nummer`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.avdoedFnr,
                rolle = PersonRolle.AVDOED,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                doedsdato = OpplysningDTO(doedshendelseInternal.avdoedDoedsdato, null),
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of("69057949961"), null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AvdoedHarDNummer)
    }

    @Test
    fun `Skal opprette kontrollpunkt ved samtidig doedsfall`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = doedshendelseInternal.beroertFnr,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of(doedshendelseInternal.beroertFnr), null),
                doedsdato = OpplysningDTO(doedshendelseInternal.avdoedDoedsdato, null),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamtidigDoedsfall)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom vi ikke finner den andre forelderen`() {
        every {
            pdlTjenesterKlient.hentPdlModell(
                foedselsnummer = any(),
                rolle = PersonRolle.BARN,
                saktype = any(),
            )
        } returns
            mockPerson().copy(
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjon(
                            ansvarligeForeldre = emptyList(),
                            foreldre = listOf(KONTANT_FOT),
                            barn = emptyList(),
                        ),
                        null,
                    ),
            )

        val kontrollpunkter = kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet)
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt hvis alle sjekker er OK`() {
        kontrollpunktService.identifiserKontrollerpunkter(doedshendelseInternal) shouldBe emptyList()
    }
}
