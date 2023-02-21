package no.nav.etterlatte.libs.common.soeknad.dataklasser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.AndreYtelser
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.AnnenUtdanning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ArbeidOgUtdanning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Barn
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.EnumSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ForholdTilAvdoede
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.FritekstSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.HoeyesteUtdanning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ImageTag
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Innsender
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Kontaktinfo
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtland
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Samboer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SivilstatusType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.UtbetalingsInformasjon
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
    val forholdTilAvdoede: ForholdTilAvdoede
) : Person {
    override val type = PersonType.GJENLEVENDE
}