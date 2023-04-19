package no.nav.etterlatte.gyldigsoeknad

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext

fun sendOpplysningsbehov(
    sak: Sak,
    persongalleri: Persongalleri,
    context: MessageContext,
    packet: JsonMessage
) {
    context.publish(
        JsonMessage.newMessage(
            mapOf(
                BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1,
                "sakId" to sak.id,
                "sakType" to sak.sakType,
                "fnr" to persongalleri.soeker,
                "rolle" to PersonRolle.BARN,
                CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
            )
        ).toJson()
    )

    persongalleri.gjenlevende.forEach { fnr ->
        context.publish(
            JsonMessage.newMessage(
                mapOf(
                    BEHOV_NAME_KEY to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                    "sakId" to sak.id,
                    "sakType" to sak.sakType,
                    "fnr" to fnr,
                    "rolle" to PersonRolle.GJENLEVENDE,
                    CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                )
            ).toJson()
        )
    }

    persongalleri.avdoed.forEach { fnr ->
        context.publish(
            JsonMessage.newMessage(
                mapOf(
                    BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                    "sakId" to sak.id,
                    "sakType" to sak.sakType,
                    "fnr" to fnr,
                    "rolle" to PersonRolle.AVDOED,
                    CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                )
            ).toJson()
        )
    }
}