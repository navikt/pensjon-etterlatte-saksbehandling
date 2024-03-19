package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Sivilstatus
import kotlin.math.absoluteValue

internal class DoedshendelseKontrollpunktEktefelleService {
    fun identifiser(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        return listOfNotNull(
            // todo:  Disse her henger ikke helt i hop!
            kontrollerSkiltSistefemAarEllerGiftI15(eps, avdoed),
            kontrollerSkilti5AarMedUkjentGiftemaalStart(eps, avdoed),
            kontrollerEpsVarighet(avdoed, eps),
        )
    }

    private fun kontrollerSkilti5AarMedUkjentGiftemaalStart(
        ektefelle: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde? {
        if (ektefelle.sivilstand != null) {
            val skilt = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed =
                skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val doedsdato = avdoed.doedsdato!!.verdi
            val gift = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            if (skiltMedAvdoed.isNotEmpty() && giftMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = safeYearsBetween(skiltsivilstand.gyldigFraOgMed, doedsdato).absoluteValue
                val giftSivilstand = giftMedAvdoed.first()
                return if (antallSkilteAar <= 5 && giftSivilstand.gyldigFraOgMed == null) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5MedUkjentGiftemaalLengde(
                        doedsdato = avdoed.doedsdato!!.verdi,
                        fnr = ektefelle.foedselsnummer.verdi.value,
                    )
                } else {
                    null
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }

    private fun kontrollerSkiltSistefemAarEllerGiftI15(
        ektefelle: PersonDTO,
        avdoed: PersonDTO,
    ): DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5OgGiftI15? {
        if (ektefelle.sivilstand != null) {
            val skilt = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.SKILT }
            val skiltMedAvdoed =
                skilt.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val gift = ektefelle.sivilstand!!.map { it.verdi }.filter { it.sivilstatus == Sivilstatus.GIFT }
            val giftMedAvdoed = gift.filter { it.relatertVedSiviltilstand?.value == avdoed.foedselsnummer.verdi.value }
            val doedsdato = avdoed.doedsdato!!.verdi
            if (skiltMedAvdoed.isNotEmpty() && giftMedAvdoed.isNotEmpty()) {
                val skiltsivilstand = skiltMedAvdoed.first()
                val antallSkilteAar = safeYearsBetween(skiltsivilstand.gyldigFraOgMed, doedsdato).absoluteValue
                val giftSivilstand = giftMedAvdoed.first()
                val antallGifteAar = safeYearsBetween(giftSivilstand.gyldigFraOgMed, doedsdato).absoluteValue
                return if (antallSkilteAar <= 5 && antallGifteAar >= 15) {
                    DoedshendelseKontrollpunkt.EpsHarVaertSkiltSiste5OgGiftI15(
                        doedsdato = avdoed.doedsdato!!.verdi,
                        fnr = ektefelle.foedselsnummer.verdi.value,
                    )
                } else {
                    null
                }
            } else {
                return null
            }
        } else {
            return null
        }
    }

    private fun kontrollerEpsVarighet(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): DoedshendelseKontrollpunkt? {
        val antallAarGift =
            avdoed.sivilstand
                ?.asSequence()
                ?.map { it.verdi }
                ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi }
                ?.filter { it.sivilstatus == Sivilstatus.GIFT }
                ?.sortedBy { it.gyldigFraOgMed }
                ?.lastOrNull()
                ?.let { safeYearsBetween(it.gyldigFraOgMed, avdoed.doedsdato!!.verdi) }

        if (antallAarGift != null) {
            if (antallAarGift < 5) {
                // For ektefeller hvor varigheten av ekteskapet er under 5 år gjeler egne regler
                if (harFellesBarn(avdoed, eps)) {
                    // Ektefeller med felles barn har rett.
                    return null
                }
                if (!harBarn(avdoed) && !harBarn(eps)) {
                    // Hvis ingen har barn har ektefellen ingen rettighet.
                    return DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenBarn
                } else if (harBarn(avdoed) || !harBarn(eps)) {
                    // Hvis en av ektefellene har/hadde barn kan gjenlevende ha rett.
                    return DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenFellesBarn
                }
            } else {
                // Ektefeller hvor ekteskapet har vart over 5 år har rett.
                return null
            }
        } else {
            return null
        }
        return TODO("Provide the return value")
    }
}
