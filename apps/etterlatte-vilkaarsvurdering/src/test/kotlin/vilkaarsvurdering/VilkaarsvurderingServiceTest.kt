package vilkaarsvurdering

import GrunnlagTestData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Doedsdato
import no.nav.etterlatte.libs.common.vikaar.kriteriegrunnlagTyper.Foedselsdato
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarOpplysningsType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.vilkaarsvurdering.SakType
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingRepositoryInMemory
import no.nav.etterlatte.vilkaarsvurdering.VilkaarsvurderingService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VilkaarsvurderingServiceTest {
    private val sendToRapid = mockk<(String) -> Unit>()
    private val service = VilkaarsvurderingService(VilkaarsvurderingRepositoryInMemory(), sendToRapid)
    private val uuid: UUID = UUID.randomUUID()

    @Test
    fun `Skal opprette en vilkaarsvurdering for foerstegangsbehandling av barnepensjon med grunnlagsopplysninger`() {
        val grunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val vilkaarsvurdering = service.opprettVilkaarsvurdering(
            uuid,
            SakType.BARNEPENSJON,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = LocalDate.of(2022, 1, 1),
            "",
            grunnlag
        )

        vilkaarsvurdering shouldNotBe null
        vilkaarsvurdering.behandlingId shouldBe uuid
        vilkaarsvurdering.vilkaar shouldHaveSize 7
        vilkaarsvurdering.vilkaar.first { it.hovedvilkaar.type == VilkaarType.ALDER_BARN }.let { vilkaar ->
            vilkaar.grunnlag shouldNotBe null
            vilkaar.grunnlag!! shouldHaveSize 2

            requireNotNull(vilkaar.grunnlag?.get(0)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.FOEDSELSDATO
                val opplysning = it.opplysning as Foedselsdato
                opplysning.foedselsdato shouldBe grunnlag.soeker.hentFoedselsdato()?.verdi
                opplysning.foedselsnummer shouldBe grunnlag.soeker.hentFoedselsnummer()?.verdi
            }
            requireNotNull(vilkaar.grunnlag?.get(1)).let {
                it.opplysningsType shouldBe VilkaarOpplysningsType.DOEDSDATO
                val opplysning = it.opplysning as Doedsdato
                opplysning.foedselsnummer shouldBe grunnlag.hentAvdoed().hentFoedselsnummer()?.verdi
                opplysning.doedsdato shouldBe grunnlag.hentAvdoed().hentDoedsdato()?.verdi
            }
        }
    }
}