package no.nav.etterlatte.libs.regler.beregning

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAlleKnekkpunkter
import no.nav.etterlatte.libs.regler.saksbehandler
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeregnBarnepensjonTest {
    private val grunnlag = BarnepensjonGrunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = BigDecimal(30))
        )
    )

    @Test
    fun `Skal eksekvere regel for barnepensjon`() {
        val periode = RegelPeriode(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1))

        val resultat = kroneavrundetBarnepensjonRegel.eksekver(grunnlag, periode)
    }

    @Test
    fun `Skal finne alle knekkpunktene i regelverket`() {
        val knekkpunkter = kroneavrundetBarnepensjonRegel.finnAlleKnekkpunkter()

        knekkpunkter.size shouldBe 2
    }

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .addModule(JavaTimeModule())
        .addModule(KotlinModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .build()

    private fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}