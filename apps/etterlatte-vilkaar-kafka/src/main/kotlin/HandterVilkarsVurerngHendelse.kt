import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory
import vilkaar.HendelseGrunnlagsbehov
import vilkaar.HendelseVilkaarsvureringOpprettet
import vilkaar.VilkarsVurerngHendelse
import java.util.*

class HandterVilkarsVurerngHendelse {
    companion object{
        private val logger = LoggerFactory.getLogger(HandterVilkarsVurerngHendelse::class.java)
    }

    fun handterHendelse (hendelse: VilkarsVurerngHendelse, originalPakke: JsonMessage, messageContext: MessageContext, behandling: UUID){
        if(hendelse is HendelseVilkaarsvureringOpprettet) {
            originalPakke["@vilkaarsvurdering"] = hendelse.vurderVilkaar
            messageContext.publish(originalPakke.toJson())
            logger.info("Vurdert Vilkår i behandling $behandling")
        }
        if (hendelse is HendelseGrunnlagsbehov){
            messageContext.publish(JsonMessage.newMessage(
                mapOf<String, Any>(
                    "@behov" to hendelse.grunnlagstype,
                    "behandling" to hendelse.behandling,
                ).let {
                    hendelse.person?.let {person -> it + ("fnr" to person) }?: it
                }.let {
                    hendelse.grunnlagstype.personRolle?.let {rolle -> it + ("rolle" to rolle) }?: it
                }
            ).toJson())
        }
    }
}

private val Opplysningstyper.personRolle: PersonRolle? get() = when(this){
    Opplysningstyper.AVDOED_SOEKNAD_V1, Opplysningstyper.AVDOED_PDL_V1 -> PersonRolle.AVDOED
    Opplysningstyper.SOEKER_SOEKNAD_V1, Opplysningstyper.SOEKER_PDL_V1 -> PersonRolle.BARN
    Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
    else -> null
}