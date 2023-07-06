package no.nav.etterlatte

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.opplysninger.kilde.pdl.PdlServiceInterface
import no.nav.etterlatte.opplysninger.kilde.pdl.lagEnkelopplysningerFraPDL

fun Route.PdlRoute(pdlService: PdlServiceInterface) {
    route("hentPerson") {
        get {
            kunSystembruker {
                val (fnr, personRolle, saktype, opplysningstype) = call.receive<Grunnlagsperson>()
                val person = pdlService.hentPerson(fnr, personRolle, saktype)
                val opplysningsperson = pdlService.hentOpplysningsperson(fnr, personRolle, saktype)
                val grunnlagsopplysningPerson = lagEnkelopplysningerFraPDL(
                    person = person,
                    personDTO = opplysningsperson,
                    opplysningsbehov = opplysningstype,
                    fnr = Folkeregisteridentifikator.of(fnr)
                )
                call.respond(grunnlagsopplysningPerson)
            }
        }
    }
}

data class Grunnlagsperson(
    val fnr: String,
    val personRolle: PersonRolle,
    val sakType: SakType,
    val opplysningstype: Opplysningstype
)