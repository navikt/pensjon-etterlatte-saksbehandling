package soeknad

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.AndreYtelser
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BankkontoType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.BetingetOpplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.DatoSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.EnumSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ForholdTilAvdoede
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.ForholdTilAvdoedeType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.FritekstSvar
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Gjenlevende
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Innsender
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Kontaktinfo
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SivilstatusType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.UtbetalingsInformasjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import java.time.LocalDate

object InnsendtSoeknadTestData {

    fun omstillingsSoeknad(): Omstillingsstoenad {
        return Omstillingsstoenad(
            imageTag = "9f1f95b2472742227b37d19dd2d735ac9001995e",
            spraak = Spraak.NB,
            innsender = Innsender(
                fornavn = Opplysning(
                    svar = "GØYAL",
                    spoersmaal = "Fornavn"
                ),
                etternavn = Opplysning(
                    svar = "HØYSTAKK",
                    spoersmaal = "Etternavn"
                ),
                folkeregisteridentifikator = Opplysning(
                    svar = Folkeregisteridentifikator.of("03108718357"),
                    spoersmaal = "Fødselsnummer"
                )
            ),
            harSamtykket = Opplysning(
                svar = true,
                spoersmaal = ""
            ),
            utbetalingsInformasjon = BetingetOpplysning(
                svar = EnumSvar(
                    verdi = BankkontoType.NORSK,
                    innhold = "Norsk"
                ),
                spoersmaal = "Ønsker du å motta utbetalingen på norsk eller utenlandsk bankkonto?",
                opplysning = UtbetalingsInformasjon(
                    kontonummer = Opplysning(
                        svar = FritekstSvar(
                            innhold = "6848.64.44444"
                        ),
                        spoersmaal = "Oppgi norsk kontonummer for utbetaling"
                    ),
                    utenlandskBankNavn = null,
                    utenlandskBankAdresse = null,
                    iban = null,
                    swift = null,
                    skattetrekk = null
                )
            ),
            soeker = Gjenlevende(
                fornavn = Opplysning(svar = "Kirsten", spoersmaal = "Spoersmal"),
                etternavn = Opplysning(svar = "Jakobsen", spoersmaal = "Etternavn"),
                folkeregisteridentifikator = Opplysning(
                    svar = Folkeregisteridentifikator.of("26058411891"),
                    spoersmaal = "Barnets fødselsnummer / d-nummer"
                ),
                statsborgerskap = Opplysning(
                    svar = "Norge",
                    spoersmaal = "Statsborgerskap"
                ),
                sivilstatus = Opplysning(svar = "Gift", spoersmaal = "sivilstatus"),
                adresse = Opplysning(
                    svar = "Et sted 31",
                    spoersmaal = "adresse"
                ),
                bostedsAdresse = BetingetOpplysning(
                    svar = EnumSvar(
                        verdi = JaNeiVetIkke.NEI,
                        innhold = "Nei"
                    ),
                    spoersmaal = "Bor søker i et annet land enn Norge?",
                    opplysning = null
                ),
                kontaktinfo = Kontaktinfo(
                    telefonnummer = Opplysning(
                        svar = FritekstSvar(
                            innhold = "12345678"
                        ),
                        spoersmaal = "telefonnummer"
                    )
                ),
                flyktning = null,
                oppholdUtland = null,
                nySivilstatus = BetingetOpplysning(
                    svar = EnumSvar(
                        verdi = SivilstatusType.EKTESKAP,
                        innhold = "Nei"
                    ),
                    spoersmaal = null,
                    opplysning = null
                ),
                arbeidOgUtdanning = null,
                fullfoertUtdanning = null,
                andreYtelser = AndreYtelser(
                    kravOmAnnenStonad = BetingetOpplysning(
                        svar = EnumSvar(
                            verdi = JaNeiVetIkke.NEI,
                            innhold = "Nei"
                        ),
                        spoersmaal = null,
                        opplysning = null
                    ),
                    annenPensjon = BetingetOpplysning(
                        svar = EnumSvar(
                            verdi = JaNeiVetIkke.NEI,
                            innhold = "Nei"
                        ),
                        spoersmaal = null,
                        opplysning = null
                    ),
                    pensjonUtland = BetingetOpplysning(
                        svar = EnumSvar(
                            verdi = JaNeiVetIkke.NEI,
                            innhold = "Nei"
                        ),
                        spoersmaal = null,
                        opplysning = null
                    )
                ),
                uregistrertEllerVenterBarn = Opplysning(
                    svar = EnumSvar(
                        verdi = JaNeiVetIkke.NEI,
                        innhold = "Nei"
                    ),
                    spoersmaal = null
                ),
                forholdTilAvdoede = ForholdTilAvdoede(
                    relasjon = Opplysning(
                        svar = EnumSvar(
                            verdi = ForholdTilAvdoedeType.GIFT,
                            innhold = "Nei"
                        ),
                        spoersmaal = null
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
                    mottokEktefelleBidrag = null
                )
            ),
            avdoed = Avdoed(
                fornavn = Opplysning(svar = "Bernt", spoersmaal = null),
                etternavn = Opplysning(svar = "Jakobsen", spoersmaal = null),
                folkeregisteridentifikator = Opplysning(
                    svar = Folkeregisteridentifikator.of("22128202440"),
                    spoersmaal = "Barnets fødselsnummer / d-nummer"
                ),
                datoForDoedsfallet = Opplysning(
                    svar = DatoSvar(
                        innhold = LocalDate.parse("2022-01-01")
                    ),
                    spoersmaal = null
                ),
                statsborgerskap = Opplysning(
                    svar = FritekstSvar(
                        innhold = "Norge"
                    ),
                    spoersmaal = null
                ),
                utenlandsopphold = BetingetOpplysning(
                    svar = EnumSvar(
                        verdi = JaNeiVetIkke.NEI,
                        innhold = ""
                    ),
                    spoersmaal = null,
                    opplysning = null
                ),
                doedsaarsakSkyldesYrkesskadeEllerYrkessykdom = Opplysning(
                    svar = EnumSvar(
                        verdi = JaNeiVetIkke.NEI,
                        innhold = ""
                    ),
                    spoersmaal = null
                ),
                naeringsInntekt = null,
                militaertjeneste = null
            ),
            barn = listOf()

        )
    }
}