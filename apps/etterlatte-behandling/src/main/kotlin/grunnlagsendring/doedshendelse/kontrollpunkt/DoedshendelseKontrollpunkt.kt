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
    data class AvdoedHarYtelse(
        override val sak: Sak,
    ) : DoedshendelseKontrollpunkt(),
        KontrollpunktMedSak {
        override val kode = "AVDOED_HAR_YTELSE_I_GJENNY"
        override val beskrivelse: String = "Avdød har ytelse i gjenny"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "OPPHØR: Bruker har krav til behandling som ikke kan avsluttes automatisk." +
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
    data class BarnHarBarnepensjon(
        override val sak: Sak,
    ) : DoedshendelseKontrollpunkt(),
        KontrollpunktMedSak {
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

    @JsonTypeName("TIDLIGERE_EPS_GIFT_MER_ENN_25_AAR")
    data class TidligereEpsGiftMerEnn25Aar(
        val doedsdato: LocalDate,
        val fnr: String,
    ) : DoedshendelseKontrollpunkt() {
        override val kode = "TIDLIGERE_EPS_GIFT_MER_ENN_25_AAR"
        override val beskrivelse: String = "Eps er skilt fra avdød, men har vært gift mer enn 25 år tidligere"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Tidligere ektefelle ($fnr) døde ($doedsdato), og ekteskapet varte i mer enn 25 år." +
                    " Saksbehandler må vurdere om gjenlevende har rettigheter."
    }

    @JsonTypeName("TIDLIGERE_EPS_GIFT_UNDER_25_AAR_UTEN_FELLES_BARN")
    data object TidligereEpsGiftUnder25AarUtenFellesBarn : DoedshendelseKontrollpunkt() {
        override val kode = "TIDLIGERE_EPS_GIFT_UNDER_25_AAR_UTEN_FELLES_BARN"
        override val beskrivelse: String = "Eps er skilt fra avdød, har ikke vært gift i 25 år, og har ikke felles barn"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
        override val oppgaveTekst: String? = null
    }

    @JsonTypeName("TIDLIGERE_EPS_GIFT_MER_ENN_15_AAR_FELLES_BARN")
    data class TidligereEpsGiftMerEnn15AarFellesBarn(
        val doedsdato: LocalDate,
        val fnr: String,
    ) : DoedshendelseKontrollpunkt() {
        override val kode = "TIDLIGERE_EPS_GIFT_MER_ENN_15_AAR_FELLES_BARN"
        override val beskrivelse: String = "Eps er skilt fra avdød, men har vært gift mer enn 15 år tidligere og har felles barn"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Tidligere ektefelle ($fnr) døde ($doedsdato), og ekteskapet varte i mer enn 15 år og de har felles barn." +
                    " Saksbehandler må vurdere om gjenlevende har rettigheter."
    }

    @JsonTypeName("TIDLIGERE_EPS_GIFT_MINDRE_ENN_15_AAR_FELLES_BARN")
    data object TidligereEpsGiftMindreEnn15AarFellesBarn : DoedshendelseKontrollpunkt() {
        override val kode = "TIDLIGERE_EPS_GIFT_MINDRE_ENN_15_AAR_FELLES_BARN"
        override val beskrivelse: String = "Eps er skilt fra avdød, men har vært gift mindre enn 15 år tidligere og har felles barn"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
        override val oppgaveTekst: String? = null
    }

    @JsonTypeName("EPS_GIFT_UKJENT_GIFTEMAAL_VARIGHET")
    data class EktefelleMedUkjentGiftemaalLengde(
        val doedsdato: LocalDate,
        val fnr: String,
    ) : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_GIFT_UKJENT_GIFTEMAAL_VARIGHET"
        override val beskrivelse: String = "Eps har ukjent lengde på giftermål"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Tidligere ektefelle ($fnr) døde ($doedsdato), men lengden på ekteskapet er ikke mulig å utlede." +
                    " Saksbehandler må vurdere lengden på ekteskapet og eventuelt sende informasjonsbrev til gjenlevende."
    }

    @JsonTypeName("EPS_ER_GIFT_PAA_NYTT")
    data class EpsErGiftPaaNytt(
        val doedsdato: LocalDate,
        val fnr: String,
        val nyEktefelleFnr: String,
    ) : DoedshendelseKontrollpunkt() {
        override val kode = "EPS_GIFT_UKJENT_GIFTEMAAL_VARIGHET"
        override val beskrivelse: String = "Tidligere ektefelle har giftet seg på nytt med en annen person"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
        override val oppgaveTekst: String
            get() = "Tidligere ektefelle ($fnr) døde ($doedsdato), men brukeren er gift med en ny person ($nyEktefelleFnr)."
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
    data class EpsHarSakMedIverksattBehandlingIGjenny(
        override val sak: Sak,
    ) : DoedshendelseKontrollpunkt(),
        KontrollpunktMedSak {
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
    ) : DoedshendelseKontrollpunkt() {
        override val kode = "DUPLIKAT_GRUNNLAGSENDRINGSHENDELSE"
        override val beskrivelse: String = "Det finnes en duplikat grunnlagsendringshendelse"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val oppgaveTekst: String? = null
        override val avbryt: Boolean = true
    }

    @JsonTypeName("GJENLEVENDE_MANGLER_ADRESSE")
    data object GjenlevendeManglerAdresse : DoedshendelseKontrollpunkt() {
        override val kode = "GJENLEVENDE_MANGLER_ADRESSE"
        override val beskrivelse: String = "Gjenlevende har ingen aktiv adresse i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
        override val oppgaveTekst: String
            get() =
                "Informasjonsbrev om gjenlevenderettigheter er ikke sendt ut på grunn av manglende adresse. " +
                    "Saksbehandler må sørge for å sende dette manuelt."
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

fun List<DoedshendelseKontrollpunkt>.finnSak(): Sak? = this.filterIsInstance<KontrollpunktMedSak>().firstOrNull()?.sak

fun List<DoedshendelseKontrollpunkt>.finnOppgaveId(): UUID? =
    this.filterIsInstance<DuplikatGrunnlagsendringsHendelse>().firstOrNull()?.oppgaveId
