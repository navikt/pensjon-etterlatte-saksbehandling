package barnepensjon.kommerbarnettilgode

import lagMockPersonSoekerSoeknad
import mapTilVilkaarstypeSoekerSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vilkaar.barnepensjon.barnIngenOppgittUtlandsadresse

class BarnIngenUtlandsadresseTest {

    @Test
    fun vurderBarnIngenUtlandsadresse() {
        val barnSoeknadNorge = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.NEI, null, null))
        val barnSoeknadDanmark = lagMockPersonSoekerSoeknad(UtenlandsadresseBarn(JaNeiVetIkke.JA, null, null))

        val ikkeUtland = barnIngenOppgittUtlandsadresse(
            Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadNorge)
        )

        val utland = barnIngenOppgittUtlandsadresse(
            Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE,
            mapTilVilkaarstypeSoekerSoeknad(barnSoeknadDanmark)
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, ikkeUtland.resultat)
        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, utland.resultat)

    }
}