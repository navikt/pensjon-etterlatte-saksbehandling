package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.BlockTilSlateKonverterer
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import org.slf4j.LoggerFactory
import java.util.Base64

class BrevbakerService(
    private val brevbakerKlient: BrevbakerKlient,
    private val adresseService: AdresseService,
    private val brevDataMapper: BrevDataMapper,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        brevID: BrevID,
        brevRequest: BrevbakerRequest,
    ): Pdf {
        val brevbakerResponse = brevbakerKlient.genererPdf(brevRequest)

        return Base64.getDecoder().decode(brevbakerResponse.base64pdf)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }

    suspend fun hentRedigerbarTekstFraBrevbakeren(behandling: Behandling): Slate {
        val request =
            BrevbakerRequest.fra(
                brevDataMapper.brevKode(behandling, BrevProsessType.REDIGERBAR).redigering,
                brevDataMapper.brevData(behandling),
                behandling,
                adresseService.hentAvsender(behandling.vedtak),
            )
        val brevbakerResponse = brevbakerKlient.genererJSON(request)
        return BlockTilSlateKonverterer.konverter(brevbakerResponse)
    }
}
