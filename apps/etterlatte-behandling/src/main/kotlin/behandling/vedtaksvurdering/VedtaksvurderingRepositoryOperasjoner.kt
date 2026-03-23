package no.nav.etterlatte.behandling.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.AvkortetYtelsePeriode
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

interface VedtaksvurderingRepositoryOperasjoner {
    fun opprettVedtak(opprettVedtak: OpprettVedtak): Vedtak

    fun oppdaterVedtak(oppdatertVedtak: Vedtak): Vedtak

    fun hentVedtak(vedtakId: Long): Vedtak?

    fun hentVedtak(behandlingId: UUID): Vedtak?

    fun hentVedtakForSak(sakId: SakId): List<Vedtak>

    fun fattVedtak(
        behandlingId: UUID,
        vedtakFattet: VedtakFattet,
    ): Vedtak

    fun attesterVedtak(
        behandlingId: UUID,
        attestasjon: Attestasjon,
    ): Vedtak

    fun underkjennVedtak(behandlingId: UUID): Vedtak

    fun tilSamordningVedtak(behandlingId: UUID): Vedtak

    fun samordnetVedtak(behandlingId: UUID): Vedtak

    fun iverksattVedtak(behandlingId: UUID): Vedtak

    fun tilbakestillIkkeIverksatteVedtak(behandlingId: UUID): Vedtak?

    fun hentFerdigstilteVedtak(
        fnr: Folkeregisteridentifikator,
        sakType: SakType? = null,
    ): List<Vedtak>

    fun hentSakIdMedUtbetalingForInntektsaar(
        inntektsaar: Int,
        sakType: SakType? = null,
    ): List<SakId>

    fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
        sakType: SakType,
    ): Boolean

    fun hentAvkortetYtelsePerioder(vedtakIds: Set<Long>): List<AvkortetYtelsePeriode>

    fun lagreManuellBehandlingSamordningsmelding(
        oppdatering: OppdaterSamordningsmelding,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    )

    fun slettManuellBehandlingSamordningsmelding(samId: Long)

    fun tilbakestillTilbakekrevingsvedtak(tilbakekrevingId: UUID)
}
