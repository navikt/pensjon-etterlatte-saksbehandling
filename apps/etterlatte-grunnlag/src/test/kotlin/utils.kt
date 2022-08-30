import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

internal fun lagGrunnlagsopplysning(
    opplysningstyper: Opplysningstyper,
    kilde: Grunnlagsopplysning.Kilde = Grunnlagsopplysning.Pdl(
        navn = "pdl",
        tidspunktForInnhenting = Instant.now(),
        registersReferanse = null,
        opplysningId = UUID.randomUUID().toString()
    ),
    uuid: UUID = UUID.randomUUID(),
    verdi: JsonNode = objectMapper.createObjectNode(),
    fnr: Foedselsnummer? = null
) =
    Grunnlagsopplysning(
        uuid,
        kilde,
        opplysningstyper,
        objectMapper.createObjectNode(),
        verdi,
        null,
        fnr
    )