package vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.vedtaksvurdering.OpprettVedtak
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.*

const val FNR_1 = "11057523044"
const val SAKSBEHANDLER_1 = "saksbehandler1"
const val SAKSBEHANDLER_2 = "saksbehandler2"
const val ENHET_1 = "1234"
const val ENHET_2 = "4321"

val saksbehandler = Saksbehandler("token", SAKSBEHANDLER_1, null)
val attestant = Saksbehandler("token", SAKSBEHANDLER_2, null)

fun opprettVedtak(
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    sakId: Long = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode(),
    avkorting: ObjectNode? = objectMapper.createObjectNode()
) = OpprettVedtak(
    soeker = Folkeregisteridentifikator.of(FNR_1),
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt = virkningstidspunkt,
    type = VedtakType.INNVILGELSE,
    beregning = beregning,
    avkorting = avkorting,
    vilkaarsvurdering = vilkaarsvurdering,
    utbetalingsperioder = listOf(
        Utbetalingsperiode(
            id = 0,
            periode = Periode(virkningstidspunkt, null),
            beloep = BigDecimal.valueOf(100),
            type = UtbetalingsperiodeType.UTBETALING
        )
    )
)

fun vedtak(
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    sakId: Long = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode(),
    avkorting: ObjectNode? = objectMapper.createObjectNode()
) = Vedtak(
    id = 1L,
    status = VedtakStatus.OPPRETTET,
    soeker = Folkeregisteridentifikator.of(FNR_1),
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt = virkningstidspunkt,
    type = VedtakType.INNVILGELSE,
    beregning = beregning,
    avkorting = avkorting,
    vilkaarsvurdering = vilkaarsvurdering,
    utbetalingsperioder = listOf(
        Utbetalingsperiode(
            id = 1,
            periode = Periode(virkningstidspunkt, null),
            beloep = BigDecimal.valueOf(100),
            type = UtbetalingsperiodeType.UTBETALING
        )
    )
)