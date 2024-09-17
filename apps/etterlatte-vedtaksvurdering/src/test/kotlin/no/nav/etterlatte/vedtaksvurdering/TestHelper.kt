package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.ktor.token.simpleAttestant
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

const val FNR_1 = "28098208560"
const val FNR_2 = "04417103428"
const val SAKSBEHANDLER_1 = "saksbehandler1"
const val SAKSBEHANDLER_2 = "saksbehandler2"
val ENHET_1 = Enhet.STEINKJER
val ENHET_2 = Enhet.PORSGRUNN

val saksbehandler = simpleSaksbehandler(ident = SAKSBEHANDLER_1)
val attestant = simpleAttestant(ident = SAKSBEHANDLER_2)

fun opprettVedtak(
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    soeker: Folkeregisteridentifikator = SOEKER_FOEDSELSNUMMER,
    sakId: SakId = 1L,
    type: VedtakType = VedtakType.INNVILGELSE,
    behandlingId: UUID = UUID.randomUUID(),
    status: VedtakStatus = VedtakStatus.OPPRETTET,
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode(),
    avkorting: ObjectNode? = null,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    revurderingAarsak: Revurderingaarsak? = null,
) = OpprettVedtak(
    soeker = soeker,
    sakId = sakId,
    sakType = sakType,
    behandlingId = behandlingId,
    type = type,
    status = status,
    innhold =
        VedtakInnhold.Behandling(
            behandlingType = behandlingType,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            avkorting = avkorting,
            vilkaarsvurdering = vilkaarsvurdering,
            revurderingInfo = null,
            utbetalingsperioder =
                listOf(
                    Utbetalingsperiode(
                        id = 0,
                        periode = Periode(virkningstidspunkt, null),
                        beloep = BigDecimal.valueOf(100),
                        type = UtbetalingsperiodeType.UTBETALING,
                    ),
                ),
        ),
)

fun opprettVedtakTilbakekreving(
    soeker: Folkeregisteridentifikator = SOEKER_FOEDSELSNUMMER,
    sakId: SakId = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    tilbakekreving: ObjectNode = objectMapper.createObjectNode(),
) = OpprettVedtak(
    soeker = soeker,
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    type = VedtakType.TILBAKEKREVING,
    innhold = VedtakInnhold.Tilbakekreving(tilbakekreving = tilbakekreving),
)

fun opprettVedtakKlage(
    soeker: Folkeregisteridentifikator = SOEKER_FOEDSELSNUMMER,
    sakId: SakId = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    klage: ObjectNode = objectMapper.createObjectNode(),
) = OpprettVedtak(
    soeker = soeker,
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    type = VedtakType.AVVIST_KLAGE,
    innhold = VedtakInnhold.Klage(klage = klage),
)

fun vedtak(
    id: Long = 1L,
    virkningstidspunkt: YearMonth = YearMonth.of(2023, Month.JANUARY),
    sakId: SakId = 1L,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingId: UUID = UUID.randomUUID(),
    vilkaarsvurdering: ObjectNode? = objectMapper.createObjectNode(),
    beregning: ObjectNode? = objectMapper.createObjectNode(),
    avkorting: ObjectNode? = objectMapper.createObjectNode(),
    revurderingAarsak: Revurderingaarsak? = null,
    status: VedtakStatus = VedtakStatus.OPPRETTET,
    vedtakFattet: VedtakFattet? = null,
    utbetalingsperioder: List<Utbetalingsperiode>? = null,
) = Vedtak(
    id = id,
    status = status,
    soeker = SOEKER_FOEDSELSNUMMER,
    sakId = sakId,
    sakType = sakType,
    behandlingId = behandlingId,
    type = VedtakType.INNVILGELSE,
    vedtakFattet = vedtakFattet,
    innhold =
        VedtakInnhold.Behandling(
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            avkorting = avkorting,
            vilkaarsvurdering = vilkaarsvurdering,
            utbetalingsperioder =
                utbetalingsperioder
                    ?: listOf(
                        Utbetalingsperiode(
                            id = 1,
                            periode = Periode(virkningstidspunkt, null),
                            beloep = BigDecimal.valueOf(100),
                            type = UtbetalingsperiodeType.UTBETALING,
                        ),
                    ),
            revurderingAarsak = revurderingAarsak,
        ),
)

fun vedtakTilbakekreving(
    sakId: SakId = 1L,
    behandlingId: UUID = UUID.randomUUID(),
    tilbakekreving: ObjectNode = objectMapper.createObjectNode(),
    status: VedtakStatus = VedtakStatus.OPPRETTET,
    vedtakFattet: VedtakFattet? = null,
    attestasjon: Attestasjon? = null,
) = Vedtak(
    id = 1L,
    status = status,
    soeker = SOEKER_FOEDSELSNUMMER,
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    type = VedtakType.INNVILGELSE,
    innhold =
        VedtakInnhold.Tilbakekreving(tilbakekreving),
    vedtakFattet = vedtakFattet,
    attestasjon = attestasjon,
)

fun vedtakKlage(
    sakId: SakId = 142L,
    behandlingId: UUID = UUID.randomUUID(),
    klage: ObjectNode = objectMapper.createObjectNode(),
    status: VedtakStatus = VedtakStatus.OPPRETTET,
    vedtakFattet: VedtakFattet? = null,
) = Vedtak(
    id = Random.nextLong(10_000),
    status = status,
    soeker = SOEKER_FOEDSELSNUMMER,
    sakId = sakId,
    sakType = SakType.BARNEPENSJON,
    behandlingId = behandlingId,
    type = VedtakType.AVVIST_KLAGE,
    innhold = VedtakInnhold.Klage(klage),
    vedtakFattet = vedtakFattet,
)
