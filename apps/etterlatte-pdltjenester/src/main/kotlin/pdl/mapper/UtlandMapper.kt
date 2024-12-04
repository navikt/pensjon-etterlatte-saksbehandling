package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.pdl.PdlBostedsadresse
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlInnflyttingTilNorge
import no.nav.etterlatte.pdl.PdlUtflyttingFraNorge
import java.time.LocalDate

object UtlandMapper {
    fun mapUtland(hentPerson: PdlHentPerson): Utland =
        Utland(
            utflyttingFraNorge = hentPerson.utflyttingFraNorge?.map { (mapUtflytting(it)) },
            innflyttingTilNorge = hentPerson.innflyttingTilNorge?.map { (mapInnflytting(it, hentPerson.bostedsadresse)) },
        )

    private fun mapUtflytting(utflytting: PdlUtflyttingFraNorge): UtflyttingFraNorge =
        UtflyttingFraNorge(
            tilflyttingsland = utflytting.tilflyttingsland,
            dato = utflytting.utflyttingsdato,
        )

    private fun mapInnflytting(
        innflytting: PdlInnflyttingTilNorge?,
        bostedsadresse: List<PdlBostedsadresse>?,
    ): InnflyttingTilNorge {
        val gyldighetstidspunkt: LocalDate? = innflytting?.folkeregistermetadata?.gyldighetstidspunkt?.toLocalDate()
        val ajourholdstidspunkt: LocalDate? = innflytting?.folkeregistermetadata?.ajourholdstidspunkt?.toLocalDate()

        val innvandretDato =
            if (gyldighetstidspunkt != null) {
                val angittFlyttedato =
                    bostedsadresse
                        ?.filter {
                            it.gyldigFraOgMed != null
                        }?.find { it.gyldigFraOgMed?.toLocalDate() == gyldighetstidspunkt }
                        ?.angittFlyttedato

                if (angittFlyttedato != null) {
                    angittFlyttedato
                } else {
                    finnForsteDatoEtterInnflytting(gyldighetstidspunkt, bostedsadresse)
                }
            } else if (ajourholdstidspunkt != null) {
                finnForsteDatoEtterInnflytting(ajourholdstidspunkt, bostedsadresse)
            } else {
                null
            }

        return InnflyttingTilNorge(
            fraflyttingsland = innflytting?.fraflyttingsland,
            dato = innvandretDato,
            gyldighetstidspunkt = gyldighetstidspunkt,
            ajourholdstidspunkt = ajourholdstidspunkt,
        )
    }

    private fun finnForsteDatoEtterInnflytting(
        systemoppgitt: LocalDate,
        bostedstidspunkt: List<PdlBostedsadresse>?,
    ): LocalDate {
        if (bostedstidspunkt.isNullOrEmpty()) {
            return systemoppgitt
        }

        return bostedstidspunkt
            .sortedBy { it.angittFlyttedato }
            .mapNotNull { hentDatoForBostedadresse(it) }
            .firstOrNull { it.isAfter(systemoppgitt) || it.isEqual(systemoppgitt) } ?: systemoppgitt
    }

    private fun hentDatoForBostedadresse(bostedstidspunkt: PdlBostedsadresse): LocalDate? {
        if (bostedstidspunkt.angittFlyttedato != null) {
            return bostedstidspunkt.angittFlyttedato
        }

        if (bostedstidspunkt.gyldigFraOgMed != null) {
            return bostedstidspunkt.gyldigFraOgMed.toLocalDate()
        }

        return null
    }
}
