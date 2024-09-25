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
        barn: PersonDTO,
        kontrollerSamtidigDoedsfall: Boolean = true,
    ): List<DoedshendelseKontrollpunkt> {
        val kontrollpunkt = mutableListOf<DoedshendelseKontrollpunkt>()
        kontrollerBarnOgHarBP(sak)?.let { kontrollpunkt.add(it) }
        if (kontrollerSamtidigDoedsfall) {
            kontrollerSamtidigDoedsfall(avdoed, hendelse, barn)?.let { kontrollpunkt.add(it) }
        }
        return kontrollpunkt
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
        barn: PersonDTO,
    ): DoedshendelseKontrollpunkt? =
        avdoed.doedsdato?.verdi?.let { avdoedDoedsdato ->
            val annenForelderFnr =
                barn.familieRelasjon
                    ?.verdi
                    ?.foreldre
                    ?.map { it.value }
                    ?.firstOrNull { it != hendelse.avdoedFnr }

            if (annenForelderFnr == null) {
                DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
            } else {
                pdlTjenesterKlient
                    .hentPdlModellFlereSaktyper(
                        foedselsnummer = annenForelderFnr,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = SakType.BARNEPENSJON,
                    ).doedsdato
                    ?.verdi
                    ?.let { annenForelderDoedsdato ->
                        if (annenForelderDoedsdato == avdoedDoedsdato) {
                            DoedshendelseKontrollpunkt.SamtidigDoedsfall
                        } else {
                            null
                        }
                    }
            }
        }
}
