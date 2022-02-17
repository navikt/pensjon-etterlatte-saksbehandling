package no.nav.etterlatte.libs.common.soeknad.dataklasser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Barn
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ImageTag
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Innsender
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Kontaktinfo
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.UtbetalingsInformasjon
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Barnepensjon(
    override val imageTag: ImageTag,
    override val innsender: Innsender,
    override val harSamtykket: Opplysning<Boolean>,
    override val utbetalingsInformasjon: BetingetOpplysning<BankkontoType, UtbetalingsInformasjon>?,
    override val soeker: Barn,
    val foreldre: List<Person>,
    val soesken: List<Barn>
) : InnsendtSoeknad {
    override val versjon = "1"
    override val type = SoeknadType.BARNEPENSJON
    override val mottattDato: LocalDateTime = LocalDateTime.now()

    init {
        requireNotNull(versjon) { "Versjon av søknaden må være satt" }
        requireNotNull(type)
        requireNotNull(mottattDato)
    }
}

data class GjenlevendeForelder(
    override val fornavn: String,
    override val etternavn: String,
    override val foedselsnummer: Foedselsnummer,

    val adresse: Opplysning<String>,
    val statsborgerskap: Opplysning<String>,
    val kontaktinfo: Kontaktinfo,
) : Person {
    override val type = PersonType.GJENLEVENDE_FORELDER
}
