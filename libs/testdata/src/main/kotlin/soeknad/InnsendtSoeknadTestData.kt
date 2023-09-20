package soeknad

import no.nav.etterlatte.libs.common.innsendtsoeknad.AndreYtelser
import no.nav.etterlatte.libs.common.innsendtsoeknad.BankkontoType
import no.nav.etterlatte.libs.common.innsendtsoeknad.ForholdTilAvdoede
import no.nav.etterlatte.libs.common.innsendtsoeknad.ForholdTilAvdoedeType
import no.nav.etterlatte.libs.common.innsendtsoeknad.Kontaktinfo
import no.nav.etterlatte.libs.common.innsendtsoeknad.SivilstatusType
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.UtbetalingsInformasjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Avdoed
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.DatoSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.EnumSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.FritekstSvar
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Gjenlevende
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Innsender
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Opplysning
import no.nav.etterlatte.libs.common.innsendtsoeknad.omstillingsstoenad.Omstillingsstoenad
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.LocalDate

object InnsendtSoeknadTestData {
    fun omstillingsSoeknad(): Omstillingsstoenad {
        return Omstillingsstoenad(
            imageTag = "9f1f95b2472742227b37d19dd2d735ac9001995e",
            spraak = Spraak.NB,
            innsender =
                Innsender(
                    fornavn =
                        Opplysning(
                            svar = "GØYAL",
                            spoersmaal = "Fornavn",
                        ),
                    etternavn =
                        Opplysning(
                            svar = "HØYSTAKK",
                            spoersmaal = "Etternavn",
                        ),
                    foedselsnummer =
                        Opplysning(
                            svar = Foedselsnummer.of("03108718357"),
                            spoersmaal = "Fødselsnummer",
                        ),
                ),
            harSamtykket =
                Opplysning(
                    svar = true,
                    spoersmaal = "",
                ),
            utbetalingsInformasjon =
                BetingetOpplysning(
                    svar =
                        EnumSvar(
                            verdi = BankkontoType.NORSK,
                            innhold = "Norsk",
                        ),
                    spoersmaal = "Ønsker du å motta utbetalingen på norsk eller utenlandsk bankkonto?",
                    opplysning =
                        UtbetalingsInformasjon(
                            kontonummer =
                                Opplysning(
                                    svar =
                                        FritekstSvar(
                                            innhold = "6848.64.44444",
                                        ),
                                    spoersmaal = "Oppgi norsk kontonummer for utbetaling",
                                ),
                            utenlandskBankNavn = null,
                            utenlandskBankAdresse = null,
                            iban = null,
                            swift = null,
                            skattetrekk = null,
                        ),
                ),
            soeker =
                Gjenlevende(
                    fornavn = Opplysning(svar = "Kirsten", spoersmaal = "Spoersmal"),
                    etternavn = Opplysning(svar = "Jakobsen", spoersmaal = "Etternavn"),
                    foedselsnummer =
                        Opplysning(
                            svar = Foedselsnummer.of("26058411891"),
                            spoersmaal = "Barnets fødselsnummer / d-nummer",
                        ),
                    statsborgerskap =
                        Opplysning(
                            svar = "Norge",
                            spoersmaal = "Statsborgerskap",
                        ),
                    sivilstatus = Opplysning(svar = "Gift", spoersmaal = "sivilstatus"),
                    adresse =
                        Opplysning(
                            svar = "Et sted 31",
                            spoersmaal = "adresse",
                        ),
                    bostedsAdresse =
                        Opplysning(
                            svar =
                                FritekstSvar(
                                    innhold = "bostedadresse",
                                ),
                            spoersmaal = "bostedadresse",
                        ),
                    kontaktinfo =
                        Kontaktinfo(
                            telefonnummer =
                                Opplysning(
                                    svar =
                                        FritekstSvar(
                                            innhold = "12345678",
                                        ),
                                    spoersmaal = "telefonnummer",
                                ),
                        ),
                    flyktning = null,
                    oppholdUtland = null,
                    nySivilstatus =
                        BetingetOpplysning(
                            svar =
                                EnumSvar(
                                    verdi = SivilstatusType.EKTESKAP,
                                    innhold = "Nei",
                                ),
                            spoersmaal = null,
                            opplysning = null,
                        ),
                    arbeidOgUtdanning = null,
                    fullfoertUtdanning = null,
                    andreYtelser =
                        AndreYtelser(
                            kravOmAnnenStonad =
                                BetingetOpplysning(
                                    svar =
                                        EnumSvar(
                                            verdi = JaNeiVetIkke.NEI,
                                            innhold = "Nei",
                                        ),
                                    spoersmaal = null,
                                    opplysning = null,
                                ),
                            annenPensjon =
                                BetingetOpplysning(
                                    svar =
                                        EnumSvar(
                                            verdi = JaNeiVetIkke.NEI,
                                            innhold = "Nei",
                                        ),
                                    spoersmaal = null,
                                    opplysning = null,
                                ),
                            pensjonUtland =
                                BetingetOpplysning(
                                    svar =
                                        EnumSvar(
                                            verdi = JaNeiVetIkke.NEI,
                                            innhold = "Nei",
                                        ),
                                    spoersmaal = null,
                                    opplysning = null,
                                ),
                        ),
                    uregistrertEllerVenterBarn =
                        Opplysning(
                            svar =
                                EnumSvar(
                                    verdi = JaNeiVetIkke.NEI,
                                    innhold = "Nei",
                                ),
                            spoersmaal = null,
                        ),
                    forholdTilAvdoede =
                        ForholdTilAvdoede(
                            relasjon =
                                Opplysning(
                                    svar =
                                        EnumSvar(
                                            verdi = ForholdTilAvdoedeType.GIFT,
                                            innhold = "Nei",
                                        ),
                                    spoersmaal = null,
                                ),
                            datoForInngaattPartnerskap = null,
                            datoForInngaattSamboerskap = null,
                            datoForSkilsmisse = null,
                            datoForSamlivsbrudd = null,
                            fellesBarn = null,
                            samboereMedFellesBarnFoerGiftemaal = null,
                            tidligereGift = null,
                            omsorgForBarn = null,
                            mottokBidrag = null,
                            mottokEktefelleBidrag = null,
                        ),
                ),
            avdoed =
                Avdoed(
                    fornavn = Opplysning(svar = "Bernt", spoersmaal = null),
                    etternavn = Opplysning(svar = "Jakobsen", spoersmaal = null),
                    foedselsnummer =
                        Opplysning(
                            svar = Foedselsnummer.of("22128202440"),
                            spoersmaal = "Barnets fødselsnummer / d-nummer",
                        ),
                    datoForDoedsfallet =
                        Opplysning(
                            svar =
                                DatoSvar(
                                    innhold = LocalDate.parse("2022-01-01"),
                                ),
                            spoersmaal = null,
                        ),
                    statsborgerskap =
                        Opplysning(
                            svar =
                                FritekstSvar(
                                    innhold = "Norge",
                                ),
                            spoersmaal = null,
                        ),
                    utenlandsopphold =
                        BetingetOpplysning(
                            svar =
                                EnumSvar(
                                    verdi = JaNeiVetIkke.NEI,
                                    innhold = "",
                                ),
                            spoersmaal = null,
                            opplysning = null,
                        ),
                    doedsaarsakSkyldesYrkesskadeEllerYrkessykdom =
                        Opplysning(
                            svar =
                                EnumSvar(
                                    verdi = JaNeiVetIkke.NEI,
                                    innhold = "",
                                ),
                            spoersmaal = null,
                        ),
                    naeringsInntekt = null,
                    militaertjeneste = null,
                ),
            barn = listOf(),
        )
    }
}
