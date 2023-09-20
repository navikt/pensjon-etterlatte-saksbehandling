package migrering.verifisering

import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest

internal class Verifiserer(private val pdlKlient: PDLKlient) {
    fun verifiserRequest(request: MigreringRequest) {
        pdlKlient.hentPerson(PersonRolle.BARN, request.soeker)
        request.avdoedForelder.forEach { pdlKlient.hentPerson(PersonRolle.AVDOED, it.ident) }
        request.gjenlevendeForelder?.let { pdlKlient.hentPerson(PersonRolle.GJENLEVENDE, it) }
    }
}
