import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.YearMonth
import java.util.UUID

const val SAKSBEHANDLER_1 = "saksbehandler1"

val saksbehandler = simpleSaksbehandler(ident = SAKSBEHANDLER_1)

/** Vedtak med utbetaling */
fun vedtakDto(
    behandlingId: UUID = UUID.randomUUID(),
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virk: YearMonth = YearMonth.now().minusMonths(1),
) = VedtakDto(
    id = 1L,
    behandlingId = behandlingId,
    status = VedtakStatus.ATTESTERT,
    sak = VedtakSak("Z123456", SakType.BARNEPENSJON, randomSakId()),
    type = if (behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) VedtakType.INNVILGELSE else VedtakType.ENDRING,
    vedtakFattet = VedtakFattet("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
    attestasjon = Attestasjon("Z00000", Enheter.defaultEnhet.enhetNr, Tidspunkt.now()),
    innhold =
        VedtakInnholdDto.VedtakBehandlingDto(
            virkningstidspunkt = YearMonth.now(),
            behandling = Behandling(behandlingType, behandlingId),
            utbetalingsperioder =
                listOf(
                    Utbetalingsperiode(
                        periode = Periode(virk, null),
                        beloep = 1000.toBigDecimal(),
                        type = UtbetalingsperiodeType.UTBETALING,
                        regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
                    ),
                ),
            opphoerFraOgMed = null,
        ),
)
