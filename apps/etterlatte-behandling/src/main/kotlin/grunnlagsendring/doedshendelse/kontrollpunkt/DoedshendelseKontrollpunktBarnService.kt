package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak

internal class DoedshendelseKontrollpunktBarnService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val behandlingService: BehandlingService,
) {
    fun identifiser(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDTO,
        sak: Sak?,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            kontrollerBarnOgHarBP(sak),
            kontrollerSamtidigDoedsfall(avdoed, hendelse),
        )
    }

    private fun kontrollerBarnOgHarBP(sak: Sak?): DoedshendelseKontrollpunkt.BarnHarBarnepensjon? {
        if (sak == null) {
            return null
        }

        val sisteIverksatteBehandling = behandlingService.hentSisteIverksatte(sak.id)

        val harBarnepensjon = sisteIverksatteBehandling != null && sisteIverksatteBehandling.sak.sakType == SakType.BARNEPENSJON
        return if (harBarnepensjon) {
            DoedshendelseKontrollpunkt.BarnHarBarnepensjon(sak)
        } else {
            null
        }
    }

    private fun kontrollerSamtidigDoedsfall(
        avdoed: PersonDTO,
        hendelse: DoedshendelseInternal,
    ): DoedshendelseKontrollpunkt? =
        try {
            avdoed.doedsdato?.verdi?.let { avdoedDoedsdato ->
                val annenForelderFnr =
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = hendelse.beroertFnr,
                        rolle = PersonRolle.BARN,
                        saktype = SakType.BARNEPENSJON,
                    ).familieRelasjon?.verdi?.foreldre
                        ?.map { it.value }
                        ?.firstOrNull { it != hendelse.avdoedFnr }

                if (annenForelderFnr == null) {
                    DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
                } else {
                    pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                        foedselsnummer = annenForelderFnr,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = SakType.BARNEPENSJON,
                    )
                        .doedsdato?.verdi?.let { annenForelderDoedsdato ->
                            if (annenForelderDoedsdato == avdoedDoedsdato) {
                                DoedshendelseKontrollpunkt.SamtidigDoedsfall
                            } else {
                                null
                            }
                        }
                }
            }
        } catch (e: Exception) {
            DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
        }
}
