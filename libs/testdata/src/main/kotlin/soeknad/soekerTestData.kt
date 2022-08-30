package soeknad

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Forelder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Verge
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OmsorgspersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

fun soekerBarnSoeknad(
    type: PersonType = PersonType.BARN,
    fornavn: String = "Peter",
    etternavn: String = "Pan",
    foedselsnummer: Foedselsnummer = Foedselsnummer.of("31081164463"),
    statsborgerskap: String = "Norge",
    utenlandsadresse: UtenlandsadresseBarn = UtenlandsadresseBarn(JaNeiVetIkke.NEI, null, null),
    foreldre: List<Forelder> = listOf(
        Forelder(
            PersonType.FORELDER,
            "Michael",
            "Pan",
            Foedselsnummer.of("30058126663")
        )
    ),
    verge: Verge = Verge(null, null, null, null),
    omsorgPerson: OmsorgspersonType? = null
): SoekerBarnSoeknad = SoekerBarnSoeknad(
    type = type,
    fornavn = fornavn,
    etternavn = etternavn,
    foedselsnummer = foedselsnummer,
    statsborgerskap = statsborgerskap,
    utenlandsadresse = utenlandsadresse,
    foreldre = foreldre,
    verge = verge,
    omsorgPerson = omsorgPerson
)