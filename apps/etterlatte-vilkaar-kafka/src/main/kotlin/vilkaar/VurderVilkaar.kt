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
import vilkaar.grunnlag.GrunnlagHendelseType
import vilkaar.grunnlag.Grunnlagshendelse
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


interface VurderVilkaar{
    val interessantGrunnlag: List<Opplysningstyper>
    fun handleHendelse(behandlingOpprettet: HendelseBehandlingOpprettet): List<VilkarsVurerngHendelse>
    fun handleHendelse(hendelse: HendelseNyttGrunnlag): List<VilkarsVurerngHendelse>
}

class VurderVilkaarImpl(val dao: Dao<VurderteVilkaarDao>): VurderVilkaar {
    override val interessantGrunnlag = listOf(
        Opplysningstyper.AVDOED_SOEKNAD_V1,
        Opplysningstyper.SOEKER_SOEKNAD_V1,
        Opplysningstyper.SOEKER_PDL_V1,
        Opplysningstyper.AVDOED_PDL_V1,
        Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1
    )
    private val service = VilkaarService()
/*

 */


    fun spolFremGrunnlag(hendelser: List<Grunnlagshendelse>, grunnlag: Pair<Vilkaarsgrunnlag, Long>): Pair<Vilkaarsgrunnlag, Long>{
        return hendelser
            .filter { it.hendelsenummer > grunnlag.second }
            .sortedBy { it.hendelsenummer }
            .mapNotNull { it.opplysning }
            .fold(grunnlag.first){acc, opplysning ->
                when(opplysning.opplysningType){
                    Opplysningstyper.AVDOED_SOEKNAD_V1 -> acc.copy(avdoedSoeknad = setOpplysningType(opplysning))
                    Opplysningstyper.SOEKER_SOEKNAD_V1 -> acc.copy(soekerSoeknad = setOpplysningType(opplysning))
                    Opplysningstyper.SOEKER_PDL_V1 -> acc.copy(soekerPdl = setOpplysningType(opplysning))
                    Opplysningstyper.AVDOED_PDL_V1 -> acc.copy(avdoedPdl = setOpplysningType(opplysning))
                    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> acc.copy(gjenlevendePdl = setOpplysningType(opplysning))
                    else -> acc
                }
            } to hendelser.maxOf { it.hendelsenummer }

    }



    override fun handleHendelse(behandlingOpprettet: HendelseBehandlingOpprettet): List<VilkarsVurerngHendelse> {
        return dao.inTransaction {
            val eksisterendeHendelser = hentGrunnlagsHendelser(behandlingOpprettet.behandling)
            return@inTransaction if(eksisterendeHendelser.isNotEmpty()){
                emptyList() //TODO: Skal den gjøre gjøre noe annet
            }else {

                var i:Long = 1
                val persongalleri = behandlingOpprettet.opplysninger.find { it.opplysningType == Opplysningstyper.PERSONGALLERI_V1 }?.let { setOpplysningType<Persongalleri>(VilkaarOpplysning(it.opplysningType, it.kilde, it.opplysning)) }?.opplysning
                val nyeHendelser: MutableList<Grunnlagshendelse> =  mutableListOf(Grunnlagshendelse(
                    behandlingOpprettet.behandling,
                    null,
                    GrunnlagHendelseType.BEHANDLING_OPPRETTET,
                    i++,
                    null
                ))


                behandlingOpprettet.opplysninger.forEach {
                    if(interessantGrunnlag.contains(it.opplysningType)){
                        nyeHendelser.add(
                            Grunnlagshendelse(
                                behandlingOpprettet.behandling,
                                VilkaarOpplysning(it.opplysningType, it.kilde, it.opplysning),
                                GrunnlagHendelseType.NY_OPPLYSNING,
                                i++,
                                null
                            )
                        )
                    }
                }
                val grunnlag = spolFremGrunnlag(nyeHendelser, Vilkaarsgrunnlag() to 0)
                val vurdering = VilkarIBehandling(behandlingOpprettet.behandling, grunnlag.first, grunnlag.second, service.mapVilkaar(grunnlag.first))
                lagreGrunnlagshendelse(nyeHendelser)
                lagreVurdering(vurdering)

                listOfNotNull(
                    HendelseVilkaarsvureringOpprettet(behandlingOpprettet.behandling, vurdering.vilkaarResultat),
                    Opplysningstyper.AVDOED_SOEKNAD_V1.takeIf { vurdering.grunnlag.avdoedSoeknad == null}?.let { HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))},
                    Opplysningstyper.SOEKER_SOEKNAD_V1.takeIf { vurdering.grunnlag.soekerSoeknad == null}?.let { HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))},
                    Opplysningstyper.SOEKER_PDL_V1.takeIf { vurdering.grunnlag.soekerPdl == null}?.let { HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))},
                    Opplysningstyper.AVDOED_PDL_V1.takeIf { vurdering.grunnlag.avdoedPdl == null}?.let { HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))},
                    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1.takeIf { vurdering.grunnlag.gjenlevendePdl == null}?.let { HendelseGrunnlagsbehov(behandlingOpprettet.behandling, it, persongalleri?.fnrForOpplysning(it))},
                )
            }
        }

    }

    override fun handleHendelse(hendelse: HendelseNyttGrunnlag): List<VilkarsVurerngHendelse>{
        return dao.inTransaction {
            val nyesteVurdering = hentOppVurderinger(hendelse.behandling).maxByOrNull { it.versjon }
            val eksisterendeHendelser = hentGrunnlagsHendelser(hendelse.behandling)
            if(nyesteVurdering == null ) emptyList()
            else{
                var i = eksisterendeHendelser.maxOf { it.hendelsenummer } + 1
                val nyeHendelser = hendelse.opplysninger.filter { it.opplysningType in interessantGrunnlag }.map {
                    Grunnlagshendelse(
                        hendelse.behandling,
                        VilkaarOpplysning(it.opplysningType, it.kilde, it.opplysning),
                        GrunnlagHendelseType.NY_OPPLYSNING,
                        i++,
                        null
                    )
                }

                lagreGrunnlagshendelse(nyeHendelser)
                val nyttGrunnlag = spolFremGrunnlag(eksisterendeHendelser + nyeHendelser, nyesteVurdering.grunnlag to nyesteVurdering.versjon)

                if(nyttGrunnlag.first === nyesteVurdering.grunnlag) emptyList()
                else{
                    val nyvurdering = nyesteVurdering.copy(versjon = nyttGrunnlag.second, grunnlag = nyttGrunnlag.first, vilkaarResultat = service.mapVilkaar(nyttGrunnlag.first))
                    lagreVurdering(nyvurdering)
                    listOf(HendelseVilkaarsvureringOpprettet(nyvurdering.behandling, nyvurdering.vilkaarResultat))
                }
            }
        }
    }
}

inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T>? {
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
