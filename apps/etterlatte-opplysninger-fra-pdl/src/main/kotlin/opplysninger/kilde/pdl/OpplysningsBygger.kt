package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.LocalDate
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {
    override fun byggOpplysninger(pdldata: Person): List<Behandlingsopplysning<out Any>> {

        val personInfo = PersonInfo("Test", "Testesen", Foedselsnummer.of("11057523044"), PersonType.AVDOED)
        val doedsdato = Doedsdato(LocalDate.parse(pdldata.doedsdato), pdldata.foedselsnummer.toString())

        return listOf(
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("Z815493"),
                "doedsdato",
                objectMapper.createObjectNode(),
                doedsdato
            ),
            Behandlingsopplysning(
                UUID.randomUUID(),
                Behandlingsopplysning.Saksbehandler("Z815493"),
                "personinfo",
                objectMapper.createObjectNode(),
                personInfo
            )
        )

    }
}
