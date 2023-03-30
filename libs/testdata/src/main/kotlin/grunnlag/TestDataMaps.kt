package no.nav.etterlatte.libs.testdata.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.ADRESSEBESKYTTELSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOEDESBARN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.DOEDSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FAMILIERELASJON
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDELAND
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSAAR
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSDATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.FOEDSELSNUMMER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.NAVN
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONROLLE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SIVILSTATUS
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.STATSBORGERSKAP
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.AvdoedesBarn
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.pdl.personTestData
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID.randomUUID

val kilde = Grunnlagsopplysning.Pdl("pdl", Tidspunkt.now(), null, "opplysningsId1")
val statiskUuid = randomUUID()!!

val AVDOED_Folkeregisteridentifikator = Folkeregisteridentifikator.of("01448203510")
val GJENLEVENDE_Folkeregisteridentifikator = Folkeregisteridentifikator.of("29106323621")
val SOEKER_Folkeregisteridentifikator = Folkeregisteridentifikator.of("16021254243")
val HELSOESKEN_Folkeregisteridentifikator = Folkeregisteridentifikator.of("06051250220")
val HALVSOESKEN_Folkeregisteridentifikator = Folkeregisteridentifikator.of("09011076618")
val HALVSOESKEN_ANNEN_FORELDER = Folkeregisteridentifikator.of("20127905941")

val ADRESSE_DEFAULT =
    listOf(
        Adresse(
            type = AdresseType.VEGADRESSE,
            aktiv = true,
            adresseLinje1 = "Ulsetløkkja",
            adresseLinje2 = null,
            adresseLinje3 = null,
            coAdresseNavn = null,
            postnr = "2512",
            poststed = "KVIKNE",
            land = null,
            kilde = "pdl",
            gyldigFraOgMed = LocalDateTime.parse("2012-02-16T00:00"),
            gyldigTilOgMed = null
        )
    )

internal val soekerTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Søker", "Barn").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, SOEKER_Folkeregisteridentifikator.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(
        randomUUID(),
        kilde,
        SOEKER_Folkeregisteridentifikator.getBirthDate().toJsonNode()
    ),
    FOEDSELSAAR to Opplysning.Konstant(
        randomUUID(),
        kilde,
        SOEKER_Folkeregisteridentifikator.getBirthDate().year.toJsonNode()
    ),
    FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(randomUUID(), kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Periodisert(
        ADRESSE_DEFAULT.map {
            PeriodisertOpplysning(
                randomUUID(),
                kilde = kilde,
                verdi = it.toJsonNode(),
                fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
            )
        }
    ),
    STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        randomUUID(),
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDOED_Folkeregisteridentifikator, GJENLEVENDE_Folkeregisteridentifikator),
            foreldre = listOf(AVDOED_Folkeregisteridentifikator, GJENLEVENDE_Folkeregisteridentifikator),
            barn = null
        ).toJsonNode()
    )
)

internal val soeskenTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Hel", "Søsken").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, HELSOESKEN_Folkeregisteridentifikator.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(
        randomUUID(),
        kilde,
        HELSOESKEN_Folkeregisteridentifikator.getBirthDate().toJsonNode()
    ),
    FOEDSELSAAR to Opplysning.Konstant(
        randomUUID(),
        kilde,
        HELSOESKEN_Folkeregisteridentifikator.getBirthDate().year.toJsonNode()
    ),
    FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(randomUUID(), kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Periodisert(
        ADRESSE_DEFAULT.map {
            PeriodisertOpplysning(
                randomUUID(),
                kilde = kilde,
                verdi = it.toJsonNode(),
                fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
            )
        }
    ),
    STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        randomUUID(),
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDOED_Folkeregisteridentifikator, GJENLEVENDE_Folkeregisteridentifikator),
            foreldre = listOf(AVDOED_Folkeregisteridentifikator, GJENLEVENDE_Folkeregisteridentifikator),
            barn = null
        ).toJsonNode()
    )
)

internal val halvsoeskenTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Halv", "Søsken").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, HALVSOESKEN_Folkeregisteridentifikator.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(
        randomUUID(),
        kilde,
        HALVSOESKEN_Folkeregisteridentifikator.getBirthDate().toJsonNode()
    ),
    FOEDSELSAAR to Opplysning.Konstant(
        randomUUID(),
        kilde,
        HALVSOESKEN_Folkeregisteridentifikator.getBirthDate().year.toJsonNode()
    ),
    FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(randomUUID(), kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Periodisert(
        ADRESSE_DEFAULT.map {
            PeriodisertOpplysning(
                randomUUID(),
                kilde = kilde,
                verdi = it.toJsonNode(),
                fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
            )
        }
    ),
    STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.BARN.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        randomUUID(),
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = listOf(AVDOED_Folkeregisteridentifikator, HALVSOESKEN_ANNEN_FORELDER),
            foreldre = listOf(AVDOED_Folkeregisteridentifikator, HALVSOESKEN_ANNEN_FORELDER),
            barn = null
        ).toJsonNode()
    )
)

internal val avdoedTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Død", "Far").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, AVDOED_Folkeregisteridentifikator.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(
        randomUUID(),
        kilde,
        AVDOED_Folkeregisteridentifikator.getBirthDate().toJsonNode()
    ),
    FOEDSELSAAR to Opplysning.Konstant(
        randomUUID(),
        kilde,
        AVDOED_Folkeregisteridentifikator.getBirthDate().year.toJsonNode()
    ),
    FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    DOEDSDATO to Opplysning.Konstant(randomUUID(), kilde, LocalDateTime.parse("2022-08-17T00:00:00").toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(randomUUID(), kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Periodisert(
        ADRESSE_DEFAULT.map {
            PeriodisertOpplysning(
                randomUUID(),
                kilde = kilde,
                verdi = it.toJsonNode(),
                fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
            )
        }
    ),
    SIVILSTATUS to Opplysning.Konstant(randomUUID(), kilde, Sivilstatus.GIFT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.AVDOED.toJsonNode()),
    AVDOEDESBARN to Opplysning.Konstant(
        randomUUID(),
        kilde,
        AvdoedesBarn(listOf(personTestData(soekerTestopplysningerMap), personTestData(soeskenTestopplysningerMap)))
            .toJsonNode()
    ),
    FAMILIERELASJON to Opplysning.Konstant(
        randomUUID(),
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SOEKER_Folkeregisteridentifikator, HELSOESKEN_Folkeregisteridentifikator)
        ).toJsonNode()
    )
)

internal val gjenlevendeTestopplysningerMap: Map<Opplysningstype, Opplysning<JsonNode>> = mapOf(
    NAVN to Opplysning.Konstant(randomUUID(), kilde, Navn("Levende", "Mor").toJsonNode()),
    FOEDSELSNUMMER to Opplysning.Konstant(randomUUID(), kilde, GJENLEVENDE_Folkeregisteridentifikator.toJsonNode()),
    FOEDSELSDATO to Opplysning.Konstant(
        randomUUID(),
        kilde,
        GJENLEVENDE_Folkeregisteridentifikator.getBirthDate().toJsonNode()
    ),
    FOEDSELSAAR to Opplysning.Konstant(
        randomUUID(),
        kilde,
        GJENLEVENDE_Folkeregisteridentifikator.getBirthDate().year.toJsonNode()
    ),
    FOEDELAND to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    ADRESSEBESKYTTELSE to Opplysning.Konstant(randomUUID(), kilde, Adressebeskyttelse.UGRADERT.toJsonNode()),
    BOSTEDSADRESSE to Opplysning.Periodisert(
        ADRESSE_DEFAULT.map {
            PeriodisertOpplysning(
                randomUUID(),
                kilde = kilde,
                verdi = it.toJsonNode(),
                fom = it.gyldigFraOgMed!!.let { YearMonth.of(it.year, it.month) },
                tom = it.gyldigFraOgMed?.let { YearMonth.of(it.year, it.month) }
            )
        }
    ),
    SIVILSTATUS to Opplysning.Konstant(randomUUID(), kilde, Sivilstatus.GIFT.toJsonNode()),
    STATSBORGERSKAP to Opplysning.Konstant(randomUUID(), kilde, "NOR".toJsonNode()),
    PERSONROLLE to Opplysning.Konstant(randomUUID(), kilde, PersonRolle.GJENLEVENDE.toJsonNode()),
    FAMILIERELASJON to Opplysning.Konstant(
        randomUUID(),
        kilde,
        FamilieRelasjon(
            ansvarligeForeldre = emptyList(),
            foreldre = emptyList(),
            barn = listOf(SOEKER_Folkeregisteridentifikator, HELSOESKEN_Folkeregisteridentifikator)
        ).toJsonNode()
    )
)