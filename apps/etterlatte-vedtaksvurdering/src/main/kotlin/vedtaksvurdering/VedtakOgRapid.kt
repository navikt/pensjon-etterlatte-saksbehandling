package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import java.util.UUID

data class VedtakOgRapid(val vedtak: Vedtak, val rapidInfo: RapidInfo)

data class RapidInfo(
    val vedtakhendelse: VedtakKafkaHendelseType,
    val vedtak: Vedtak,
    val tekniskTid: Tidspunkt,
    val behandlingId: UUID,
    val extraParams: Map<String, Any> = emptyMap(),
)
