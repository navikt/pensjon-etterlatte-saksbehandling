import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun mapTilVilkaarstypeAvdoedSoeknad(person: AvdoedSoeknad): VilkaarOpplysning<AvdoedSoeknad> {
    return VilkaarOpplysning(
        UUID.randomUUID(),
        Opplysningstyper.AVDOED_SOEKNAD_V1,
        Grunnlagsopplysning.Privatperson("", Instant.now()),
        person
    )
}

fun mapTilVilkaarstypeSoekerSoeknad(person: SoekerBarnSoeknad): VilkaarOpplysning<SoekerBarnSoeknad> {
    return VilkaarOpplysning(
        UUID.randomUUID(),
        Opplysningstyper.SOEKER_PDL_V1,
        Grunnlagsopplysning.Privatperson("", Instant.now()),
        person
    )
}

fun lagMockPersonAvdoedSoeknad(utenlandsopphold: Utenlandsopphold): AvdoedSoeknad {
    return AvdoedSoeknad(
        PersonType.AVDOED,
        "Fornavn",
        "Etternavn",
        Foedselsnummer.of("19078504903"),
        LocalDate.parse("2020-06-10"),
        "Norge",
        utenlandsopphold,
        JaNeiVetIkke.NEI
    )
}

fun lagMockPersonSoekerSoeknad(utland: UtenlandsadresseBarn): SoekerBarnSoeknad {
    return SoekerBarnSoeknad(
        PersonType.BARN,
        "Fornavn",
        "Etternavn",
        Foedselsnummer.of("19040550081"),
        "Norge",
        utland,
        listOf(Forelder(PersonType.AVDOED, "fornavn", "etternavn", Foedselsnummer.of("19078504903"))),
        Verge(null, null, null, null),
        null
    )
}

fun adresserNorgePdl() =
    listOf(
        Adresse(
            AdresseType.VEGADRESSE,
            true,
            null,
            "Fiolveien 1A",
            null,
            null,
            "0485",
            "Oslo",
            "NOR",
            "kilde",
            LocalDateTime.parse("2020-01-26T00:00:00"),
            null
        ),
        Adresse(
            AdresseType.VEGADRESSE,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            "NOR",
            "kilde",
            LocalDateTime.parse("2010-01-25T00:00:00"),
            LocalDateTime.parse("2021-04-30T00:00:00")
        )
    )

fun adresseDanmarkPdl() = listOf(
    Adresse(
        AdresseType.UTENLANDSKADRESSE,
        true,
        null,
        "Danmarkveien 2A",
        null,
        null,
        "123345",
        "København",
        "DAN",
        "kilde",
        LocalDateTime.parse("2020-01-25T00:00:00"),
        null
    )
)

fun adresseUtlandFoerFemAar() = listOf(
    Adresse(
        AdresseType.VEGADRESSE,
        true,
        null,
        "Fiolveien 1A",
        null,
        null,
        "0485",
        "Oslo",
        "NOR",
        "kilde",
        LocalDateTime.parse("2017-01-26T00:00:00"),
        null
    ),
    Adresse(
        AdresseType.UTENLANDSKADRESSE,
        false,
        null,
        "Danmarkveien 2A",
        null,
        null,
        "123345",
        "København",
        "DAN",
        "kilde",
        LocalDateTime.parse("2012-01-25T00:00:00"),
        LocalDateTime.parse("2017-01-25T00:00:00")
    )
)