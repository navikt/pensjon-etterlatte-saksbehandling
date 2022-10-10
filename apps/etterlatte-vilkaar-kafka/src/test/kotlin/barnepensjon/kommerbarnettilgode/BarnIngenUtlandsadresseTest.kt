package barnepensjon.kommerbarnettilgode

import GrunnlagTestData
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning.Konstant
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.UTENLANDSADRESSE
import no.nav.etterlatte.libs.common.person.Utenlandsadresse
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
            opplysningsmapSoekerOverrides = mapOf(
                UTENLANDSADRESSE to Konstant(randomUUID(), kilde, Utenlandsadresse(NEI, null, null).toJsonNode())
            )
        ).hentOpplysningsgrunnlag().soeker

        val barnIDanmark = GrunnlagTestData(
            opplysningsmapSoekerOverrides = mapOf(
                UTENLANDSADRESSE to Konstant(randomUUID(), kilde, Utenlandsadresse(JA, null, null).toJsonNode())
            )
        ).hentOpplysningsgrunnlag().soeker

        val ikkeUtland = barnIngenOppgittUtlandsadresse(barnINorge)

        val utland = barnIngenOppgittUtlandsadresse(barnIDanmark)

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ikkeUtland.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utland.resultat)
    }
}