package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import java.util.UUID

data class VedtakOgRapid(val vedtak: VedtakDto, val rapidInfo: Collection<RapidInfo>)

data class RapidInfo(
    val vedtakhendelse: VedtakKafkaHendelseType,
    val vedtak: VedtakDto,
    val tekniskTid: Tidspunkt,
    val behandlingId: UUID,
    val extraParams: Map<String, Any> = emptyMap(),
)
