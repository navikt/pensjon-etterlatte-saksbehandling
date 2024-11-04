package no.nav.etterlatte.utbetaling.avstemming

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Foedselsnummer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Kjoereplan
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.NavIdent
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.SakId
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Saktype
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingslinjeId
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
        provider: SerializerProvider,
    ) {
        gen.writeStartObject()

        gen.writeObjectFieldStart("sakId")
        gen.writeObjectField("value", value.sakId.value)
        gen.writeEndObject()

        gen.writeObjectField("sakType", value.sakType.name)

        gen.writeObjectFieldStart("fnr")
        gen.writeObjectField("value", value.fnr.value)
        gen.writeEndObject()

        gen.writeArrayFieldStart("utbetalingslinjer")
        value.utbetalingslinjer.forEach { gen.writeObject(it) }
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
        provider: SerializerProvider,
    ) {
        gen.writeStartObject()
        gen.writeObjectFieldStart("id")
        gen.writeObjectField("value", value.id.value)
        gen.writeEndObject()

        gen.writeStringField("opprettet", value.opprettet.toString())
        gen.writeStringField("fraOgMed", value.fraOgMed.toString())
        gen.writeStringField("tilOgMed", value.tilOgMed?.toString())

        value.forrigeUtbetalingslinjeId?.let {
            gen.writeObjectFieldStart("forrigeUtbetalingslinjeId")
            gen.writeNumberField("value", it.value)
            gen.writeEndObject()
        }
            ?: gen.writeNullField("forrigeUtbetalingslinjeId")

        gen.writeNumberField("beloep", value.beloep)

        gen.writeArrayFieldStart("attestanter")
        value.attestanter.forEach {
            gen.writeStartObject()
            gen.writeStringField("value", it.value)
            gen.writeEndObject()
        }
        gen.writeEndArray()

        gen.writeStringField("kjoereplan", value.kjoereplan.name)

        gen.writeEndObject()
    }
}
