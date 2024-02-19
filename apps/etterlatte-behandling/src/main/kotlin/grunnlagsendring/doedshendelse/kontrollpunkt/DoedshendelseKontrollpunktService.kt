package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PersonRolle

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    fun identifiserKontrollerpunkter(hendelse: Doedshendelse): List<DoedshendelseKontrollpunkt> {
        val kontrollpunkter = mutableListOf<DoedshendelseKontrollpunkt>()

        with(hendelse) {
            kontrollpunkter.addNotNull(verifiserAvdoedDoedsdato(avdoedFnr))
        }

        return kontrollpunkter
    }

    private fun verifiserAvdoedDoedsdato(fnr: String): DoedshendelseKontrollpunkt? =
        when (pdlTjenesterKlient.hentPdlModell(fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON).doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL()
            else -> null
        }
}

fun MutableList<DoedshendelseKontrollpunkt>.addNotNull(element: DoedshendelseKontrollpunkt?) {
    element?.let { add(it) }
}
