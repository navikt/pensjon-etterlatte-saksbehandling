package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.statistikk.database.AktivitetspliktRepo
import java.time.YearMonth

class AktivitetspliktService(
    private val aktivitetspliktRepo: AktivitetspliktRepo,
) {
    fun oppdatereVurderingAktivitetsplikt(aktivitetspliktDto: AktivitetspliktDto) {
        aktivitetspliktRepo.lagreAktivitetspliktForSak(aktivitetspliktDto)
    }

    fun mapRelevantStatistikkForMaanedOgSak(
        sakId: Long,
        statistikkmaaned: YearMonth,
    ): List<String?> {
        val aktivitet =
            aktivitetspliktRepo.hentAktivitetspliktForMaaned(sakId, statistikkmaaned)
                ?: return listOf("OMSTILLINGSPERIODE", null, null)
        TODO("Mapping kommer senere")
    }
}
