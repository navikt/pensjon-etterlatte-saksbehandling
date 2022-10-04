package no.nav.etterlatte.adresse

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.model.RegoppslagResponseDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

internal class AdresseKlientTest {

    @Test
    fun `Uthening av adresse fungerer`(){
        val klient = AdresseServiceMock()

        val adresse = runBlocking {
            klient.hentMottakerAdresse("123")
        }

        Assertions.assertNotNull(adresse)
        Assertions.assertEquals("Fornavnet", adresse.navn)
        Assertions.assertEquals(RegoppslagResponseDTO.AdresseType.NORSKPOSTADRESSE, adresse.adresse.type)
        Assertions.assertEquals("Adresselinje 1", adresse.adresse.adresselinje1)
        Assertions.assertEquals("0000", adresse.adresse.postnummer)
        Assertions.assertEquals("Sted", adresse.adresse.poststed)
        Assertions.assertEquals("Norge", adresse.adresse.land)
        Assertions.assertEquals("NO", adresse.adresse.landkode)

    }
}
