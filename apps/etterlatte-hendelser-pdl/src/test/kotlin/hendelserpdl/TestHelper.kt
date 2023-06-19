package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

val FNR = Folkeregisteridentifikator.of("11057523044")
val FNR_2 = Folkeregisteridentifikator.of("07010776133")

data class MeldingSendtPaaRapid<T>(
    @JsonProperty("@event_name") val eventName: String,
    val hendelse: LeesahOpplysningstype,
    val hendelse_data: T
)