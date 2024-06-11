package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import java.util.UUID

data class VedtakOgRapid(
    val vedtak: VedtakDto,
    val rapidInfo1: RapidInfo,
    val rapidInfo2: RapidInfo? = null,
)

data class RapidInfo(
    val vedtakhendelse: VedtakKafkaHendelseHendelseType,
    val vedtak: VedtakDto,
    val tekniskTid: Tidspunkt,
    val behandlingId: UUID,
    val extraParams: Map<String, Any> = emptyMap(),
)
