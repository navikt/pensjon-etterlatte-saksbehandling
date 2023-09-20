package no.nav.etterlatte.libs.testdata.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OpplysningTest {
    @Test
    fun `kan deserializere Opplysning Konstant`() {
        val opplysning = GrunnlagTestData().hentOpplysningsgrunnlag().hentAvdoed().hentDoedsdato()!!
        val json = opplysning.toJson()

        Assertions.assertDoesNotThrow {
            objectMapper.readValue(json, Opplysning::class.java)
        }
    }
}
