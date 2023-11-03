package no.nav.etterlatte.joarkhendelser.joark

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

/**
 * Sjekker at tema registrert på [JournalfoeringHendelseRecord] tilhører Team Etterlatte
 *  EYB = [SakType.BARNEPENSJON]
 *  EYO = [SakType.OMSTILLINGSSTOENAD]
 **/
fun JournalfoeringHendelseRecord.erTemaEtterlatte(): Boolean =
    temaNytt == SakType.BARNEPENSJON.tema ||
        temaNytt == SakType.OMSTILLINGSSTOENAD.tema

/**
 * Konverterer [JournalfoeringHendelseRecord] sin journalpostStatus til merknad som brukes i Gjenny-oppgaven
 **/
fun JournalfoeringHendelseRecord.lagMerknadFraStatus(kanal: Kanal): String =
    when (journalpostStatus) {
        JournalpostStatus.MOTTATT -> "Mottatt journalpost (${kanal.beskrivelse})"
        JournalpostStatus.JOURNALFOERT -> "Ferdigstilt"
        JournalpostStatus.UKJENT_BRUKER -> "Ukjent bruker"
        JournalpostStatus.UTGAAR -> "Feil ifm. mottak eller journalføring"
        JournalpostStatus.OPPLASTING_DOKUMENT ->
            throw IllegalArgumentException("Status $journalpostStatus tilhører dagpenger!")
        else ->
            throw IllegalArgumentException("Ukjent journalpostStatus $journalpostStatus")
    }

/**
 * Konverterer [JournalfoeringHendelseRecord] tema til [SakType]
 **/
fun JournalfoeringHendelseRecord.temaTilSakType(): SakType =
    when (temaNytt) {
        "EYO" -> SakType.OMSTILLINGSSTOENAD
        "EYB" -> SakType.BARNEPENSJON
        else -> throw IllegalArgumentException("Ugyldig tema $temaNytt")
    }

object HendelseType {
    const val JOURNALPOST_MOTTATT = "JournalpostMottatt"
    const val TEMA_ENDRET = "TemaEndret"
    const val ENDELIG_JOURNALFOERT = "EndeligJournalført"
    const val JOURNALPOST_UTGAATT = "JournalpostUtgått"
}

object JournalpostStatus {
    const val MOTTATT = "MOTTATT"
    const val JOURNALFOERT = "JOURNALFOERT"
    const val UKJENT_BRUKER = "UKJENT_BRUKER"
    const val UTGAAR = "UTGAAR"
    const val OPPLASTING_DOKUMENT = "OPPLASTING_DOKUMENT"
}
