package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory.getLogger

typealias Grunnlagsdata<T> = Map<Opplysningstyper, Opplysning<T>>

inline fun <reified T> Grunnlagsdata<JsonNode>.hentKonstantOpplysning(
    opplysningstype: Opplysningstyper
): Opplysning.Konstant<T>? {
    val grunnlagsdata = this[opplysningstype] ?: return null

    return when (grunnlagsdata) {
        is Opplysning.Konstant -> Opplysning.Konstant(
            grunnlagsdata.kilde,
            objectMapper.readValue(grunnlagsdata.verdi.toJson(), object : TypeReference<T>() {})
        )

        else -> {
            getLogger(this::class.java).error("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
            throw RuntimeException("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
        }
    }
}

inline fun <reified T> Grunnlagsdata<JsonNode>.hentPeriodisertOpplysning(
    opplysningstype: Opplysningstyper
): Opplysning.Periodisert<T>? {
    val grunnlagsdata = this[opplysningstype] ?: return null

    return when (grunnlagsdata) {
        is Opplysning.Periodisert -> {
            Opplysning.Periodisert(
                grunnlagsdata.perioder.map {
                    PeriodisertOpplysning(
                        kilde = it.kilde,
                        verdi = objectMapper.readValue(it.verdi.toJson(), T::class.java),
                        fom = it.fom,
                        tom = it.tom
                    )
                }
            )
        }

        else -> {
            getLogger(this::class.java).error("Feil skjedde under henting av opplysning: Opplysningen er Konstant")
            throw RuntimeException("Feil skjedde under henting av opplysning: Opplysningen er Konstant")
        }
    }
}