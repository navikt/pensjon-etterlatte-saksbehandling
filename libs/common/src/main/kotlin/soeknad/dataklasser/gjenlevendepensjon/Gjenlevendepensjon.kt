package no.nav.etterlatte.libs.common.soeknad.dataklasser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.*
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gjenlevendepensjon(
    override val imageTag: ImageTag,
    override val spraak: Spraak,
    override val innsender: Innsender,
    override val soeker: Gjenlevende,
    override val harSamtykket: Opplysning<Boolean>,
    override val utbetalingsInformasjon: BetingetOpplysning<EnumSvar<BankkontoType>, UtbetalingsInformasjon>?,

    val avdoed: Avdoed,
    val barn: List<Barn>
) : InnsendtSoeknad {
    override val versjon = "2"
    override val type: SoeknadType = SoeknadType.GJENLEVENDEPENSJON
    override val mottattDato: LocalDateTime = LocalDateTime.now()
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
    val forholdTilAvdoede: ForholdTilAvdoede,
) : Person {
    override val type = PersonType.GJENLEVENDE
}

