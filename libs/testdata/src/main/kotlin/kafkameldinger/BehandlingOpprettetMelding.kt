package kafkameldinger

import GrunnlagTestData
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret.sakIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.JsonMessage

fun behandlingOpprettetMelding() = JsonMessage.newMessage(
    mapOf(
        eventNameKey to "BEHANDLING:OPPRETTET",
        "sakId" to 1,
        "persongalleri" to GrunnlagTestData().hentPersonGalleri()
    )
)

fun behandlingGrunnlagEndretMelding() = JsonMessage.newMessage(
    mapOf(
        eventNameKey to BehandlingGrunnlagEndret.eventName,
        sakIdKey to 1,
        "persongalleri" to GrunnlagTestData().hentPersonGalleri()
    )
)