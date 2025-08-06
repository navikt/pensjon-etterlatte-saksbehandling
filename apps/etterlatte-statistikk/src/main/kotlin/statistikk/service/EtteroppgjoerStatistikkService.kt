package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.behandling.EtteroppgjoerForbehandlingDto
import no.nav.etterlatte.libs.common.behandling.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRad
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRepository

class EtteroppgjoerStatistikkService(
    private val etteroppgjoerRepository: EtteroppgjoerRepository,
) {
    fun registrerEtteroppgjoerHendelse(
        hendelse: EtteroppgjoerHendelseType,
        forbehandling: EtteroppgjoerForbehandlingDto,
        tekniskTid: Tidspunkt,
        resultat: BeregnetEtteroppgjoerResultatDto?,
    ): EtteroppgjoerRad {
        val etteroppgjoerRad =
            EtteroppgjoerRad.fraHendelseOgDto(
                hendelse = hendelse,
                forbehandlingDto = forbehandling,
                tekniskTid = tekniskTid,
                resultat = resultat,
            )

        etteroppgjoerRepository.lagreEtteroppgjoerRad(etteroppgjoerRad)
        return etteroppgjoerRad
    }
}
