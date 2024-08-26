package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class OpprettVedtak(
    val soeker: Folkeregisteridentifikator,
    val sakId: SakId,
    val sakType: SakType,
    val behandlingId: UUID,
    val status: VedtakStatus = VedtakStatus.OPPRETTET,
    val type: VedtakType,
    val innhold: VedtakInnhold,
)

data class Vedtak(
    val id: Long,
    val soeker: Folkeregisteridentifikator,
    val sakId: SakId,
    val sakType: SakType,
    /** kan være ID-en til en behandling, klage eller tilbakekreving */
    val behandlingId: UUID,
    val status: VedtakStatus,
    val type: VedtakType,
    val vedtakFattet: VedtakFattet? = null,
    val attestasjon: Attestasjon? = null,
    val innhold: VedtakInnhold,
) {
    fun toDto(): VedtakDto =
        VedtakDto(
            id = id,
            behandlingId = behandlingId,
            status = status,
            sak = VedtakSak(soeker.value, sakType, sakId),
            type = type,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
            innhold =
                when (innhold) {
                    is VedtakInnhold.Behandling ->
                        VedtakInnholdDto.VedtakBehandlingDto(
                            virkningstidspunkt = innhold.virkningstidspunkt,
                            behandling =
                                Behandling(
                                    innhold.behandlingType,
                                    behandlingId,
                                    innhold.revurderingAarsak,
                                    innhold.revurderingInfo,
                                ),
                            utbetalingsperioder = innhold.utbetalingsperioder,
                            opphoerFraOgMed = innhold.opphoerFraOgMed,
                        )

                    is VedtakInnhold.Tilbakekreving ->
                        VedtakInnholdDto.VedtakTilbakekrevingDto(
                            tilbakekreving = innhold.tilbakekreving,
                        )

                    is VedtakInnhold.Klage -> {
                        VedtakInnholdDto.Klage(
                            klage = innhold.klage,
                        )
                    }
                },
        )

    fun underArbeid(): Boolean = status in listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT)
}

sealed interface VedtakInnhold {
    data class Behandling(
        val behandlingType: BehandlingType,
        val revurderingAarsak: Revurderingaarsak?,
        val virkningstidspunkt: YearMonth,
        val beregning: ObjectNode?,
        val avkorting: ObjectNode?,
        val vilkaarsvurdering: ObjectNode?,
        val utbetalingsperioder: List<Utbetalingsperiode>,
        val revurderingInfo: RevurderingInfo? = null,
        val opphoerFraOgMed: YearMonth? = null,
    ) : VedtakInnhold

    data class Tilbakekreving(
        val tilbakekreving: ObjectNode,
    ) : VedtakInnhold

    data class Klage(
        val klage: ObjectNode,
    ) : VedtakInnhold
}

data class LoependeYtelse(
    val erLoepende: Boolean,
    val underSamordning: Boolean,
    val dato: LocalDate,
    val behandlingId: UUID? = null,
    val sisteLoependeBehandlingId: UUID? = null,
)

class UgyldigAttestantException(
    ident: String,
) : IkkeTillattException(
        code = "ATTESTANT_OG_SAKSBEHANDLER_ER_SAMME_PERSON",
        detail = "Saksbehandler og attestant må være to forskjellige personer (ident=$ident)",
    )
