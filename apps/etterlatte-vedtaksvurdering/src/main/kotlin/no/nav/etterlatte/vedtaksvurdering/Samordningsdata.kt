package no.nav.etterlatte.vedtaksvurdering

import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDate
import java.util.UUID

data class Samordningsvedtak(
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

data class Samordningsmelding(
    val samId: Long,
    val meldingstatusKode: String,
    val tpNr: String,
    val tpNavn: String,
    val sendtDato: LocalDate,
    val svartDato: LocalDate?,
    val purretDato: LocalDate?,
    val refusjonskrav: Boolean,
    val versjon: Long,
)

data class SamordningsvedtakWrapper(
    @JsonUnwrapped
    val samordningsvedtak: Samordningsvedtak,
    val behandlingId: UUID,
)

data class OppdaterSamordningsmelding(
    val pid: String,
    val tpNr: String,
    val samId: Long,
    val vedtakId: Long,
    val refusjonskrav: Boolean,
    val kommentar: String,
)
