package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak

internal class DoedshendelseKontrollpunktBarnService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val behandlingService: BehandlingService,
) {
    fun identifiser(
        hendelse: DoedshendelseInternal,
        avdoed: PersonDoedshendelseDto,
        sak: Sak?,
        barn: PersonDoedshendelseDto,
    ): List<DoedshendelseKontrollpunkt> =
        listOfNotNull(
            kontrollerBarnErSoektFor(sak),
            kontrollerSamtidigDoedsfall(avdoed, hendelse, barn),
        )

    private fun kontrollerBarnErSoektFor(sak: Sak?): DoedshendelseKontrollpunkt.BarnErSoektFor? {
        if (sak == null) {
            return null
        }
        val behandlingerForSak = behandlingService.hentBehandlingerForSak(sak.id).filter { it.sak.sakType == SakType.BARNEPENSJON }

        val harSoektBarnepensjon = behandlingerForSak.isNotEmpty()
        return if (harSoektBarnepensjon) {
            DoedshendelseKontrollpunkt.BarnErSoektFor(sak)
        } else {
            null
        }
    }

    private fun kontrollerSamtidigDoedsfall(
        avdoed: PersonDoedshendelseDto,
        hendelse: DoedshendelseInternal,
        barn: PersonDoedshendelseDto,
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
                    .hentPdlModellForSaktype(
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
