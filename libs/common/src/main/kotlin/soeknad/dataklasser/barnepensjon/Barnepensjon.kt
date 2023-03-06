package no.nav.etterlatte.libs.common.soeknad.dataklasser

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Barn
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.EnumSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ImageTag
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Innsender
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.UtbetalingsInformasjon
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Barnepensjon(
    override val imageTag: ImageTag,
    override val spraak: Spraak,
    override val innsender: Innsender,
    override val harSamtykket: Opplysning<Boolean>,
    override val utbetalingsInformasjon: BetingetOpplysning<EnumSvar<BankkontoType>, UtbetalingsInformasjon>?,
    override val soeker: Barn,
    val foreldre: List<Person>,
    val soesken: List<Barn>
) : InnsendtSoeknad {
    override val versjon = "2"
    override val type = SoeknadType.BARNEPENSJON
    override val mottattDato: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC()

    init {
        requireNotNull(versjon) { "Versjon av søknaden må være satt" }
        requireNotNull(type)
        requireNotNull(mottattDato)
    }
}