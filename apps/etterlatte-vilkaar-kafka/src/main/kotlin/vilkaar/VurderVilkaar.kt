package vilkaar

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.model.VilkaarService
import vilkaar.grunnlag.Vilkaarsgrunnlag
import vilkaar.grunnlag.VilkarIBehandling

import java.util.UUID


data class HendelseBehandlingOpprettet(
    val behandling: UUID,
    val opplysninger: List<Behandlingsopplysning<ObjectNode>> = emptyList()
)

data class HendelseNyttGrunnlag(
    val behandling: UUID,
    val opplysninger: List<Behandlingsopplysning<ObjectNode>> = emptyList()
)

data class HendelseVilkaarsvureringOpprettet(
    val behandling: UUID,
    val vurderVilkaar: VilkaarResultat
): VilkarsVurerngHendelse
data class HendelseGrunnlagsbehov(
    val behandling: UUID,
    val grunnlagstype: Opplysningstyper,
    val person: String?
): VilkarsVurerngHendelse
sealed interface VilkarsVurerngHendelse

class VurderVilkaar(val dao: Dao<VurderteVilkaarDao>) {
    val interessantGrunnlag = listOf(
        Opplysningstyper.AVDOED_SOEKNAD_V1,
        Opplysningstyper.SOEKER_SOEKNAD_V1,
        Opplysningstyper.SOEKER_PDL_V1,
        Opplysningstyper.AVDOED_PDL_V1,
        Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1
    )
    val service = VilkaarService()
/*

 */
    fun handleHendelse(behandlingOpprettet: HendelseBehandlingOpprettet): List<VilkarsVurerngHendelse> {
        return dao.inTransaction {
            val eksisterendeVurderinger = hentOppVurderinger(behandlingOpprettet.behandling)
            return@inTransaction if(eksisterendeVurderinger.isNotEmpty()){
                emptyList() //TODO: Skal den gjøre gjøre noe annet
            }else {
                val persongalleri = behandlingOpprettet.opplysninger.find { it.opplysningType == Opplysningstyper.PERSONGALLERI_V1 }?.let { setOpplysningType<Persongalleri>(it) }?.opplysning
                val grunnlag = behandlingOpprettet.opplysninger.fold(Vilkaarsgrunnlag() to interessantGrunnlag) { acc, opplysning ->
                    when (opplysning.opplysningType) {
                        Opplysningstyper.AVDOED_SOEKNAD_V1 -> acc.first.copy(avdoedSoeknad = setOpplysningType(opplysning))
                        Opplysningstyper.SOEKER_SOEKNAD_V1 -> acc.first.copy(soekerSoeknad = setOpplysningType(opplysning))
                        Opplysningstyper.SOEKER_PDL_V1 -> acc.first.copy(soekerPdl = setOpplysningType(opplysning))
                        Opplysningstyper.AVDOED_PDL_V1 -> acc.first.copy(avdoedPdl = setOpplysningType(opplysning))
                        Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> acc.first.copy(
                            gjenlevendePdl = setOpplysningType(
                                opplysning
                            )
                        )
                        else -> acc.first
                    } to acc.second.minus(opplysning.opplysningType)
                }
                val vurdering = VilkarIBehandling(behandlingOpprettet.behandling, grunnlag.first, 1, service.mapVilkaar(grunnlag.first))
                lagreVurdering(vurdering)
                println(vurdering)
                grunnlag.second.map {
                    HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))
                } + HendelseVilkaarsvureringOpprettet(behandlingOpprettet.behandling, vurdering.vilkaarResultat)
            }
        }

    }

    fun handleHendelse(hendelse: HendelseNyttGrunnlag): List<VilkarsVurerngHendelse>{
        return dao.inTransaction {
            val nyesteVurdering = hentOppVurderinger(hendelse.behandling).maxByOrNull { it.versjon }
            if(nyesteVurdering == null ) emptyList()
            else{
                val nyttGrunnlag = hendelse.opplysninger.fold(nyesteVurdering.grunnlag){acc, opplysning ->
                    when(opplysning.opplysningType){
                        Opplysningstyper.AVDOED_SOEKNAD_V1 -> acc.copy(avdoedSoeknad = setOpplysningType(opplysning))
                        Opplysningstyper.SOEKER_SOEKNAD_V1 -> acc.copy(soekerSoeknad = setOpplysningType(opplysning))
                        Opplysningstyper.SOEKER_PDL_V1 -> acc.copy(soekerPdl = setOpplysningType(opplysning))
                        Opplysningstyper.AVDOED_PDL_V1 -> acc.copy(avdoedPdl = setOpplysningType(opplysning))
                        Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> acc.copy(gjenlevendePdl = setOpplysningType(opplysning))
                        else -> acc
                    }
                }
                if(nyttGrunnlag === nyesteVurdering.grunnlag) emptyList()
                else{
                    val nyvurdering = nyesteVurdering.copy(versjon = nyesteVurdering.versjon + 1, grunnlag = nyttGrunnlag, vilkaarResultat = service.mapVilkaar(nyttGrunnlag))
                    lagreVurdering(nyvurdering)
                    listOf(HendelseVilkaarsvureringOpprettet(nyvurdering.behandling, nyvurdering.vilkaarResultat))
                }
            }


        }
    }

}

inline fun <reified T> setOpplysningType(opplysning: Behandlingsopplysning<ObjectNode>?): VilkaarOpplysning<T>? {
    return opplysning?.let {
        VilkaarOpplysning(
            opplysning.opplysningType,
            opplysning.kilde,
            objectMapper.readValue(opplysning.opplysning.toString())
        )
    }
}


fun Persongalleri.fnrForOpplysning(opplysningstype: Opplysningstyper): String? = when (opplysningstype) {
    Opplysningstyper.SOEKER_SOEKNAD_V1, Opplysningstyper.SOEKER_PDL_V1 -> soker
    Opplysningstyper.AVDOED_SOEKNAD_V1, Opplysningstyper.AVDOED_PDL_V1 -> avdoed.firstOrNull()
    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> gjenlevende.firstOrNull()
    else -> null
}
