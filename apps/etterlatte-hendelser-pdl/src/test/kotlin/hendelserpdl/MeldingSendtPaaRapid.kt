package no.nav.etterlatte.hendelserpdl

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.pdl.hendelse.LeesahOpplysningstype

data class MeldingSendtPaaRapid<T>(
    @JsonProperty("@event_name") val eventName: String,
    val hendelse: LeesahOpplysningstype,
    val hendelse_data: T,
)
