package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.libs.common.periode.Periode
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Opplysning.Periodisert::class, name = "periodisert"),
    JsonSubTypes.Type(value = Opplysning.Konstant::class, name = "konstant")
)
sealed class Opplysning<T>(val type: String) {
    data class Periodisert<T>(
        val perioder: List<PeriodisertOpplysning<T>>
    ) : Opplysning<T>("periodisert") {
        companion object {

            private val logger = LoggerFactory.getLogger(this::class.java)
            fun <T> create(grunnlagsopplysninger: List<Grunnlagsopplysning<T>>) =
                Periodisert(
                    grunnlagsopplysninger.map {
                        val periode = if (it.periode == null) {
                            logger.warn(
                                "Så en periodisert opplysning med id=${it.id} som mangler periode. Setter " +
                                    "periode som en fiktiv periode fra og med 1900-01 uten til og med"
                            )
                            Periode(YearMonth.of(1900, Month.JANUARY), null)
                        } else {
                            it.periode
                        }
                        PeriodisertOpplysning(
                            id = it.id,
                            kilde = it.kilde,
                            verdi = it.opplysning,
                            fom = periode.fom,
                            tom = periode.tom
                        )
                    }
                )
        }

        /* TODO ai: Midlertidig funksjon for å hente ut data. Skal erstattes av periodisering senere */
        fun hentSenest() = perioder.lastOrNull()
    }

    data class Konstant<T>(
        val id: UUID,
        val kilde: Grunnlagsopplysning.Kilde,
        val verdi: T
    ) : Opplysning<T>("konstant") {
        companion object {
            fun <T> create(grunnlagsopplysning: Grunnlagsopplysning<T>) = Konstant(
                id = grunnlagsopplysning.id,
                kilde = grunnlagsopplysning.kilde,
                verdi = grunnlagsopplysning.opplysning
            )
        }
    }
}

data class PeriodisertOpplysning<T>(
    val id: UUID,
    val kilde: Grunnlagsopplysning.Kilde,
    val verdi: T,
    val fom: YearMonth,
    val tom: YearMonth?
)