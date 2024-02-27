package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AnnenForelderIkkeFunnet
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarDNummer
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarUtvandret
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedLeverIPDL
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.BarnHarBarnepensjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.BarnHarUfoereTrygd
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.DoedshendelseErAnnulert
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.EpsHarSakIGjenny
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.KryssendeYtelseIPesys
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.SamtidigDoedsfall
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kode")
@JsonSubTypes(
    JsonSubTypes.Type(value = AvdoedLeverIPDL::class, name = "AVDOED_LEVER_I_PDL"),
    JsonSubTypes.Type(value = BarnHarBarnepensjon::class, name = "BARN_HAR_BP"),
    JsonSubTypes.Type(value = BarnHarUfoereTrygd::class, name = "BARN_HAR_UFOERE"),
    JsonSubTypes.Type(value = KryssendeYtelseIPesys::class, name = "KRYSSENDE_YTELSE_I_PESYS"),
    JsonSubTypes.Type(value = AvdoedHarDNummer::class, name = "AVDOED_HAR_D_NUMMER"),
    JsonSubTypes.Type(value = AvdoedHarUtvandret::class, name = "AVDOED_HAR_UTVANDRET"),
    JsonSubTypes.Type(value = AnnenForelderIkkeFunnet::class, name = "ANNEN_FORELDER_IKKE_FUNNET"),
    JsonSubTypes.Type(value = SamtidigDoedsfall::class, name = "SAMTIDIG_DOEDSFALL"),
    JsonSubTypes.Type(value = EpsHarSakIGjenny::class, name = "EPS_HAR_SAK_I_GJENNY"),
    JsonSubTypes.Type(value = DoedshendelseErAnnulert::class, name = "ANNULERT_DOEDSHENDELSE_PDL"),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class DoedshendelseKontrollpunkt {
    abstract val kode: String
    abstract val beskrivelse: String
    abstract val sendBrev: Boolean
    abstract val opprettOppgave: Boolean
    abstract val avbryt: Boolean

    data object AvdoedLeverIPDL : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_LEVER_I_PDL"
        override val beskrivelse: String = "Avdød lever i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object BarnHarBarnepensjon : DoedshendelseKontrollpunkt() {
        override val kode = "BARN_HAR_BP"
        override val beskrivelse: String = "Barn har barnepensjon"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object BarnHarUfoereTrygd : DoedshendelseKontrollpunkt() {
        override val kode = "BARN_HAR_UFOERE"
        override val beskrivelse: String = "Barnet har uføretrygd"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = false
    }

    data object KryssendeYtelseIPesys : DoedshendelseKontrollpunkt() {
        override val kode = "KRYSSENDE_YTELSE_I_PESYS"
        override val beskrivelse: String = "Den berørte har uføretrygd eller alderspensjon i Pesys"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object AvdoedHarDNummer : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_D_NUMMER"
        override val beskrivelse: String = "Den avdøde har D-nummer"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object AvdoedHarUtvandret : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_UTVANDRET"
        override val beskrivelse: String = "Den avdøde har utvandret"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object AnnenForelderIkkeFunnet : DoedshendelseKontrollpunkt() {
        override val kode = "ANNEN_FORELDER_IKKE_FUNNET"
        override val beskrivelse: String = "Klarer ikke finne den andre forelderen automatisk"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object SamtidigDoedsfall : DoedshendelseKontrollpunkt() {
        override val kode = "SAMTIDIG_DOEDSFALL"
        override val beskrivelse: String = "Personen har mistet begge foreldrene samtidig"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data class EpsHarSakIGjenny(val sak: Sak) : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_HAR_SAK_I_GJENNY"
        override val beskrivelse: String = "Det eksisterer allerede en sak på EPS i Gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data class DuplikatGrunnlagsendringsHendelse(
        val grunnlagsendringshendelseId: UUID,
        val oppgaveId: UUID?,
    ) :
        DoedshendelseKontrollpunkt() {
        override val kode = "DUPLIKAT_GRUNNLAGSENDRINGSHENDELSE"
        override val beskrivelse: String = "Det finnes en duplikat grunnlagsendringshendelse"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object DoedshendelseErAnnulert : DoedshendelseKontrollpunkt() {
        override val kode = "ANNULERT_DOEDSHENDELSE_PDL"
        override val beskrivelse: String = "Dødshendelsen ble annulert i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }
}

fun List<DoedshendelseKontrollpunkt>.finnSak(): Sak? {
    return this.filterIsInstance<EpsHarSakIGjenny>().firstOrNull()?.sak
}

fun List<DoedshendelseKontrollpunkt>.finnOppgaveId(): UUID? {
    return this.filterIsInstance<DuplikatGrunnlagsendringsHendelse>().firstOrNull()?.oppgaveId
}
