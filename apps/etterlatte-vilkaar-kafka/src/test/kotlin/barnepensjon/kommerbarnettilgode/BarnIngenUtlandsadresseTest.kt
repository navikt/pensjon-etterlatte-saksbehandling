package barnepensjon.kommerbarnettilgode

import GrunnlagTestData
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning.Konstant
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTENLANDSOPPHOLD
import no.nav.etterlatte.libs.common.person.Utenlandsopphold
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke.JA
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke.NEI
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnIngenOppgittUtlandsadresse
import java.util.UUID.randomUUID

class BarnIngenUtlandsadresseTest {

    @Test
    fun vurderBarnIngenUtlandsadresse() {
        val barnINorge = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                UTENLANDSOPPHOLD to Konstant(randomUUID(), kilde, Utenlandsopphold(NEI, null, null).toJsonNode())
            )
        ).hentOpplysningsgrunnlag().søker

        val barnIDanmark = GrunnlagTestData(
            opplysningsmapSøkerOverrides = mapOf(
                UTENLANDSOPPHOLD to Konstant(randomUUID(), kilde, Utenlandsopphold(JA, null, null).toJsonNode())
            )
        ).hentOpplysningsgrunnlag().søker

        val ikkeUtland = barnIngenOppgittUtlandsadresse(barnINorge)

        val utland = barnIngenOppgittUtlandsadresse(barnIDanmark)

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ikkeUtland.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utland.resultat)
    }
}