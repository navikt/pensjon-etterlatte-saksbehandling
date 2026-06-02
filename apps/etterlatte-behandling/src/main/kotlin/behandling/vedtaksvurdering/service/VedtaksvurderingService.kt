package no.nav.etterlatte.behandling.vedtaksvurdering.service

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.vedtaksvurdering.InnvilgetPeriode
import no.nav.etterlatte.behandling.vedtaksvurdering.LoependeYtelse
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtakstidslinje
import no.nav.etterlatte.behandling.vedtaksvurdering.VedtaksvurderingRepository
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.sjekk
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
    private val beregningKlient: BeregningKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentVedtak(vedtakId: Long): Vedtak? {
        logger.info("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)
    }

    fun hentVedtakMedBehandlingId(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }

    fun hentVedtakISak(sakId: SakId): List<Vedtak> = repository.hentVedtakForSak(sakId)

    fun sjekkOmVedtakErLoependePaaDato(
        sakId: SakId,
        dato: LocalDate,
    ): LoependeYtelse {
        logger.info("Sjekker om det finnes løpende vedtak for sak $sakId på dato $dato")
        val alleVedtakForSak =
            repository
                .hentVedtakForSak(sakId)
                .filter { it.vedtakFattet != null && it.attestasjon != null }
        return Vedtakstidslinje(alleVedtakForSak).harLoependeVedtakPaaEllerEtter(dato)
    }

    suspend fun sjekkOmVedtakErLoepende(
        sakId: SakId,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO {
        val loependeYtelse = sjekkOmVedtakErLoependePaaDato(sakId, dato)
        val harAktivSanksjon =
            if (loependeYtelse.erLoepende &&
                loependeYtelse.behandlingId != null &&
                loependeYtelse.sakType == SakType.OMSTILLINGSSTOENAD
            ) {
                val sanksjoner =
                    beregningKlient.hentSanksjoner(loependeYtelse.behandlingId, brukerTokenInfo) ?: emptyList()
                sanksjoner.any { it.fom <= YearMonth.from(dato) && it.tom == null }
            } else {
                false
            }
        return loependeYtelse.toDto(harAktivSanksjon)
    }

    private fun LoependeYtelse.toDto(harAktivSanksjon: Boolean) =
        LoependeYtelseDTO(
            erLoepende = erLoepende,
            underSamordning = underSamordning,
            dato = dato,
            behandlingId = behandlingId,
            sisteLoependeBehandlingId = sisteLoependeBehandlingId,
            harAktivSanksjon = harAktivSanksjon,
        )

    fun hentIverksatteVedtakISak(sakId: SakId): List<Vedtak> =
        repository
            .hentVedtakForSak(sakId)
            .filter { it.status == VedtakStatus.IVERKSATT }

    fun hentVedtak(fnr: Folkeregisteridentifikator): List<Vedtak> = repository.hentFerdigstilteVedtak(fnr)

    fun hentSakIdMedUtbetalingForInntektsaar(aar: Int): List<SakId> =
        repository.hentSakIdMedUtbetalingForInntektsaar(aar, SakType.OMSTILLINGSSTOENAD)

    fun harSakUtbetalingForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Boolean = repository.harSakUtbetalingForInntektsaar(sakId, inntektsaar, SakType.OMSTILLINGSSTOENAD)

    /**
     * Henter perioder hvor saken har vært innvilget.
     * Tar bare med vedtak som er attestert.
     */
    fun hentInnvilgedePerioder(sakId: SakId): List<InnvilgetPeriode> {
        val vedtak = hentVedtakISak(sakId).filter { it.type != VedtakType.AVSLAG }
        val tidslinje = Vedtakstidslinje(vedtak)
        return tidslinje.innvilgedePerioder()
    }

    /**
     * Henter perioder hvor saken har vært innvilget, fram til og med vedtaket for behandlingen som er angitt.
     * Tar bare med vedtak som er attestert.
     */
    fun hentInnvilgedePerioder(behandlingId: UUID): List<InnvilgetPeriode> {
        val vedtak =
            hentVedtakMedBehandlingId(behandlingId)
                ?: throw NotFoundException("Fant ikke vedtak med behandlingId=$behandlingId")
        sjekk(vedtak.attestasjon != null) { "Vedtaket må være attestert" }
        sjekk(vedtak.innhold is VedtakInnhold.Behandling) { "Vedtaket må ha behandling-innhold" }

        val vedtakTilOgMedDetAktuelle =
            hentVedtakISak(vedtak.sakId)
                .filter { it.attestasjon != null }
                .filterNot { it.attestasjon!!.tidspunkt.isAfter(vedtak.attestasjon.tidspunkt) }

        return Vedtakstidslinje(vedtakTilOgMedDetAktuelle).innvilgedePerioder()
    }
}
