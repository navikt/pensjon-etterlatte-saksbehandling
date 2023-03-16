package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.etterlatte.libs.common.person.Foedselsnummer

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
    JsonSubTypes.Type(value = Innsender::class, name = "INNSENDER")
)
interface Person {
    val type: PersonType
    val fornavn: Opplysning<String>
    val etternavn: Opplysning<String>
    val foedselsnummer: Opplysning<Foedselsnummer>
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
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>
) : Person {
    override val type: PersonType = PersonType.INNSENDER
}

data class GjenlevendeForelder(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>,

    val adresse: Opplysning<String>,
    val statsborgerskap: Opplysning<String>,
    val kontaktinfo: Kontaktinfo
) : Person {
    override val type = PersonType.GJENLEVENDE_FORELDER
}

data class Forelder(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>
) : Person {
    override val type: PersonType = PersonType.FORELDER
}

data class Barn(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>,

    val statsborgerskap: Opplysning<String>,
    val utenlandsAdresse: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Utenlandsadresse?>?,
    val foreldre: List<Forelder>,
    val verge: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Verge>?,
    val dagligOmsorg: Opplysning<EnumSvar<OmsorgspersonType>>?
) : Person {
    override val type = PersonType.BARN
}

data class Gjenlevende(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>,

    val statsborgerskap: Opplysning<String>,
    val sivilstatus: Opplysning<String>,
    val adresse: Opplysning<String>?,
    val bostedsAdresse: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<FritekstSvar>?>?,
    val kontaktinfo: Kontaktinfo,
    val flyktning: Opplysning<EnumSvar<JaNeiVetIkke>>?,
    val oppholdUtland: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, OppholdUtland?>?,
    val nySivilstatus: BetingetOpplysning<EnumSvar<SivilstatusType>, Samboer?>,
    val arbeidOgUtdanning: ArbeidOgUtdanning?,
    val fullfoertUtdanning: BetingetOpplysning<EnumSvar<HoeyesteUtdanning>, Opplysning<AnnenUtdanning>?>?,
    val andreYtelser: AndreYtelser,
    val uregistrertEllerVenterBarn: Opplysning<EnumSvar<JaNeiVetIkke>>,
    val forholdTilAvdoede: ForholdTilAvdoede
) : Person {
    override val type = PersonType.GJENLEVENDE
}

data class Avdoed(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>,

    val datoForDoedsfallet: Opplysning<DatoSvar>,
    val statsborgerskap: Opplysning<FritekstSvar>,
    val utenlandsopphold: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, List<Utenlandsopphold>>,
    val doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: Opplysning<EnumSvar<JaNeiVetIkke>>,

    // Næringsinntekt og militærtjeneste er kun relevant dersom begge foreldrene er døde.
    val naeringsInntekt: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Naeringsinntekt?>?,
    val militaertjeneste: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, Opplysning<AarstallForMilitaerTjeneste>?>?
) : Person {
    override val type = PersonType.AVDOED
}

data class Verge(
    val fornavn: Opplysning<String>?,
    val etternavn: Opplysning<String>?,
    val foedselsnummer: Opplysning<Foedselsnummer>?
) {
    val type = PersonType.VERGE
}

data class Samboer(
    override val fornavn: Opplysning<String>,
    override val etternavn: Opplysning<String>,
    override val foedselsnummer: Opplysning<Foedselsnummer>,

    val fellesBarnEllertidligereGift: Opplysning<EnumSvar<JaNeiVetIkke>>,
    val inntekt: BetingetOpplysning<EnumSvar<JaNeiVetIkke>, SamboerInntekt?>?
) : Person {
    override val type = PersonType.SAMBOER
}