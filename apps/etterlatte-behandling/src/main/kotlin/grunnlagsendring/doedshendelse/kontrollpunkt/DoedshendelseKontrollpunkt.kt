package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse
import no.nav.etterlatte.libs.common.sak.Sak
import java.time.LocalDate
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
    abstract val oppgaveTekst: String?

    @JsonTypeName("AVDOED_HAR_YTELSE_I_GJENNY")
    data class AvdoedHarYtelse(override val sak: Sak) : DoedshendelseKontrollpunkt(), KontrollpunktMedSak {
        override val kode = "AVDOED_HAR_YTELSE_I_GJENNY"
        override val beskrivelse: String = "Avdød har ytelse i gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Bruker har krav til behandling som ikke kan avsluttes automatisk." +
                    " Kravet må ferdigbehandles, eventuelt feilregistreres, før det opphøres manuelt."
    }

    @JsonTypeName("AVDOED_HAR_IKKE_YTELSE_I_GJENNY")
    data object AvdoedHarIkkeYtelse : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_IKKE_YTELSE_I_GJENNY"
        override val beskrivelse: String = "Avdød har ikke ytelse i gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
        override val oppgaveTekst: String? = null
    }

    @JsonTypeName("AVDOED_LEVER_I_PDL")
    data object AvdoedLeverIPDL : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_LEVER_I_PDL"
        override val beskrivelse: String = "Avdød lever i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("BARN_HAR_BP")
    data class BarnHarBarnepensjon(override val sak: Sak) : DoedshendelseKontrollpunkt(), KontrollpunktMedSak {
        override val kode = "BARN_HAR_BP"
        override val beskrivelse: String = "Barn har barnepensjon"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = this.beskrivelse
    }

    @JsonTypeName("KRYSSENDE_YTELSE_I_PESYS")
    data object KryssendeYtelseIPesysEps : DoedshendelseKontrollpunkt() {
        override val kode = "KRYSSENDE_YTELSE_I_PESYS"
        override val beskrivelse: String = "Den berørte(EPS) har uføretrygd eller alderspensjon i Pesys"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("AVDOED_HAR_D_NUMMER")
    data object AvdoedHarDNummer : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_D_NUMMER"
        override val beskrivelse: String = "Den avdøde har D-nummer"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = "Avdød har d-nummer. Sjekk familierelasjoner i Pesys og send eventuelt informasjonsbrev til gjenlevende/barn manuelt."
    }

    @JsonTypeName("EPS_HAR_DOEDSDATO")
    data object EpsHarDoedsdato : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_HAR_DOEDSDATO"
        override val beskrivelse: String = "Eps har dødsdato"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("EPS_KAN_HA_ALDERSPENSJON")
    data object EpsKanHaAlderspensjon : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_KAN_HA_ALDERSPENSJON"
        override val beskrivelse: String = "Eps kan ha alderspensjon"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("EPS_SKILT_SISTE_5_OG_GIFT_I_15")
    data class EpsHarVaertSkiltSiste5OgGiftI15(val doedsdato: LocalDate, val fnr: String) : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_SKILT_SISTE_5_OG_GIFT_I_15"
        override val beskrivelse: String = "Eps er skilt siste 5 år og gift i 15"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = "Tidligere ektefelle ($fnr) døde ($doedsdato). Saksbehandler må vurdere om gjenlevende har rettigheter."
    }

    @JsonTypeName("EPS_VARIGHET_UNDER_5_AAR_UTEN_BARN")
    data object EpsVarighetUnderFemAarUtenBarn : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_VARIGHET_UNDER_5_AAR_UTEN_BARN"
        override val beskrivelse: String = "Giftemål har ikke vart i 5 år, og finner ingen barn"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
        override val oppgaveTekst: String? = null
    }

    @JsonTypeName("EPS_VARIGHET_UNDER_5_AAR_UTEN_FELLES_BARN")
    data object EpsVarighetUnderFemAarUtenFellesBarn : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_VARIGHET_UNDER_5_AAR_UTEN_FELLES_BARN"
        override val beskrivelse: String = "Giftemål har ikke vart i 5 år, og finner ingen felles barn"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String = beskrivelse
    }

    @JsonTypeName("SAMBOER_SAMME_ADRESSE_OG_FELLES_BARN")
    data object SamboerSammeAdresseOgFellesBarn : DoedshendelseKontrollpunkt() {
        override val kode = "SAMBOER_SAMME_ADRESSE_OG_FELLES_BARN"
        override val beskrivelse: String = "Samboer til avdød med samme adresse og felles barn"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = false
    }

    @JsonTypeName("EPS_SKILT_SISTE_5_UKJENT_GIFTEMAAL_VARIGHET")
    data class EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde(val doedsdato: LocalDate, val fnr: String) : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_SKILT_SISTE_5_UKJENT_GIFTEMAAL_VARIGHET"
        override val beskrivelse: String = "Eps er skilt siste 5 år med ukjent lengde på giftermål"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Tidligere ektefelle ($fnr) døde ($doedsdato), men lengden på ekteskapet er ikke mulig å utlede." +
                    " Saksbehandler må vurdere lengden på ekteskapet og eventuelt sende informasjonsbrev til gjenlevende."
    }

    @JsonTypeName("AVDOED_HAR_UTVANDRET")
    data object AvdoedHarUtvandret : DoedshendelseKontrollpunkt() {
        override val kode = "AVDOED_HAR_UTVANDRET"
        override val beskrivelse: String = "Den avdøde har utvandret"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = "Avdød er utvandret. Sjekk familierelasjoner i Pesys og send eventuelt informasjonsbrev til gjenlevende/barn manuelt."
    }

    @JsonTypeName("ANNEN_FORELDER_IKKE_FUNNET")
    data object AnnenForelderIkkeFunnet : DoedshendelseKontrollpunkt() {
        override val kode = "ANNEN_FORELDER_IKKE_FUNNET"
        override val beskrivelse: String = "Klarer ikke finne den andre forelderen automatisk"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = beskrivelse
    }

    @JsonTypeName("SAMTIDIG_DOEDSFALL")
    data object SamtidigDoedsfall : DoedshendelseKontrollpunkt() {
        override val kode = "SAMTIDIG_DOEDSFALL"
        override val beskrivelse: String = "Personen har mistet begge foreldrene samtidig"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() = beskrivelse
    }

    @JsonTypeName("EPS_HAR_SAK_I_GJENNY")
    data class EpsHarSakMedIverksattBehandlingIGjenny(override val sak: Sak) : DoedshendelseKontrollpunkt(), KontrollpunktMedSak {
        override val kode = "EPS_HAR_SAK_I_GJENNY"
        override val beskrivelse: String = "Det eksisterer allerede en aktiv sak på EPS i Gjenny med iverksatt behandling"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
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
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("ANNULERT_DOEDSHENDELSE_PDL")
    data object DoedshendelseErAnnullert : DoedshendelseKontrollpunkt() {
        override val kode = "ANNULERT_DOEDSHENDELSE_PDL"
        override val beskrivelse: String = "Dødshendelsen ble annulert i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }
}

fun List<DoedshendelseKontrollpunkt>.finnSak(): Sak? {
    return this.filterIsInstance<KontrollpunktMedSak>().firstOrNull()?.sak
}

fun List<DoedshendelseKontrollpunkt>.finnOppgaveId(): UUID? {
    return this.filterIsInstance<DuplikatGrunnlagsendringsHendelse>().firstOrNull()?.oppgaveId
}
