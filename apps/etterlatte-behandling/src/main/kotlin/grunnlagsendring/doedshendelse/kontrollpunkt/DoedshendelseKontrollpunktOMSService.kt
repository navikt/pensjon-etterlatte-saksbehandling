package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.LOPENDE
import no.nav.etterlatte.common.klienter.SakSammendragResponse.Status.TIL_BEHANDLING
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.safeYearsBetween
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import kotlin.math.absoluteValue

internal class DoedshendelseKontrollpunktOMSService(
    private val pesysKlient: PesysKlient,
    private val behandlingService: BehandlingService,
) {
    fun identifiser(
        hendelse: DoedshendelseInternal,
        sak: Sak?,
        eps: PersonDoedshendelseDto,
        avdoed: PersonDoedshendelseDto,
        bruker: BrukerTokenInfo,
    ): List<DoedshendelseKontrollpunkt> =
        listOfNotNull(
            kontrollerKryssendeYtelseBeroert(hendelse, bruker),
            kontrollerEksisterendeBehandling(sak),
            kontrollerBeroertErDoed(eps),
            kontrollerBeroertFylt67Aar(eps, avdoed),
        )

    private fun kontrollerKryssendeYtelseBeroert(
        hendelse: DoedshendelseInternal,
        bruker: BrukerTokenInfo,
    ): DoedshendelseKontrollpunkt? {
        if (hendelse.sakTypeForEpsEllerBarn() == SakType.BARNEPENSJON) {
            return null
        }
        return runBlocking {
            val kryssendeYtelser =
                pesysKlient
                    .hentSaker(hendelse.beroertFnr, bruker)
                    .filter {
                        it.sakStatus in listOf(TIL_BEHANDLING, LOPENDE)
                    }.filter { it.sakType in listOf(SakSammendragResponse.UFORE_SAKTYPE, SakSammendragResponse.ALDER_SAKTYPE) }
                    .filter { it.fomDato == null || it.fomDato.isBefore(hendelse.avdoedDoedsdato) }
                    .any { it.tomDate == null || it.tomDate.isAfter(hendelse.avdoedDoedsdato) }

            when (kryssendeYtelser) {
                true -> DoedshendelseKontrollpunkt.KryssendeYtelseIPesysEps
                false -> null
            }
        }
    }

    private fun kontrollerEksisterendeBehandling(sak: Sak?): DoedshendelseKontrollpunkt? =
        sak?.let {
            val harSoekt =
                behandlingService.hentBehandlingerForSak(sak.id).any { it.sak.sakType == SakType.OMSTILLINGSSTOENAD }
            if (harSoekt) {
                DoedshendelseKontrollpunkt.EpsHarSoektOMS(sak)
            } else {
                null
            }
        }

    private fun kontrollerBeroertFylt67Aar(
        eps: PersonDoedshendelseDto,
        avdoed: PersonDoedshendelseDto,
    ): DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon? =
        if (eps.foedselsdato != null) {
            val foedselsdato = eps.foedselsdato?.verdi
            val maanedenEtterDoedsdato =
                avdoed.doedsdato!!
                    .verdi
                    .plusMonths(1)
                    .withDayOfMonth(1)
            val alder = safeYearsBetween(foedselsdato, maanedenEtterDoedsdato).absoluteValue
            val alderspensjonAar = 67
            if (alder >= alderspensjonAar) {
                DoedshendelseKontrollpunkt.EpsKanHaAlderspensjon
            } else {
                null
            }
        } else {
            null
        }

    private fun kontrollerBeroertErDoed(eps: PersonDoedshendelseDto): DoedshendelseKontrollpunkt.EpsHarDoedsdato? =
        if (eps.doedsdato != null) {
            DoedshendelseKontrollpunkt.EpsHarDoedsdato
        } else {
            null
        }
}
