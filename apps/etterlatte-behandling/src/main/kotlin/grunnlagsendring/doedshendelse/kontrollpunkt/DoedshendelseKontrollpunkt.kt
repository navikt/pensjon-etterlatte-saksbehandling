package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

sealed class DoedshendelseKontrollpunkt {
    abstract val beskrivelse: String
    abstract val kanSendeBrev: Boolean
    abstract val opprettOppgave: Boolean
    abstract val avbryt: Boolean

    class AvdoedLeverIPDL : DoedshendelseKontrollpunkt() {
        override val beskrivelse: String = "Avdød lever i PDL"
        override val kanSendeBrev: Boolean = false
        override val opprettOppgave: Boolean = false
        override val avbryt: Boolean = true
    }
}
