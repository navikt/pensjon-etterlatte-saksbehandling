package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import java.time.LocalDate

class Samordningsvedtak(
    val samordningVedtakId: Long,
    val fagsystem: String,
    val saksId: Long,
    val saksKode: String,
    val vedtakId: Long,
    val vedtakstatusKode: String,
    val etterbetaling: Boolean,
    val utvidetSamordningsfrist: Boolean,
    val virkningFom: LocalDate,
    val virkningTom: LocalDate?,
    val versjon: Long,
    val samordningsmeldinger: List<Samordningsmelding> = emptyList(),
)

class Samordningsmelding(
    val samId: Long,
    val meldingstatusKode: String,
    val tssEksternId: String,
    val sendtDato: LocalDate,
    val svartDato: LocalDate?,
    val purretDato: LocalDate?,
    val refusjonskrav: Boolean,
    val versjon: Long,
)
