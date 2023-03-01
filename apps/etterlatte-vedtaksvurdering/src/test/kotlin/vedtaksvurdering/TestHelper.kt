package vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
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

val saksbehandler = Saksbehandler("token", SAKSBEHANDLER_1)
val attestant = Saksbehandler("token", SAKSBEHANDLER_2)

fun nyttVedtak(
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    sakId: Long = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode()
) = OpprettVedtak(
    soeker = Foedselsnummer.of(FNR_1),
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt = virkningstidspunkt,
    beregning = beregning,
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

fun opprettetVedtak(
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    sakId: Long = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode()
) = Vedtak(
    id = 1L,
    soeker = Foedselsnummer.of(FNR_1),
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    status = VedtakStatus.OPPRETTET,
    virkningstidspunkt = virkningstidspunkt,
    beregning = beregning,
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

fun fattetVedtak() = opprettetVedtak().copy(
    vedtakFattet = VedtakFattet(
        ansvarligSaksbehandler = SAKSBEHANDLER_1,
        ansvarligEnhet = ENHET_1,
        tidspunkt = Tidspunkt.now().toNorskTid()
    )
)