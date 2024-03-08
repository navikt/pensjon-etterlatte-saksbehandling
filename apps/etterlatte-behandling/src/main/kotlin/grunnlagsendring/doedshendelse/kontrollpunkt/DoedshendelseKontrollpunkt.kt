package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

interface KontrollpunktMedSak {
    val sak: Sak
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kode")
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class DoedshendelseKontrollpunkt {
    abstract val kode: String
    abstract val beskrivelse: String
    abstract val sendBrev: Boolean
    abstract val opprettOppgave: Boolean
    abstract val avbryt: Boolean

    @JsonTypeName("AVDOED_HAR_YTELSE_I_GJENNY")
    data object AvdoedHarYtelse : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_YTELSE_I_GJENNY"
        override val beskrivelse: String = "Avdød har ytelse i gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("AVDOED_HAR_IKKE_YTELSE_I_GJENNY")
    data object AvdoedHarIkkeYtelse : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_IKKE_YTELSE_I_GJENNY"
        override val beskrivelse: String = "Avdød har ikke ytelse i gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("AVDOED_LEVER_I_PDL")
    data object AvdoedLeverIPDL : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_LEVER_I_PDL"
        override val beskrivelse: String = "Avdød lever i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("BARN_HAR_BP")
    data class BarnHarBarnepensjon(override val sak: Sak) : DoedshendelseKontrollpunkt(), KontrollpunktMedSak {
        override val kode = "BARN_HAR_BP"
        override val beskrivelse: String = "Barn har barnepensjon"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("BARN_HAR_UFOERE")
    data object BarnHarUfoereTrygd : DoedshendelseKontrollpunkt() {
        override val kode = "BARN_HAR_UFOERE"
        override val beskrivelse: String = "Barnet har uføretrygd"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = false
    }

    @JsonTypeName("KRYSSENDE_YTELSE_I_PESYS")
    data object KryssendeYtelseIPesysEps : DoedshendelseKontrollpunkt() {
        override val kode = "KRYSSENDE_YTELSE_I_PESYS"
        override val beskrivelse: String = "Den berørte(EPS) har uføretrygd eller alderspensjon i Pesys"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("AVDOED_HAR_D_NUMMER")
    data object AvdoedHarDNummer : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_D_NUMMER"
        override val beskrivelse: String = "Den avdøde har D-nummer"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("EPS_HAR_DOEDSDATO")
    data object EpsHarDoedsdato : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_HAR_DOEDSDATO"
        override val beskrivelse: String = "Eps har dødsdato"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("EPS_KAN_HA_ALDERSPENSJON")
    data object EpsKanHaAlderspensjon : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_KAN_HA_ALDERSPENSJON"
        override val beskrivelse: String = "Eps kan ha alderspensjon"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("EPS_SKILT_SISTE_5_OG_GIFT_I_15")
    data object EpsHarVaertSkiltSiste5OgGiftI15 : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_SKILT_SISTE_5_OG_GIFT_I_15"
        override val beskrivelse: String = "Eps er skilt siste 5 år og gift i 15"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("EPS_SKILT_SISTE_5_UKJENT_GIFTEMAAL_VARIGHET")
    data object EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_SKILT_SISTE_5_UKJENT_GIFTEMAAL_VARIGHET"
        override val beskrivelse: String = "Eps er skilt siste 5 år med ukjent lengde på giftermål"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("AVDOED_HAR_UTVANDRET")
    data object AvdoedHarUtvandret : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_UTVANDRET"
        override val beskrivelse: String = "Den avdøde har utvandret"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("ANNEN_FORELDER_IKKE_FUNNET")
    data object AnnenForelderIkkeFunnet : DoedshendelseKontrollpunkt() {
        override val kode = "ANNEN_FORELDER_IKKE_FUNNET"
        override val beskrivelse: String = "Klarer ikke finne den andre forelderen automatisk"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("SAMTIDIG_DOEDSFALL")
    data object SamtidigDoedsfall : DoedshendelseKontrollpunkt() {
        override val kode = "SAMTIDIG_DOEDSFALL"
        override val beskrivelse: String = "Personen har mistet begge foreldrene samtidig"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    @JsonTypeName("EPS_HAR_SAK_I_GJENNY")
    data class EpsHarSakIGjenny(override val sak: Sak) : DoedshendelseKontrollpunkt(), KontrollpunktMedSak {
        override val kode = "EPS_HAR_SAK_I_GJENNY"
        override val beskrivelse: String = "Det eksisterer allerede en sak på EPS i Gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    @JsonTypeName("DUPLIKAT_GRUNNLAGSENDRINGSHENDELSE")
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

    @JsonTypeName("ANNULERT_DOEDSHENDELSE_PDL")
    data object DoedshendelseErAnnullert : DoedshendelseKontrollpunkt() {
        override val kode = "ANNULERT_DOEDSHENDELSE_PDL"
        override val beskrivelse: String = "Dødshendelsen ble annulert i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }
}

fun List<DoedshendelseKontrollpunkt>.finnSak(): Sak? {
    return this.filterIsInstance<KontrollpunktMedSak>().firstOrNull()?.sak
}

fun List<DoedshendelseKontrollpunkt>.finnOppgaveId(): UUID? {
    return this.filterIsInstance<DuplikatGrunnlagsendringsHendelse>().firstOrNull()?.oppgaveId
}
