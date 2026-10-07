package no.nav.etterlatte.behandling.vedtaksvurdering

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

class Vedtakstidslinje(
    vedtak: List<Vedtak>,
) {
    private val sakType = vedtak.firstOrNull()?.sakType

    private val attesterteBehandlingVedtak =
        vedtak
            .filter { it.innhold is VedtakInnhold.Behandling }
            .filter { it.attestasjon != null }

    private val iverksatteBehandlingVedtak =
        attesterteBehandlingVedtak
            .filter { it.status == VedtakStatus.IVERKSATT }

    fun harLoependeVedtakPaaEllerEtter(dato: LocalDate): LoependeYtelse {
        val erUnderSamordning =
            attesterteBehandlingVedtak.any { listOf(VedtakStatus.TIL_SAMORDNING, VedtakStatus.SAMORDNET).contains(it.status) }

        if (iverksatteBehandlingVedtak.isEmpty()) return LoependeYtelse(false, erUnderSamordning, dato, sakType)

        val senesteVedtakPaaDato = hentSenesteVedtakSomKanLoepePaaDato(dato)
        val erLoepende = senesteVedtakPaaDato?.type in listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING)
        return LoependeYtelse(
            erLoepende = erLoepende,
            underSamordning = erUnderSamordning,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato,
            sakType = sakType,
            behandlingId = if (erLoepende) senesteVedtakPaaDato!!.behandlingId else null,
            sisteLoependeBehandlingId =
                if (erLoepende) {
                    sammenstill(YearMonth.from(dato))
                        .filter { it.type != VedtakType.OPPHOER }
                        .maxByOrNull { it.attestasjon?.tidspunkt!! }
                        ?.behandlingId
                } else {
                    null
                },
        )
    }

    /**
     * Oppretter en kontinuerlig, "gjeldende" tidslinje med vedtak og underliggende perioder.
     * Returnerer en liste av de vedtakene som er gjeldenede på eller etter fomDato.
     */
    fun sammenstill(fomDato: YearMonth): List<Vedtak> {
        val vedtakByVirkningsdato = mutableMapOf<Periode, Vedtak>()
        var currentVirkningstidspunkt: YearMonth? = null

        for (currentVedtak in attesterteBehandlingVedtak
            .filter { it.attestasjon != null }
            .sortedByDescending { it.attestasjon!!.tidspunkt }) {
            with(currentVedtak) {
                if (currentVirkningstidspunkt?.isAfter(virkningstidspunkt) != false) {
                    val periode = Periode(virkningstidspunkt, currentVirkningstidspunkt?.minusMonths(1))
                    vedtakByVirkningsdato[periode] = currentVedtak
                    currentVirkningstidspunkt = virkningstidspunkt
                }
            }
        }

        return vedtakByVirkningsdato
            .filter { (periode, _) -> periode.tom == null || periode.tom == fomDato || periode.tom!!.isAfter(fomDato) }
            .map { (periode, vedtak) -> vedtak.kopier(periode) }
            .sortedBy { it.virkningstidspunkt }
    }

    fun innvilgedePerioder(): List<InnvilgetPeriode> {
        if (attesterteBehandlingVedtak.isEmpty()) {
            return emptyList()
        }
        val foersteVirk = attesterteBehandlingVedtak.minOf { it.virkningstidspunkt }
        val sammenstilt = sammenstill(foersteVirk)
        val perioder =
            sammenstilt
                .flatMapIndexed { index, vedtak ->
                    val sisteGjeldendeMaaned =
                        listOfNotNull(
                            sammenstilt.getOrNull(index + 1)?.virkningstidspunkt?.minusMonths(1),
                            vedtak.opphoer()?.minusMonths(1),
                        ).minOrNull()
                    (vedtak.innhold as VedtakInnhold.Behandling)
                        .utbetalingsperioder
                        // Også 0 kr etter avkorting eller sanksjon er en innvilget periode.
                        .filter { it.type == UtbetalingsperiodeType.UTBETALING }
                        .mapNotNull { utbetaling ->
                            val fom = maxOf(vedtak.virkningstidspunkt, utbetaling.periode.fom)
                            val tom = listOfNotNull(sisteGjeldendeMaaned, utbetaling.periode.tom).minOrNull()
                            if (tom != null && tom < fom) {
                                null
                            } else {
                                InnvilgetPeriode(Periode(fom, tom), listOf(vedtak))
                            }
                        }
                }.sortedBy { it.periode.fom }

        val innvilgedePerioder = mutableListOf<InnvilgetPeriode>()
        perioder.forEach { periode ->
            val forrige = innvilgedePerioder.lastOrNull()
            val forrigeTom = forrige?.periode?.tom
            if (forrige != null && (forrigeTom == null || periode.periode.fom <= forrigeTom.plusMonths(1))) {
                val tom = periode.periode.tom
                innvilgedePerioder[innvilgedePerioder.lastIndex] =
                    InnvilgetPeriode(
                        Periode(forrige.periode.fom, if (forrigeTom == null || tom == null) null else maxOf(forrigeTom, tom)),
                        (forrige.vedtak + periode.vedtak).distinctBy { it.id },
                    )
            } else {
                innvilgedePerioder.add(periode)
            }
        }
        return innvilgedePerioder
    }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato =
            (iverksatteBehandlingVedtak.minBy { it.virkningstidspunkt }.innhold as VedtakInnhold.Behandling)
                .virkningstidspunkt
                .atDay(1)
        return maxOf(foersteVirkningsdato, fraDato)
    }

    private fun hentSenesteVedtakSomKanLoepePaaDato(dato: LocalDate): Vedtak? =
        iverksatteBehandlingVedtak
            .filter { it.type.vanligBehandling }
            .filter {
                it.virkningstidspunkt
                    .atDay(1)
                    .isAfter(foersteMuligeVedtaksdag(dato))
                    .not()
            }.maxByOrNull { it.attestasjon?.tidspunkt!! }
            ?.let { senesteVedtak ->
                return if (senesteVedtak.opphoerFraOgMed == null || senesteVedtak.opphoerFraOgMed!!.atDay(1) > dato) {
                    senesteVedtak
                } else {
                    null
                }
            }

    // Opprette kopier av data class-struktur, med (potensielt) endret liste med utbetalingsperioder
    private fun Vedtak.kopier(gyldighetsperiode: Periode): Vedtak =
        copy(
            id = id,
            soeker = soeker,
            sakId = sakId,
            sakType = sakType,
            behandlingId = behandlingId,
            status = status,
            type = type,
            vedtakFattet = vedtakFattet,
            attestasjon = attestasjon,
            innhold =
                when (innhold) {
                    is VedtakInnhold.Behandling -> innhold.kopier(gyldighetsperiode)

                    is VedtakInnhold.Tilbakekreving, is VedtakInnhold.Klage -> throw UgyldigForespoerselException(
                        code = "VEDTAKSINNHOLD_IKKE_STOETTET",
                        detail = "Skal ikke benyttes på annet enn vedtak med behandlingsinnhold",
                    )
                },
        )

    private fun VedtakInnhold.Behandling.kopier(gyldighetsperiode: Periode): VedtakInnhold.Behandling =
        copy(
            behandlingType = behandlingType,
            revurderingAarsak = revurderingAarsak,
            virkningstidspunkt = virkningstidspunkt,
            beregning = beregning,
            avkorting = avkorting,
            vilkaarsvurdering = vilkaarsvurdering,
            utbetalingsperioder = filtrerUtbetalingsperioder(gyldighetsperiode),
            revurderingInfo = revurderingInfo,
        )

    /**
     * 2 oppgaver:
     * - fjerne perioder som er utenfor vedtakets tidsrom (kan være endret pga revurdering tilbake i tid osv)
     * - lukke siste perioder dersom vedtakets gyldighetsperiode også er lukket, dvs det finnes vedtak med senere virkningstidspunkt
     */
    private fun VedtakInnhold.Behandling.filtrerUtbetalingsperioder(gyldighetsperiode: Periode): List<Utbetalingsperiode> =
        utbetalingsperioder
            .filter {
                gyldighetsperiode.tom?.let { tom -> !it.periode.fom.isAfter(tom) } ?: true
            }.map {
                if (it.periode.tom == null || gyldighetsperiode.tom?.isBefore(it.periode.tom) == true) {
                    it.copy(
                        id = it.id,
                        periode = Periode(it.periode.fom, gyldighetsperiode.tom),
                        beloep = it.beloep,
                        type = it.type,
                    )
                } else {
                    it
                }
            }

    private val Vedtak.virkningstidspunkt: YearMonth
        get() = (this.innhold as VedtakInnhold.Behandling).virkningstidspunkt

    private val Vedtak.opphoerFraOgMed: YearMonth?
        get() = (this.innhold as VedtakInnhold.Behandling).opphoerFraOgMed

    private fun Vedtak.opphoer(): YearMonth? =
        when (this.type) {
            VedtakType.OPPHOER -> this.virkningstidspunkt
            else -> this.opphoerFraOgMed
        }
}

data class InnvilgetPeriode(
    val periode: Periode,
    val vedtak: List<Vedtak>,
) {
    fun tilDto(): InnvilgetPeriodeDto =
        InnvilgetPeriodeDto(
            periode = this.periode,
            vedtak = this.vedtak.map(Vedtak::toDto),
        )
}
