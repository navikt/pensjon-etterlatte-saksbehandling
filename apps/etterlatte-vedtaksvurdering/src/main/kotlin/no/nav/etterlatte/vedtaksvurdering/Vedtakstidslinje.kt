package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDate
import java.time.YearMonth

class Vedtakstidslinje(
    private val vedtak: List<Vedtak>,
) {
    private val iverksatteVedtak = hentIverksatteVedtak()

    fun harLoependeVedtakPaaEllerEtter(dato: LocalDate): LoependeYtelse {
        val erUnderSamordning =
            vedtak.any { listOf(VedtakStatus.TIL_SAMORDNING, VedtakStatus.SAMORDNET).contains(it.status) }

        if (iverksatteVedtak.isEmpty()) return LoependeYtelse(false, erUnderSamordning, dato)

        val senesteVedtakPaaDato = hentSenesteVedtakSomKanLoepePaaDato(dato)
        val erLoepende = senesteVedtakPaaDato?.type in listOf(VedtakType.INNVILGELSE, VedtakType.ENDRING)
        return LoependeYtelse(
            erLoepende = erLoepende,
            underSamordning = erUnderSamordning,
            dato = if (erLoepende) foersteMuligeVedtaksdag(dato) else dato,
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

    private fun hentSenesteVedtakSomKanLoepePaaDato(dato: LocalDate): Vedtak? =
        iverksatteVedtak
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

    private fun hentIverksatteVedtak(): List<Vedtak> = vedtak.filter { it.status === VedtakStatus.IVERKSATT }

    private fun foersteMuligeVedtaksdag(fraDato: LocalDate): LocalDate {
        val foersteVirkningsdato =
            (iverksatteVedtak.minBy { it.virkningstidspunkt }.innhold as VedtakInnhold.Behandling)
                .virkningstidspunkt
                .atDay(1)
        return maxOf(foersteVirkningsdato, fraDato)
    }

    /**
     * Opprette en kontinuerlig, "gjeldende" tidslinje med vedtak og underliggende perioder
     */
    fun sammenstill(fomDato: YearMonth): List<Vedtak> {
        val vedtakByVirkningsdato = mutableMapOf<Periode, Vedtak>()
        var currentVirkningstidspunkt: YearMonth? = null

        for (currentVedtak in vedtak
            .filter { it.innhold is VedtakInnhold.Behandling }
            .filter { it.vedtakFattet != null }
            .sortedByDescending { it.vedtakFattet!!.tidspunkt }) {
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
        val foersteVirk = vedtak.minOf { it.virkningstidspunkt }
        val sammenstilt = sammenstill(foersteVirk)
        if (sammenstilt.size == 1) {
            // Håndter special case
            val vedtak = sammenstilt.single()
            return listOf(
                InnvilgetPeriode(Periode(vedtak.virkningstidspunkt, vedtak.opphoerFraOgMed?.minusMonths(1)), vedtak = listOf(vedtak)),
            )
        }

        // Enhver overgang mellom vedtak betyr at vi kan potensielt hoppe ut av en innvilget periode
        val innvilgedePerioder = mutableListOf<InnvilgetPeriode>()
        var startPaaEksisterendePeriode: Vedtak? = null
        val vedtakMedIPerioden = mutableListOf<Vedtak>()

        val naavaerendeOgNesteVedtakListe = sammenstilt.zipWithNext()
        naavaerendeOgNesteVedtakListe.forEach { (naavaerendeVedtak, nesteVedtak) ->
            // Hvis vi ikke har en åpen periode, start med nåværende vedtak
            if (startPaaEksisterendePeriode == null) {
                startPaaEksisterendePeriode = naavaerendeVedtak
            }

            // Det kan være en periode uten ytelse mellom nåværende vedtak sin periode, og neste vedtak
            // Dette skjer når: nåværende vedtak har et opphør / er et opphør og neste vedtak har virk strengt etter opphør fom
            val naavaerendeVedtakOpphoererDato = naavaerendeVedtak.opphoer()
            if (naavaerendeVedtakOpphoererDato != null && naavaerendeVedtakOpphoererDato < nesteVedtak.virkningstidspunkt) {
                innvilgedePerioder.add(
                    InnvilgetPeriode(
                        Periode(startPaaEksisterendePeriode!!.virkningstidspunkt, naavaerendeVedtakOpphoererDato.minusMonths(1)),
                        vedtakMedIPerioden + naavaerendeVedtak,
                    ),
                )
                startPaaEksisterendePeriode = null
                vedtakMedIPerioden.clear()
            } else {
                vedtakMedIPerioden.add(naavaerendeVedtak)
            }
        }
        val opphoerSisteVedtak = sammenstilt.last().opphoer()

        // Håndter siste vedtak - dette er enten en fortsettelse på perioden til forrige vedtak eller en egen periode

        when (val eksisterendePeriode = startPaaEksisterendePeriode) {
            // vi har ikke en eksisterende periode
            null ->
                innvilgedePerioder.add(
                    InnvilgetPeriode(
                        Periode(sammenstilt.last().virkningstidspunkt, opphoerSisteVedtak?.minusMonths(1)),
                        vedtak = listOf(sammenstilt.last()),
                    ),
                )
            else ->
                innvilgedePerioder.add(
                    InnvilgetPeriode(
                        Periode(eksisterendePeriode.virkningstidspunkt, opphoerSisteVedtak?.minusMonths(1)),
                        vedtak = vedtakMedIPerioden + sammenstilt.last(),
                    ),
                )
        }

        return innvilgedePerioder
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
