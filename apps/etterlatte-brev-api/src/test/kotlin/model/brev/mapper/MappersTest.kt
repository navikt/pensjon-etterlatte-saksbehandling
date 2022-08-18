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
    private val vedtak = VedtakServiceMock().hentVedtak("1").copy(
        grunnlag = objectMapper.readValue<Testdata>(readFile("/grunnlag.json")).grunnlag
    )

    @Test
    fun `Skal finne avdoede i grunnlaget`() {
        val avdoed = vedtak.finnAvdoed()

        avdoed.navn shouldBe "VAKKER LAPP"
        avdoed.doedsdato shouldBe LocalDate.of(2022, 2, 10)
    }

    @Test
    fun `Skal finne barnet i grunnlaget`() {
        val barn = vedtak.finnBarn()

        barn.navn shouldBe "TALENTFULL BLYANT"
        barn.fnr shouldBe "12101376212"
    }

    @Test
    fun `Skal finne barnets adresse som mottaker`() {
        val mottaker = vedtak.finnMottaker()

        mottaker.fornavn shouldBe "TALENTFULL"
        mottaker.etternavn shouldBe "BLYANT"
        mottaker.adresse shouldBe "Bøveien 937"
        mottaker.postnummer shouldBe "8475"
        mottaker.poststed shouldBe "Oslo"
        mottaker.land shouldBe "Norge"
    }
}

data class Testdata(val grunnlag: List<Grunnlagsopplysning<ObjectNode>>)
fun readFile(file: String): String = MappersTest::class.java.getResource(file)?.readText()
    ?: throw FileNotFoundException("Fant ikke filen $file")