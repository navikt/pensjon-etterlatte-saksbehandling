package no.nav.etterlatte.rettsgebyr

import no.nav.etterlatte.avkorting.regler.EtteroppgjoerRettsgebyr
import no.nav.etterlatte.libs.common.objectMapper
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
