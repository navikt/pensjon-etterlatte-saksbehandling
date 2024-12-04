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
        innflytting: PdlInnflyttingTilNorge,
        bostedsadresse: List<PdlBostedsadresse>?,
    ): InnflyttingTilNorge {
        val gyldighetsdato: LocalDate? = innflytting.folkeregistermetadata?.gyldighetstidspunkt?.toLocalDate()
        val ajourholdsdato: LocalDate? = innflytting.folkeregistermetadata?.ajourholdstidspunkt?.toLocalDate()

        val innvandretDato =
            if (gyldighetsdato != null) {
                val angittFlyttedato =
                    bostedsadresse
                        ?.filter {
                            it.gyldigFraOgMed != null
                        }?.find { it.gyldigFraOgMed?.toLocalDate() == gyldighetsdato }
                        ?.angittFlyttedato

                if (angittFlyttedato != null) {
                    angittFlyttedato
                } else {
                    finnForsteDatoEtterInnflytting(gyldighetsdato, bostedsadresse)
                }
            } else if (ajourholdsdato != null) {
                finnForsteDatoEtterInnflytting(ajourholdsdato, bostedsadresse)
            } else {
                null
            }

        return InnflyttingTilNorge(
            fraflyttingsland = innflytting.fraflyttingsland,
            dato = innvandretDato,
            gyldighetsdato = gyldighetsdato,
            ajourholdsdato = ajourholdsdato,
        )
    }

    private fun finnForsteDatoEtterInnflytting(
        systemoppgitt: LocalDate,
        bostedsadresse: List<PdlBostedsadresse>?,
    ): LocalDate {
        if (bostedsadresse.isNullOrEmpty()) {
            return systemoppgitt
        }

        return bostedsadresse
            .sortedBy { it.angittFlyttedato }
            .mapNotNull { hentDatoForBostedadresse(it) }
            .firstOrNull { it.isAfter(systemoppgitt) || it.isEqual(systemoppgitt) } ?: systemoppgitt
    }

    private fun hentDatoForBostedadresse(bostedsadresse: PdlBostedsadresse): LocalDate? {
        if (bostedsadresse.angittFlyttedato != null) {
            return bostedsadresse.angittFlyttedato
        }

        if (bostedsadresse.gyldigFraOgMed != null) {
            return bostedsadresse.gyldigFraOgMed.toLocalDate()
        }

        return null
    }
}
