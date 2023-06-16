package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtaleKriteria

class AvtaleService() {
    private var avtaler: List<TrygdetidAvtale>
    private var kriterier: List<TrygdetidAvtaleKriteria>

    init {
        avtaler = loadJson("/trygdetid_avtaler.json")
        kriterier = loadJson("/trygdetid_avtale_kriterier.json")
    }

    fun hentAvtaler(): List<TrygdetidAvtale> = avtaler

    fun hentAvtaleKriterier(): List<TrygdetidAvtaleKriteria> = kriterier

    private inline fun <reified T> loadJson(filename: String) =
        this::class.java.getResource(filename)?.readText()?.let { json ->
            objectMapper.readValue<List<T>>(json)
        } ?: throw RuntimeException("Unable to load $filename")
}