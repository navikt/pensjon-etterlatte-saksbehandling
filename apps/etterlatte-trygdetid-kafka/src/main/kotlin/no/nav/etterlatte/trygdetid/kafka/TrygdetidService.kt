package no.nav.etterlatte.trygdetid.kafka

import io.ktor.client.HttpClient

class TrygdetidService(
    private val trygdetidApp: HttpClient,
    private val url: String,
)
