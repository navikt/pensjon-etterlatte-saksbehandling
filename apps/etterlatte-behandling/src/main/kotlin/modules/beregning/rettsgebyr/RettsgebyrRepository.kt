package no.nav.etterlatte.modules.beregning.rettsgebyr

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.modules.beregning.etteroppgjoer.EtteroppgjoerRettsgebyr
import java.io.FileNotFoundException

object RettsgebyrRepository {
    val historiskeRettsgebyr: List<EtteroppgjoerRettsgebyr> =
        objectMapper.readValue(readFile("/rettsgebyr.json"), RettsgebyrListe::class.java).rettsgebyr

    private fun readFile(file: String) =
        RettsgebyrRepository::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
}

data class RettsgebyrListe(
    val rettsgebyr: List<EtteroppgjoerRettsgebyr>,
)
