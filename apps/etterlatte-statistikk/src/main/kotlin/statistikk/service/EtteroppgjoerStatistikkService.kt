package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRad
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRepository
import java.util.UUID

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

    fun hentNyesteRad(forbehandlingId: UUID): EtteroppgjoerRad? {
        val rader = etteroppgjoerRepository.hentEtteroppgjoerRaderForForbehandling(forbehandlingId)
        return rader.maxByOrNull { it.tekniskTid }
    }
}
