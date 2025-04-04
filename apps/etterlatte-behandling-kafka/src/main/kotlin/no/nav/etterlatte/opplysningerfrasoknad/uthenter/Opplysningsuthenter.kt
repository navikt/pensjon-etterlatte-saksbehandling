package no.nav.etterlatte.opplysningerfrasoknad.uthenter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.UtbetalingsInformasjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Avdoed
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.EnumSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.AvdoedSoeknad
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Utbetalingsinformasjon
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Utenlandsopphold as UtenlandsoppholdOpplysningstype
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.UtenlandsoppholdOpplysninger as UtenlandsoppholdOpplysningerOld

class Opplysningsuthenter {
    fun lagOpplysningsListe(
        jsonNode: JsonNode,
        type: SoeknadType,
    ): List<Grunnlagsopplysning<out Any?>> =
        when (type) {
            SoeknadType.BARNEPENSJON -> BarnepensjonUthenter.lagOpplysningsListe(jsonNode)
            SoeknadType.OMSTILLINGSSTOENAD -> OmstillingsstoenadUthenter.lagOpplysningsListe(jsonNode)
            else -> throw Exception("Ugyldig SoeknadType")
        }
}

internal fun avdoedOpplysning(avdoed: Avdoed): AvdoedSoeknad =
    AvdoedSoeknad(
        type = PersonType.AVDOED,
        fornavn = avdoed.fornavn.svar,
        etternavn = avdoed.etternavn.svar,
        foedselsnummer = avdoed.foedselsnummer?.svar?.toFolkeregisteridentifikator(),
        foedselsdato = avdoed.foedselsdato?.svar,
        doedsdato = avdoed.datoForDoedsfallet.svar.innhold,
        statsborgerskap = avdoed.statsborgerskap.svar.innhold!!,
        utenlandsopphold =
            UtenlandsoppholdOpplysningstype(
                avdoed.utenlandsopphold.svar.verdi,
                avdoed.utenlandsopphold.opplysning?.map { opphold ->
                    UtenlandsoppholdOpplysningerOld(
                        opphold.land.svar.innhold!!,
                        opphold.fraDato?.svar?.innhold,
                        opphold.tilDato?.svar?.innhold,
                        opphold.oppholdsType.svar.map { it.verdi },
                        opphold.medlemFolketrygd.svar.verdi,
                        opphold.pensjonsutbetaling?.svar?.innhold,
                    )
                },
            ),
        doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = avdoed.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi,
    )

internal fun utbetalingsinformasjonOpplysning(
    betalingsinformasjon: BetingetOpplysning<EnumSvar<BankkontoType>, UtbetalingsInformasjon>?,
): Utbetalingsinformasjon =
    Utbetalingsinformasjon(
        betalingsinformasjon?.svar?.verdi,
        betalingsinformasjon
            ?.opplysning
            ?.kontonummer
            ?.svar
            ?.innhold,
        betalingsinformasjon
            ?.opplysning
            ?.utenlandskBankNavn
            ?.svar
            ?.innhold,
        betalingsinformasjon
            ?.opplysning
            ?.utenlandskBankAdresse
            ?.svar
            ?.innhold,
        betalingsinformasjon
            ?.opplysning
            ?.iban
            ?.svar
            ?.innhold,
        betalingsinformasjon
            ?.opplysning
            ?.swift
            ?.svar
            ?.innhold,
    )

internal fun Foedselsnummer.toFolkeregisteridentifikator(): Folkeregisteridentifikator = Folkeregisteridentifikator.of(this.value)
