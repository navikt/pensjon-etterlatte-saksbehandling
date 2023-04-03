package no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.UtbetalingsInformasjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Avdoed
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.EnumSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Utbetalingsinformasjon
import java.util.*
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Utenlandsopphold as UtenlandsoppholdOpplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsoppholdOpplysninger as UtenlandsoppholdOpplysningerOld

class Opplysningsuthenter {
    fun lagOpplysningsListe(jsonNode: JsonNode, type: SoeknadType): List<Grunnlagsopplysning<out Any?>> {
        return when (type) {
            SoeknadType.BARNEPENSJON -> BarnepensjonUthenter.lagOpplysningsListe(jsonNode)
            SoeknadType.OMSTILLINGSSTOENAD -> OmstillingsstoenadUthenter.lagOpplysningsListe(jsonNode)
            else -> throw Exception("Ugyldig SoeknadType")
        }
    }
}

internal fun avdoedOpplysning(avdoed: Avdoed): AvdoedSoeknad {
    return AvdoedSoeknad(
        type = PersonType.AVDOED,
        fornavn = avdoed.fornavn.svar,
        etternavn = avdoed.etternavn.svar,
        foedselsnummer = avdoed.foedselsnummer.svar.toFolkeregisteridentifikator(),
        doedsdato = avdoed.datoForDoedsfallet.svar.innhold,
        statsborgerskap = avdoed.statsborgerskap.svar.innhold,
        utenlandsopphold = UtenlandsoppholdOpplysningstype(
            avdoed.utenlandsopphold.svar.verdi,
            avdoed.utenlandsopphold.opplysning?.map { opphold ->
                UtenlandsoppholdOpplysningerOld(
                    opphold.land.svar.innhold,
                    opphold.fraDato?.svar?.innhold,
                    opphold.tilDato?.svar?.innhold,
                    opphold.oppholdsType.svar.map { it.verdi },
                    opphold.medlemFolketrygd.svar.verdi,
                    opphold.pensjonsutbetaling?.svar?.innhold
                )
            }
        ),
        doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi
    )
}

internal fun utbetalingsinformasjonOpplysning(
    betalingsinformasjon: BetingetOpplysning<EnumSvar<BankkontoType>, UtbetalingsInformasjon>?
): Utbetalingsinformasjon {
    return Utbetalingsinformasjon(
        betalingsinformasjon?.svar?.verdi,
        betalingsinformasjon?.opplysning?.kontonummer?.svar?.innhold,
        betalingsinformasjon?.opplysning?.utenlandskBankNavn?.svar?.innhold,
        betalingsinformasjon?.opplysning?.utenlandskBankAdresse?.svar?.innhold,
        betalingsinformasjon?.opplysning?.iban?.svar?.innhold,
        betalingsinformasjon?.opplysning?.swift?.svar?.innhold,
        betalingsinformasjon?.opplysning?.skattetrekk?.svar?.verdi,
        betalingsinformasjon?.opplysning?.skattetrekk?.opplysning?.svar?.innhold
    )
}

internal fun <T : Any> lagOpplysning(
    opplysningsType: Opplysningstype,
    kilde: Grunnlagsopplysning.Kilde,
    opplysning: T,
    periode: Periode?
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = kilde,
        opplysningType = opplysningsType,
        meta = objectMapper.createObjectNode(),
        opplysning = opplysning,
        periode = periode
    )
}

internal fun Foedselsnummer.toFolkeregisteridentifikator(): Folkeregisteridentifikator {
    return Folkeregisteridentifikator.of(this.value)
}