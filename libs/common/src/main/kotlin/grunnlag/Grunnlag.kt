package no.nav.etterlatte.libs.common.grunnlag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.YearMonth

data class Grunnlag(
    val saksId: Long,
    val grunnlag: List<Grunnlagsopplysning<JsonNode>>,
    val versjon: Long
)

data class Opplysningsgrunnlag(
    val søker: Grunnlagsdata<JsonNode>,
    val familie: List<Grunnlagsdata<JsonNode>>,
    val sak: Grunnlagsdata<JsonNode>,
    private val metadata: Metadata
) {
    companion object {
        fun empty() = Opplysningsgrunnlag(
            søker = Grunnlagsdata(emptyMap()),
            familie = listOf(),
            sak = Grunnlagsdata(mapOf()),
            metadata = Metadata(0, 0)
        )
    }

    fun hentAvdoed(): Grunnlagsdata<JsonNode> =
        familie.find {
            it.hentKonstantOpplysning<PersonRolle>(Opplysningstyper.PERSONROLLE)?.verdi == PersonRolle.AVDOED
        }!!

    fun hentVersjon() = metadata.versjon
}

data class Metadata(val sakId: Long, val versjon: Long)

data class Grunnlagsdata<T>(
    val verdi: Map<Opplysningstyper, Opplysning<T>>
) : Map<Opplysningstyper, Opplysning<T>> by verdi {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    inline fun <reified T> hentKonstantOpplysning(opplysningstype: Opplysningstyper): Opplysning.Konstant<T>? {
        val grunnlagsdata = verdi[opplysningstype] ?: return null

        return when (grunnlagsdata) {
            is Opplysning.Konstant -> Opplysning.Konstant(
                grunnlagsdata.kilde,
                objectMapper.readValue(grunnlagsdata.verdi!!.toJson(), object : TypeReference<T>() {})
            )

            else -> {
                logger.error("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
                throw RuntimeException("Feil skjedde under henting av opplysning: Opplysningen er periodisert")
            }
        }
    }

    inline fun <reified T> hentPeriodisertOpplysning(opplysningstype: Opplysningstyper): Opplysning.Periodisert<T>? {
        val grunnlagsdata = verdi[opplysningstype] ?: return null

        return when (grunnlagsdata) {
            is Opplysning.Periodisert -> {
                Opplysning.Periodisert(
                    grunnlagsdata.perioder.map {
                        PeriodisertOpplysning(
                            kilde = it.kilde,
                            verdi = objectMapper.readValue(it.verdi!!.toJson(), T::class.java),
                            fom = it.fom,
                            tom = it.tom
                        )
                    }
                )
            }

            else -> {
                logger.error("Feil skjedde under henting av opplysning: Opplysningen er Konstant")
                throw RuntimeException("Feil skjedde under henting av opplysning: Opplysningen er Konstant")
            }
        }
    }
}

data class PeriodisertOpplysning<T>(
    val kilde: Grunnlagsopplysning.Kilde,
    val verdi: T,
    val fom: YearMonth,
    val tom: YearMonth?
)

sealed class Opplysning<T> {
    data class Periodisert<T>(
        val perioder: List<PeriodisertOpplysning<T>>
    ) : Opplysning<T>()

    data class Konstant<T>(
        val kilde: Grunnlagsopplysning.Kilde,
        val verdi: T
    ) : Opplysning<T>()
}