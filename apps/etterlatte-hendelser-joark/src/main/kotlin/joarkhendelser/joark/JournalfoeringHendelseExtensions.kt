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
fun JournalfoeringHendelseRecord.lagMerknadFraStatus(): String =
    when (journalpostStatus) {
        "MOTTATT" -> "Mottatt"
        "JOURNALFOERT" -> "Ferdigstilt"
        "UKJENT_BRUKER" -> "Ukjent bruker"
        "UTGAAR" -> "Feil ifm. mottak eller journalføring"
        "OPPLASTING_DOKUMENT" -> throw IllegalArgumentException("Status $journalpostStatus tilhører dagpenger!")
        else -> throw IllegalArgumentException("Ukjent journalpostStatus $journalpostStatus")
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
