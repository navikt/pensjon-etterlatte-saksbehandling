package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtaleKriteria
import java.util.UUID

class AvtaleService(val avtaleRepository: AvtaleRepository) {
    private var avtaler: List<TrygdetidAvtale>
    private var kriterier: List<TrygdetidAvtaleKriteria>

    init {
        avtaler = loadJson("/trygdetid_avtaler.json")
        kriterier = loadJson("/trygdetid_avtale_kriterier.json")
    }

    fun hentAvtaler(): List<TrygdetidAvtale> = avtaler

    fun hentAvtaleKriterier(): List<TrygdetidAvtaleKriteria> = kriterier

    fun hentAvtaleForBehandling(id: UUID): Trygdeavtale? = avtaleRepository.hentAvtale(id)

    fun lagreAvtale(trygdeavtale: Trygdeavtale) {
        avtaleRepository.lagreAvtale(trygdeavtale)
    }

    fun opprettAvtale(trygdeavtale: Trygdeavtale) {
        avtaleRepository.opprettAvtale(trygdeavtale)
    }

    private inline fun <reified T> loadJson(filename: String) =
        this::class.java.getResource(filename)?.readText()?.let { json ->
            objectMapper.readValue<List<T>>(json)
        } ?: throw RuntimeException("Unable to load $filename")
}
