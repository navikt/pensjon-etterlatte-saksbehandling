
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDate
import java.util.UUID

internal fun lagGrunnlagsopplysning(
    opplysningstype: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde =
        Grunnlagsopplysning.Pdl(
            tidspunktForInnhenting = Tidspunkt.now(),
            registersReferanse = null,
            opplysningId = UUID.randomUUID().toString(),
        ),
    uuid: UUID = UUID.randomUUID(),
    verdi: JsonNode = objectMapper.createObjectNode(),
    fnr: Folkeregisteridentifikator? = null,
) = Grunnlagsopplysning(
    uuid,
    kilde,
    opplysningstype,
    objectMapper.createObjectNode(),
    verdi,
    null,
    fnr,
)

internal fun lagGrunnlagHendelse(
    sakId: SakId,
    hendelseNummer: Long,
    opplysningType: Opplysningstype,
    id: UUID = UUID.randomUUID(),
    fnr: Folkeregisteridentifikator? = null,
    verdi: JsonNode = objectMapper.createObjectNode(),
    kilde: Grunnlagsopplysning.Kilde =
        Grunnlagsopplysning.Pdl(
            tidspunktForInnhenting = Tidspunkt.now(),
            registersReferanse = null,
            opplysningId = UUID.randomUUID().toString(),
        ),
) = OpplysningDao.GrunnlagHendelse(
    opplysning =
        lagGrunnlagsopplysning(
            opplysningstype = opplysningType,
            kilde = kilde,
            uuid = id,
            fnr = fnr,
            verdi = verdi,
        ),
    sakId = sakId,
    hendelseNummer = hendelseNummer,
)

fun mockPerson() =
    PersonDTO(
        fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
        mellomnavn = OpplysningDTO(verdi = "Mellom", opplysningsid = null),
        etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
        foedselsnummer = OpplysningDTO(Folkeregisteridentifikator.of("10418305857"), null),
        foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
        foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
        foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
        doedsdato = null,
        adressebeskyttelse = null,
        bostedsadresse =
            listOf(
                OpplysningDTO(
                    Adresse(
                        type = AdresseType.VEGADRESSE,
                        aktiv = true,
                        coAdresseNavn = "Hos Geir",
                        adresseLinje1 = "Testveien 4",
                        adresseLinje2 = null,
                        adresseLinje3 = null,
                        postnr = "1234",
                        poststed = null,
                        land = "NOR",
                        kilde = "FREG",
                        gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                        gyldigTilOgMed = null,
                    ),
                    UUID.randomUUID().toString(),
                ),
            ),
        deltBostedsadresse = listOf(),
        kontaktadresse = listOf(),
        oppholdsadresse = listOf(),
        sivilstatus = null,
        sivilstand = null,
        statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
        utland = OpplysningDTO(Utland(emptyList(), emptyList()), UUID.randomUUID().toString()),
        familieRelasjon = null,
        avdoedesBarn = null,
        vergemaalEllerFremtidsfullmakt = null,
        pdlStatsborgerskap = null,
    )
