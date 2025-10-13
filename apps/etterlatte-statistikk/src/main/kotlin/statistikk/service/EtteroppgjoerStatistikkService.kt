package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatistikkDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType.BEREGNET
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRad
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRepository
import java.util.UUID

class EtteroppgjoerStatistikkService(
    private val etteroppgjoerRepository: EtteroppgjoerRepository,
) {
    fun registrerEtteroppgjoerHendelse(
        hendelse: EtteroppgjoerHendelseType,
        statistikkDto: EtteroppgjoerForbehandlingStatistikkDto,
        tekniskTid: Tidspunkt,
        resultat: BeregnetEtteroppgjoerResultatDto?,
    ): EtteroppgjoerRad {
        val etteroppgjoerRad =
            EtteroppgjoerRad.fraHendelseOgDto(
                hendelse = hendelse,
                statistikkDto = statistikkDto,
                tekniskTid = tekniskTid,
                resultat = resultat,
            )

        etteroppgjoerRepository.lagreEtteroppgjoerRad(etteroppgjoerRad)
        return etteroppgjoerRad
    }

    fun hentNyesteRad(
        forbehandlingId: UUID,
        filter: (EtteroppgjoerRad) -> Boolean = { true },
    ): EtteroppgjoerRad? {
        val rader = etteroppgjoerRepository.hentEtteroppgjoerRaderForForbehandling(forbehandlingId)
        return rader
            .filter { filter(it) }
            .maxByOrNull { it.tekniskTid }
    }

    fun hentNyesteBeregnedeResultat(forbehandlingId: UUID): EtteroppgjoerResultatType? =
        hentNyesteRad(forbehandlingId) { it.hendelse == BEREGNET }?.resultatType
}
