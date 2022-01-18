package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Opplysning
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class Opplysningsuthenter() {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Opplysning> {
        val skjema_info_opplysning = lagSkjemaInfoOpplysning(jsonNode)
        return listOf<Opplysning>(skjema_info_opplysning)
    }

    fun lagSkjemaInfoOpplysning(jsonNode: JsonNode): Opplysning {
        return Opplysning(
            UUID.randomUUID(), Opplysning.Privatperson(
                jsonNode["innsender"]["foedselsnummer"].asText(),
                LocalDateTime.parse(jsonNode["mottattDato"].asText()).toInstant(ZoneOffset.UTC)

            ), "innsendt soeknad", objectMapper.createObjectNode(), jsonNode.deepCopy()
        )

    }

}