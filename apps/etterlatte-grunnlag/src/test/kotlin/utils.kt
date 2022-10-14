import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

internal fun lagGrunnlagsopplysning(
    opplysningstype: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde = Grunnlagsopplysning.Pdl(
        navn = "pdl",
        tidspunktForInnhenting = Instant.now(),
        registersReferanse = null,
        opplysningId = UUID.randomUUID().toString()
    ),
    uuid: UUID = UUID.randomUUID(),
    verdi: JsonNode = objectMapper.createObjectNode(),
    fnr: Foedselsnummer? = null
) = Grunnlagsopplysning(
    uuid,
    kilde,
    opplysningstype,
    objectMapper.createObjectNode(),
    verdi,
    null,
    fnr
)

internal fun lagGrunnlagHendelse(
    sakId: Long,
    hendelseNummer: Long,
    opplysningType: Opplysningstype,
    id: UUID = UUID.randomUUID(),
    fnr: Foedselsnummer? = null,
    verdi: JsonNode = objectMapper.createObjectNode(),
    kilde: Grunnlagsopplysning.Kilde = Grunnlagsopplysning.Pdl(
        navn = "pdl",
        tidspunktForInnhenting = Instant.now(),
        registersReferanse = null,
        opplysningId = UUID.randomUUID().toString()
    )
) = OpplysningDao.GrunnlagHendelse(
    opplysning = lagGrunnlagsopplysning(
        opplysningstype = opplysningType,
        kilde = kilde,
        uuid = id,
        fnr = fnr,
        verdi = verdi
    ),
    sakId = sakId,
    hendelseNummer = hendelseNummer
)