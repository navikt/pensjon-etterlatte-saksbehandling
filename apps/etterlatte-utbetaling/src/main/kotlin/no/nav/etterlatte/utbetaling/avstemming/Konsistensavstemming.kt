package no.nav.etterlatte.utbetaling.avstemming

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ser.std.StdSerializer
import java.math.BigDecimal
import java.time.LocalDate

data class Konsistensavstemming(
    val id: UUIDBase64,
    val sakType: Saktype,
    val opprettet: Tidspunkt,
    val avstemmingsdata: String?,
    val loependeFraOgMed: Tidspunkt,
    val opprettetTilOgMed: Tidspunkt,
    val loependeUtbetalinger: List<OppdragForKonsistensavstemming>,
)

@JsonSerialize(using = OppdragForKonsistensavstemmingSerializer::class)
data class OppdragForKonsistensavstemming(
    val sakId: SakId,
    val sakType: Saktype,
    val fnr: Foedselsnummer,
    val utbetalingslinjer: List<OppdragslinjeForKonsistensavstemming>,
)

@JsonSerialize(using = OppdragslinjeForKonsistensavstemmingSerializer::class)
data class OppdragslinjeForKonsistensavstemming(
    val id: UtbetalingslinjeId,
    val opprettet: Tidspunkt,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    var forrigeUtbetalingslinjeId: UtbetalingslinjeId?,
    val beloep: BigDecimal?,
    val attestanter: List<NavIdent>,
    val kjoereplan: Kjoereplan,
)

/**
 * For performance - avoid the standard serializer's use of reflection
 */
internal class OppdragForKonsistensavstemmingSerializer :
    StdSerializer<OppdragForKonsistensavstemming>(OppdragForKonsistensavstemming::class.java) {
    override fun serialize(
        value: OppdragForKonsistensavstemming,
        gen: JsonGenerator,
        provider: SerializationContext,
    ) {
        gen.writeStartObject()

        gen.writeObjectPropertyStart("sakId")
        gen.writePOJOProperty("value", value.sakId.value)
        gen.writeEndObject()

        gen.writePOJOProperty("sakType", value.sakType.name)

        gen.writeObjectPropertyStart("fnr")
        gen.writePOJOProperty("value", value.fnr.value)
        gen.writeEndObject()

        gen.writeArrayPropertyStart("utbetalingslinjer")
        value.utbetalingslinjer.forEach { gen.writePOJO(it) }
        gen.writeEndArray()
        gen.writeEndObject()
    }
}

/**
 * For performance - avoid the standard serializer's use of reflection
 */
internal class OppdragslinjeForKonsistensavstemmingSerializer :
    StdSerializer<OppdragslinjeForKonsistensavstemming>(OppdragslinjeForKonsistensavstemming::class.java) {
    override fun serialize(
        value: OppdragslinjeForKonsistensavstemming,
        gen: JsonGenerator,
        provider: SerializationContext,
    ) {
        gen.writeStartObject()
        gen.writeObjectPropertyStart("id")
        gen.writePOJOProperty("value", value.id.value)
        gen.writeEndObject()

        gen.writeStringProperty("opprettet", value.opprettet.toString())
        gen.writeStringProperty("fraOgMed", value.fraOgMed.toString())
        gen.writeStringProperty("tilOgMed", value.tilOgMed?.toString())

        value.forrigeUtbetalingslinjeId?.let {
            gen.writeObjectPropertyStart("forrigeUtbetalingslinjeId")
            gen.writeNumberProperty("value", it.value)
            gen.writeEndObject()
        }
            ?: gen.writeNullProperty("forrigeUtbetalingslinjeId")

        gen.writeNumberProperty("beloep", value.beloep)

        gen.writeArrayPropertyStart("attestanter")
        value.attestanter.forEach {
            gen.writeStartObject()
            gen.writeStringProperty("value", it.value)
            gen.writeEndObject()
        }
        gen.writeEndArray()

        gen.writeStringProperty("kjoereplan", value.kjoereplan.name)

        gen.writeEndObject()
    }
}
