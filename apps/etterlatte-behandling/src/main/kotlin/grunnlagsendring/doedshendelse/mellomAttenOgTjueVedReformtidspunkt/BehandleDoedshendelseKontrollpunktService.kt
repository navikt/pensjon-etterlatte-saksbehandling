package no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harAktivAdresse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktAvdoedService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktBarnService
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class BehandleDoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val sakService: SakService,
    private val pesysKlient: PesysKlient,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val kontrollpunktAvdoedService = DoedshendelseKontrollpunktAvdoedService()
    private val kontrollpunktBarnService = DoedshendelseKontrollpunktBarnService(pdlTjenesterKlient, behandlingService)

    fun identifiserKontrollerpunkter(hendelse: DoedshendelseInternal): List<DoedshendelseKontrollpunkt> {
        val (sak, avdoed, barn) = hentDataForBeroert(hendelse, PersonRolle.BARN)

        return if (avdoed.doedsdato == null) {
            listOf(DoedshendelseKontrollpunkt.AvdoedLeverIPDL)
        } else {
            val barnKontrollpunkter = kontrollpunktBarnService.identifiser(hendelse, avdoed, sak, barn)
            val avdoedKontrollpunkter = kontrollpunktAvdoedService.identifiser(avdoed)
            val fellesKontrollpunkter = fellesKontrollpunkter(avdoed, barn)

            barnKontrollpunkter + avdoedKontrollpunkter + fellesKontrollpunkter
        }
    }

    private fun hentDataForBeroert(
        hendelse: DoedshendelseInternal,
        beroert: PersonRolle,
    ): Triple<Sak?, PersonDTO, PersonDTO> {
        val sakType = hendelse.sakTypeForEpsEllerBarn()
        val sak = sakService.finnSak(hendelse.beroertFnr, sakType)
        val avdoed = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.avdoedFnr, PersonRolle.AVDOED, sakType)
        val gjenlevende = pdlTjenesterKlient.hentPdlModellFlereSaktyper(hendelse.beroertFnr, beroert, hendelse.sakTypeForEpsEllerBarn())

        return Triple(sak, avdoed, gjenlevende)
    }

    private fun fellesKontrollpunkter(
        avdoed: PersonDTO,
        gjenlevende: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        val adresseKontrollpunkt = kontrollerAktivAdresse(gjenlevende)
        val haandtertAvPesys = behandletAvPesys(avdoed, gjenlevende)

        return listOfNotNull(adresseKontrollpunkt, haandtertAvPesys)
    }

    private fun kontrollerAktivAdresse(gjenlevende: PersonDTO): DoedshendelseKontrollpunkt? =
        when (harAktivAdresse(gjenlevende)) {
            true -> null
            false -> DoedshendelseKontrollpunkt.GjenlevendeManglerAdresse
        }

    private fun behandletAvPesys(
        avdoed: PersonDTO,
        gjenlevende: PersonDTO,
    ): DoedshendelseKontrollpunkt.TilstoetendeBehandletIPesys? =
        runBlocking {
            try {
                if (pesysKlient.erTilstoetendeBehandlet(gjenlevende.foedselsnummer.verdi.value, avdoed.doedsdato!!.verdi)) {
                    DoedshendelseKontrollpunkt.TilstoetendeBehandletIPesys
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.error("Feil ved kall til Pesys for å sjekke om tilstøtt har blitt behandlet", e)
                null // TODO: Fjern try-catch når vi har testet at grensesnittet fungerer som det skal mot Pesys
            }
        }
}
