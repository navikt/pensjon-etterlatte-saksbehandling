package no.nav.etterlatte.model

import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.person.Person
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class BeregningsperiodeDAO(
    val beregningId: UUID,
    val behandlingId: UUID,
    val beregnetDato: LocalDateTime,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<Person>,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val grunnlagMetadata: Metadata
)

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagMetadata: Metadata
)