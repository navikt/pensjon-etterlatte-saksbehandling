package no.nav.etterlatte.vedtak

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

class VedtakService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
) {
    suspend fun hentVedtak(fnr: Folkeregisteridentifikator): VedtakForEksterntDto {
        val vedtak = vedtaksvurderingKlient.hentVedtak(fnr)
        return VedtakForEksterntDto(
            vedtak = vedtak.filter { it.type.vanligBehandling }.map { it.toEksternDto() },
        )
    }
}
