package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.ALDER_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Companion.UFORE_SAKTYPE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.LOPENDE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.OPPRETTET
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.TIL_BEHANDLING
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Doedshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.PersonRolle

class DoedshendelseKontrollpunktService(
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val pesysKlient: PesysKlient,
) {
    fun identifiserKontrollerpunkter(hendelse: Doedshendelse): List<DoedshendelseKontrollpunkt> {
        val kontrollpunkter = mutableListOf<DoedshendelseKontrollpunkt>()

        with(hendelse) {
            kontrollpunkter.addNotNull(verifiserAvdoedDoedsdato(avdoedFnr))
            kontrollpunkter.addNotNull(verifiserKryssendeYtelse(hendelse))
        }

        return kontrollpunkter
    }

    private fun verifiserAvdoedDoedsdato(fnr: String): DoedshendelseKontrollpunkt? =
        when (pdlTjenesterKlient.hentPdlModell(fnr, PersonRolle.AVDOED, SakType.BARNEPENSJON).doedsdato) {
            null -> DoedshendelseKontrollpunkt.AvdoedLeverIPDL
            else -> null
        }

    private fun verifiserKryssendeYtelse(hendelse: Doedshendelse): DoedshendelseKontrollpunkt? =
        runBlocking {
            val kryssendeYtelser =
                pesysKlient.hentSaker(hendelse.avdoedFnr)
                    .filter { it.sakStatus in listOf(OPPRETTET, TIL_BEHANDLING, LOPENDE) }
                    .filter { it.sakType in listOf(UFORE_SAKTYPE, ALDER_SAKTYPE) }
                    .filter { it.fomDato == null || it.fomDato.isBefore(hendelse.avdoedDoedsdato) }
                    .any { it.tomDate == null || it.tomDate.isAfter(hendelse.avdoedDoedsdato) }

            when (kryssendeYtelser) {
                true -> DoedshendelseKontrollpunkt.KryssendeYtelseIPesys
                false -> null
            }
        }
}

private fun MutableList<DoedshendelseKontrollpunkt>.addNotNull(element: DoedshendelseKontrollpunkt?) {
    element?.let { add(it) }
}
