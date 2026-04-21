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
    // Noen som har intransaction lenger opp i fila som er avsluttet?
    fun opprettVedtak(opprettVedtak: OpprettVedtak): Vedtak // DONE

    fun oppdaterVedtak(oppdatertVedtak: Vedtak): Vedtak // DONE

    fun hentVedtak(vedtakId: Long): Vedtak? // DONE

    fun hentVedtak(behandlingId: UUID): Vedtak? // DONE

    fun hentVedtakForSak(sakId: SakId): List<Vedtak> // DONE

    fun fattVedtak( // DONE
        behandlingId: UUID,
        vedtakFattet: VedtakFattet,
    ): Vedtak

    fun attesterVedtak( // DONE
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
