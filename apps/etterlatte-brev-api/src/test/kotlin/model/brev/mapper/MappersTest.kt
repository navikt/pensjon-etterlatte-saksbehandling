package model.brev.mapper

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import org.junit.jupiter.api.Test
import vedtak.VedtakServiceMock
import java.io.FileNotFoundException
import java.time.LocalDate

internal class MappersTest {

    @Test
    fun `Skal finne avdoede i grunnlaget`() {
        val vedtak = VedtakServiceMock().hentVedtak(1).copy(
            grunnlag = objectMapper.readValue<Testdata>(readFile("/grunnlag.json")).grunnlag
        )

        val avdoed = vedtak.finnAvdoed()

        avdoed.navn shouldBe "VAKKER LAPP"
        avdoed.doedsdato shouldBe LocalDate.of(2022, 2, 10)
    }

    @Test
    fun `Skal finne barnet i grunnlaget`() {
        val vedtak = VedtakServiceMock().hentVedtak(1).copy(
            grunnlag = objectMapper.readValue<Testdata>(readFile("/grunnlag.json")).grunnlag
        )

        val barn = vedtak.finnBarn()

        barn.navn shouldBe "TALENTFULL BLYANT"
        barn.fnr shouldBe "12101376212"
    }
}

data class Testdata(val grunnlag: List<Grunnlagsopplysning<ObjectNode>>)
fun readFile(file: String): String = MappersTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")
