package vilkaar.grunnlag

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.UUID


data class VilkarIBehandling(
    val behandling:UUID,
    val grunnlag:Vilkaarsgrunnlag,
    val versjon: Long,
    val vilkaarResultat: VilkaarResultat
)

data class Vilkaarsgrunnlag (
    val avdoedSoeknad: VilkaarOpplysning<AvdoedSoeknad>? = null,
    val soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>? = null,
    val soekerPdl: VilkaarOpplysning<Person>? = null,
    val avdoedPdl : VilkaarOpplysning<Person>? = null,
    val gjenlevendePdl: VilkaarOpplysning<Person>? = null
    )