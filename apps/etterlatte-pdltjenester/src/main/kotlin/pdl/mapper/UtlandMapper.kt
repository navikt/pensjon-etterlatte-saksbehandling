package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlInnflyttingTilNorge
import no.nav.etterlatte.pdl.PdlUtflyttingFraNorge

object UtlandMapper {
    fun mapUtland(hentPerson: PdlHentPerson): Utland {
        return Utland(
            utflyttingFraNorge = hentPerson.utflyttingFraNorge?.map { (mapUtflytting(it)) },
            innflyttingTilNorge = hentPerson.innflyttingTilNorge?.map { (mapInnflytting(it)) },
        )
    }

    private fun mapUtflytting(utflytting: PdlUtflyttingFraNorge): UtflyttingFraNorge {
        return UtflyttingFraNorge(
            tilflyttingsland = utflytting.tilflyttingsland,
            dato = utflytting.utflyttingsdato,
        )
    }

    private fun mapInnflytting(innflytting: PdlInnflyttingTilNorge): InnflyttingTilNorge {
        return InnflyttingTilNorge(
            fraflyttingsland = innflytting.fraflyttingsland,
            // TODO her må vi heller sjekke mot gyldighetsdato på bostedsadresse
            // TODO skal ikke være tostring her
            dato = innflytting.folkeregistermetadata?.gyldighetstidspunkt?.toLocalDate(),
        )
    }
}
