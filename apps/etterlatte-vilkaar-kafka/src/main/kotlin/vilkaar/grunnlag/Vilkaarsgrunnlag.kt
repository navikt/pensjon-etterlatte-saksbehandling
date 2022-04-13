package vilkaar.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.util.UUID
import javax.swing.tree.TreeNode


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

data class Grunnlagshendelse (
    val behandling:UUID,
    val opplysning: VilkaarOpplysning<ObjectNode>?,
    val hendelsetype: GrunnlagHendelseType,
    val hendelsenummer: Long,
    val hendelsereferanse: Long?,
)

enum class GrunnlagHendelseType {
    BEHANDLING_OPPRETTET,
    NY_OPPLYSNING
}