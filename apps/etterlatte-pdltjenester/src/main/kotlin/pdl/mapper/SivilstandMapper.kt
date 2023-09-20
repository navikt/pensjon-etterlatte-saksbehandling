package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.pdl.PdlSivilstand

object SivilstandMapper {
    fun mapSivilstand(sivilstander: List<PdlSivilstand>): List<Sivilstand> =
        runBlocking {
            sivilstander.map {
                Sivilstand(
                    sivilstatus = it.type.let { type -> Sivilstatus.valueOf(type.name) },
                    relatertVedSiviltilstand = it.relatertVedSivilstand?.let { Folkeregisteridentifikator.of(it) },
                    gyldigFraOgMed = it.gyldigFraOgMed,
                    bekreftelsesdato = it.bekreftelsesdato,
                    kilde = it.metadata.master,
                )
            }
        }
}
