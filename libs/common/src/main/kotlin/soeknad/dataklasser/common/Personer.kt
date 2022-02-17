package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Gjenlevende
import no.nav.etterlatte.libs.common.soeknad.dataklasser.GjenlevendeForelder
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Gjenlevende::class, name = "GJENLEVENDE"),
    JsonSubTypes.Type(value = GjenlevendeForelder::class, name = "GJENLEVENDE_FORELDER"),
    JsonSubTypes.Type(value = Avdoed::class, name = "AVDOED"),
    JsonSubTypes.Type(value = Samboer::class, name = "SAMBOER"),
    JsonSubTypes.Type(value = Verge::class, name = "VERGE"),
    JsonSubTypes.Type(value = Barn::class, name = "BARN"),
    JsonSubTypes.Type(value = Forelder::class, name = "FORELDER"),
    JsonSubTypes.Type(value = Innsender::class, name = "INNSENDER"),
)
interface Person {
    val type: PersonType
    val fornavn: String
    val etternavn: String
    val foedselsnummer: Foedselsnummer
}

enum class PersonType {
    INNSENDER,
    GJENLEVENDE,
    GJENLEVENDE_FORELDER,
    AVDOED,
    SAMBOER,
    VERGE,
    BARN,
    FORELDER
}

data class Innsender(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer
) : Person {
    override val type: PersonType = PersonType.INNSENDER
}

data class Forelder(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer
) : Person {
    override val type: PersonType = PersonType.FORELDER
}

data class Barn(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,

    val statsborgerskap: Opplysning<String>,
    val utenlandsAdresse: BetingetOpplysning<Svar, Utenlandsadresse?>?,
    val foreldre: List<Forelder>,
    val verge: BetingetOpplysning<Svar, Verge>?,
    val dagligOmsorg: Opplysning<OmsorgspersonType>?
) : Person {
    override val type = PersonType.BARN
}

data class Avdoed(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,

    val datoForDoedsfallet: Opplysning<LocalDate>,
    val statsborgerskap: Opplysning<String>,
    val utenlandsopphold: BetingetOpplysning<Svar, List<Utenlandsopphold>>,
    val doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: Opplysning<Svar>,

    // Næringsinntekt og militærtjeneste er kun relevant dersom begge foreldrene er døde.
    val naeringsInntekt: BetingetOpplysning<Svar, Naeringsinntekt?>?,
    val militaertjeneste: BetingetOpplysning<Svar, Opplysning<AarstallForMilitaerTjeneste>?>?
) : Person {
    override val type = PersonType.AVDOED
}

data class Verge(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,
) : Person {
    override val type = PersonType.VERGE
}

data class Samboer(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,

    val fellesBarnEllertidligereGift: Opplysning<Svar>,
    val inntekt: BetingetOpplysning<Svar, SamboerInntekt?>?,
) : Person {
    override val type = PersonType.SAMBOER
}
