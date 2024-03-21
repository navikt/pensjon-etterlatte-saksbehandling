@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Sivilstatus.*
import kotlin.math.absoluteValue

internal class DoedshendelseKontrollpunktEktefelleService {
    fun identifiser(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> = kontrollerEpsVarighet(avdoed, eps)

    private fun kontrollerEpsVarighet(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> {
        return if (varEktefelleVedDoedsfall(avdoed, eps)) {
            when (val antallAarGiftVedDoedsfall = finnAntallAarGiftVedDoedsfall(avdoed, eps)) {
                null ->
                    listOf(
                        DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                            doedsdato = avdoed.doedsdato!!.verdi,
                            fnr = eps.foedselsnummer.verdi.value,
                        ),
                    )
                else -> kontrollerEktefeller(antallAarGiftVedDoedsfall, avdoed, eps)
            }
        } else {
            listOf(kontrollerTidligereEktefeller(avdoed, eps))
        }
    }

    private fun kontrollerEktefeller(
        antallAarGift: Long,
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> =
        if (antallAarGift < 5) {
            if (harFellesBarn(avdoed, eps)) {
                emptyList()
            } else if (!harBarn(avdoed) && !harBarn(eps)) {
                listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenBarn)
            } else {
                listOf(DoedshendelseKontrollpunkt.EpsVarighetUnderFemAarUtenFellesBarn)
            }
        } else {
            emptyList()
        }

    private fun kontrollerTidligereEktefeller(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): DoedshendelseKontrollpunkt {
        val sivilstanderForEpsMedAvdoed =
            eps.sivilstand
                ?.map { it.verdi }
                ?.filter { it.relatertVedSiviltilstand == avdoed.foedselsnummer.verdi }

        if (sivilstanderForEpsMedAvdoed.isNullOrEmpty()) {
            throw IllegalStateException("Fant ingen sivilstander for eps med avdoed")
        }

        val gift = sivilstanderForEpsMedAvdoed.firstOrNull { it.sivilstatus in listOf(GIFT, REGISTRERT_PARTNER) }?.gyldigFraOgMed
        val skilt = sivilstanderForEpsMedAvdoed.lastOrNull { it.sivilstatus in listOf(SKILT, SKILT_PARTNER) }?.gyldigFraOgMed

        if (gift == null || skilt == null) {
            return DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                doedsdato = avdoed.doedsdato!!.verdi,
                fnr = eps.foedselsnummer.verdi.value,
            )
        }

        val antallAarGift = safeYearsBetween(gift, skilt).absoluteValue

        if (harFellesBarn(avdoed, eps)) {
            return if (antallAarGift >= 15) {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn15AarFellesBarn(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = eps.foedselsnummer.verdi.value,
                )
            } else {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMindreEnn15AarFellesBarn
            }
        } else {
            return if (antallAarGift >= 25) {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn25Aar(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = eps.foedselsnummer.verdi.value,
                )
            } else {
                DoedshendelseKontrollpunkt.TidligereEpsGiftUnder25AarUtenFellesBarn
            }
        }
    }

    private fun varEktefelleVedDoedsfall(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): Boolean =
        avdoed.sivilstand
            ?.asSequence()
            ?.map { it.verdi }
            ?.sortedBy { it.gyldigFraOgMed }
            ?.lastOrNull()
            ?.let {
                if (it.sivilstatus in listOf(GIFT, SEPARERT, REGISTRERT_PARTNER, SEPARERT_PARTNER)) {
                    return it.relatertVedSiviltilstand == eps.foedselsnummer.verdi
                }
                return false
            }
            ?: false

    private fun finnAntallAarGiftVedDoedsfall(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): Long? =
        avdoed.sivilstand
            ?.asSequence()
            ?.map { it.verdi }
            ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi }
            ?.filter { it.sivilstatus in listOf(GIFT, SEPARERT, REGISTRERT_PARTNER, SEPARERT_PARTNER) }
            ?.sortedBy { it.gyldigFraOgMed }
            ?.firstOrNull()
            ?.let {
                if (it.gyldigFraOgMed != null) {
                    safeYearsBetween(it.gyldigFraOgMed, avdoed.doedsdato!!.verdi)
                } else {
                    null
                }
            }
}
