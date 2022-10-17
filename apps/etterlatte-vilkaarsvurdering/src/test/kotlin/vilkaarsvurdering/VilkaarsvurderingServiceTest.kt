package vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import no.nav.etterlatte.vilkaarsvurdering.SakType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarOpplysningsType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryInMemory
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.time.LocalDate
import java.util.*

internal class VilkaarsvurderingServiceTest {
    private val service = VilkaarsvurderingService(VilkaarsvurderingRepositoryInMemory())
    private val uuid: UUID = UUID.randomUUID()

    @Test
    fun `Skal opprette en vilkaarsvurdering for foerstegangsbehandling av barnepensjon med grunnlagsopplysninger`() {
        val vilkaarsvurdering = service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            "",
            grunnlag
        )

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 7
        vilkaarsvurdering.vilkaar.first { it.type == VilkaarType.ALDER_BARN }.let { vilkaar ->
            val grunnlag = requireNotNull(vilkaar.grunnlag?.get(0))
            grunnlag.opplysningsType shouldBe VilkaarOpplysningsType.FOEDSELSDATO
            val opplysning = grunnlag.opplysning as Foedselsdato
            opplysning.foedselsdato shouldBe LocalDate.of(2012, 2, 16)
            opplysning.foedselsnummer shouldBe Foedselsnummer.of("16021254243")
        }

        println(vilkaarsvurdering.toJson())
    }

    companion object {
        val grunnlag: Opplysningsgrunnlag = objectMapper.readValue(readFile("/grunnlag.json"))

        private fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }
}