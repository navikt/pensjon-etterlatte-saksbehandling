package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtale
import no.nav.etterlatte.libs.common.trygdetid.avtale.TrygdetidAvtaleKriteria
import java.util.UUID

class AvtaleService(
    private val avtaleRepository: AvtaleRepository,
    private val brukInternTrygdetid: Boolean,
    private val brukEgenDatabaseForTrygdetid: Boolean,
) {
    private var avtaler: List<TrygdetidAvtale>
    private var kriterier: List<TrygdetidAvtaleKriteria>

    init {
        avtaler = loadJson("/trygdetid_avtaler.json")
        kriterier = loadJson("/trygdetid_avtale_kriterier.json")
    }

    private fun sjekkInternTrygdetidAktivert() {
        if (!brukInternTrygdetid || !brukEgenDatabaseForTrygdetid) {
            throw IkkeTillattException(
                code = "INTERN_TRYGDETID_IKKE_AKTIVERT",
                detail = "Intern trygdetid i behandling er ikke aktivert ennå",
            )
        }
    }

    fun hentAvtaler(): List<TrygdetidAvtale> = avtaler

    fun hentAvtaleKriterier(): List<TrygdetidAvtaleKriteria> = kriterier

    fun hentAvtaleForBehandling(id: UUID): Trygdeavtale? {
        sjekkInternTrygdetidAktivert()
        return avtaleRepository.hentAvtale(id)
    }

    fun lagreAvtale(trygdeavtale: Trygdeavtale) {
        sjekkInternTrygdetidAktivert()
        avtaleRepository.lagreAvtale(trygdeavtale)
    }

    fun opprettAvtale(trygdeavtale: Trygdeavtale) {
        sjekkInternTrygdetidAktivert()
        avtaleRepository.opprettAvtale(trygdeavtale)
    }

    private inline fun <reified T> loadJson(filename: String) =
        this::class.java.getResource(filename)?.readText()?.let { json ->
            objectMapper.readValue<List<T>>(json)
        } ?: throw RuntimeException("Unable to load $filename")
}
