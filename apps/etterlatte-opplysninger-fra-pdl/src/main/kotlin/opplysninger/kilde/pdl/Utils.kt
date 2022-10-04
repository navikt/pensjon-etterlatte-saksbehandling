package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.LocalDateTime
import java.time.YearMonth

fun lagBostedsadresse(opplysning: OpplysningDTO<Adresse>, fnr: Foedselsnummer) =
    lagPeriodisertAdresse(BOSTEDSADRESSE, opplysning, fnr)

private fun lagPeriodisertAdresse(type: Opplysningstyper, opplysning: OpplysningDTO<Adresse>, fnr: Foedselsnummer) =
    lagPersonOpplysning(
        opplysningsType = type,
        opplysning = opplysning,
        fnr = fnr,
        periode = Periode(
            fom = opplysning.verdi.gyldigFraOgMed.toYearMonth()!!,
            tom = opplysning.verdi.gyldigTilOgMed.toYearMonth()
        )
    )

private fun LocalDateTime?.toYearMonth() = this?.let { YearMonth.of(it.year, it.month) }