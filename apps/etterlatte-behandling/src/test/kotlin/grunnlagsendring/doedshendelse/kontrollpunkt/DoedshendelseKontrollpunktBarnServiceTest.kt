package grunnlagsendring.doedshendelse.kontrollpunkt

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.LITE_BARN
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktBarnService
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.mockPerson
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DoedshendelseKontrollpunktBarnServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val kontrollpunktService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)

    @Test
    fun `Skal opprette kontrollpunkt ved samtidig doedsfall`() {
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = KONTANT_FOT.value,
                rolle = PersonRolle.AVDOED,
                saktype = SakType.BARNEPENSJON,
            )
        } returns avdoed
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende.copy(doedsdato = OpplysningDTO(doedsdato, null))

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barnet)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.SamtidigDoedsfall)
    }

    @Test
    fun `Skal opprette kontrollpunkt dersom vi ikke finner den andre forelderen`() {
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = KONTANT_FOT.value,
                rolle = PersonRolle.AVDOED,
                saktype = SakType.BARNEPENSJON,
            )
        } returns avdoed
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende.copy(doedsdato = OpplysningDTO(doedsdato, null))
        val barnet =
            barnet.copy(
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

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, null, barnet)

        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet)
    }

    @Test
    fun `Skal opprette kontrollpunkt ved iverksatt vedtak`() {
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = KONTANT_FOT.value,
                rolle = PersonRolle.AVDOED,
                saktype = SakType.BARNEPENSJON,
            )
        } returns avdoed
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende
        every {
            behandlingService.hentSisteIverksatte(sak.id)
        } returns foerstegangsbehandling(sakId = sak.id, status = BehandlingStatus.IVERKSATT)

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, sak, barnet)
        kontrollpunkter shouldContainExactly listOf(DoedshendelseKontrollpunkt.BarnHarBarnepensjon(sak))
    }

    @Test
    fun `Skal ikke opprette kontrollpunkt ved dersom det kun eksisterer en sak`() {
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = KONTANT_FOT.value,
                rolle = PersonRolle.AVDOED,
                saktype = SakType.BARNEPENSJON,
            )
        } returns avdoed
        every {
            pdlTjenesterKlient.hentPdlModellForSaktype(
                foedselsnummer = JOVIAL_LAMA.value,
                rolle = PersonRolle.GJENLEVENDE,
                saktype = SakType.BARNEPENSJON,
            )
        } returns gjenlevende
        every {
            behandlingService.hentSisteIverksatte(sak.id)
        } returns null

        val kontrollpunkter = kontrollpunktService.identifiser(doedshendelse, avdoed, sak, barnet)
        kontrollpunkter shouldBe emptyList()
    }

    companion object {
        private val doedsdato: LocalDate = LocalDate.now()
        private val doedshendelse =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = KONTANT_FOT.value,
                avdoedDoedsdato = doedsdato,
                beroertFnr = LITE_BARN.value,
                relasjon = Relasjon.BARN,
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
        private val barnet =
            mockPerson().copy(
                foedselsnummer = OpplysningDTO(JOVIAL_LAMA, null),
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
        private val sak =
            Sak(
                ident = doedshendelse.beroertFnr,
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.defaultEnhet.enhetNr,
            )
    }
}
