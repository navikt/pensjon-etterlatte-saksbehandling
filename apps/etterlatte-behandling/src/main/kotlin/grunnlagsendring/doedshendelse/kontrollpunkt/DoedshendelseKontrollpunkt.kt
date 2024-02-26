package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

sealed class DoedshendelseKontrollpunkt {
    abstract val beskrivelse: String
    abstract val sendBrev: Boolean
    abstract val opprettOppgave: Boolean
    abstract val avbryt: Boolean

    data object AvdoedLeverIPDL : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Avdød lever i PDL"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object KryssendeYtelseIPesys : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Den berørte har uføretrygd eller alderspensjon i Pesys"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }

    data object AvdoedHarDNummer : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Den avdøde har D-nummer"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object AvdoedHarUtvandret : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Den avdøde har utvandret"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object AnnenForelderIkkeFunnet : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Klarer ikke finne den andre forelderen automatisk"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data object SamtidigDoedsfall : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Personen har mistet begge foreldrene samtidig"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data class SakEksistererIGjenny(val sak: Sak) : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Det eksisterer allerede en sak på bruker i Gjenny"
        override val sendBrev: Boolean = true
        override val opprettOppgave: Boolean = true
        override val avbryt: Boolean = false
    }

    data class DuplikatGrunnlagsendringsHendelse(
        val grunnlagsendringshendelseId: UUID,
        val oppgaveId: UUID?,
    ) :
        DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Det finnes en duplikat grunnlagsendringshendelse"
        override val sendBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }
}

fun List<DoedshendelseKontrollpunkt>.finnSak(): Sak? {
    return this.filterIsInstance<DoedshendelseKontrollpunkt.SakEksistererIGjenny>().firstOrNull()?.sak
}

fun List<DoedshendelseKontrollpunkt>.finnOppgaveId(): UUID? {
    return this.filterIsInstance<DoedshendelseKontrollpunkt.DuplikatGrunnlagsendringsHendelse>().firstOrNull()?.oppgaveId
}
