package no.nav.etterlatte.rapidsandrivers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.toUUID
import java.time.LocalDate
import java.util.UUID

const val SAK_ID_KEY = "sakId"
const val BREV_ID_KEY = "brevId"
const val BREV_KODE = "brevKode"
const val SAK_ID_FLERE_KEY = "sakIdFlere"
const val SAK_TYPE = "sakType"
const val BEHANDLING_ID_KEY = "behandlingId"
const val OPPGAVE_ID_FLERE_KEY = "behandlingIdFlere"
const val BEREGNING_KEY = "beregning"
const val AVKORTING_KEY = "avkorting"
const val DATO_KEY = "dato"
const val HENDELSE_DATA_KEY = "hendelse_data"
const val OPPGAVE_KEY = "oppgadeId"
const val BEHANDLING_VI_OMREGNER_FRA_KEY = "behandling_vi_omregner_fra"
const val TILBAKESTILTE_BEHANDLINGER_KEY = "tilbakestilte_behandlinger"
const val GRUNNLAG_OPPDATERT = "grunnlag_oppdatert" // TODO: eventname
const val OPPLYSNING_KEY = "opplysning"
const val FNR_KEY = "fnr"
const val NY_OPPLYSNING_KEY = "OPPLYSNING:NY"
const val DRYRUN = "dry_run"

var JsonMessage.sakId: Long
    get() = this[SAK_ID_KEY].asLong()
    set(name) {
        this[SAK_ID_KEY] = name
    }

var JsonMessage.sakIdFlere: List<Long>
    get() = this[SAK_ID_FLERE_KEY].map { it.asLong() }
    set(name) {
        this[SAK_ID_FLERE_KEY] = name
    }

var JsonMessage.behandlingId: UUID
    get() = this[BEHANDLING_ID_KEY].asUUID()
    set(name) {
        this[BEHANDLING_ID_KEY] = name
    }

var JsonMessage.brevId: Long
    get() = this[BREV_ID_KEY].asLong()
    set(name) {
        this[BREV_ID_KEY] = name
    }

var JsonMessage.dato: LocalDate
    get() = this[DATO_KEY].asLocalDate()
    set(name) {
        this[DATO_KEY] = name
    }
var JsonMessage.oppgaveId: UUID
    get() = this[OPPGAVE_KEY].asUUID()
    set(name) {
        this[OPPGAVE_KEY] = name
    }

var JsonMessage.tilbakestilteBehandlinger: List<UUID>
    get() =
        this[TILBAKESTILTE_BEHANDLINGER_KEY].asText().trim().split(";")
            .filter { it.isNotEmpty() }
            .map { UUID.fromString(it) }
    set(name) {
        this[TILBAKESTILTE_BEHANDLINGER_KEY] = name.joinToString(";") { it.toString() }
    }

fun JsonNode.asUUID() = this.asText().toUUID()
