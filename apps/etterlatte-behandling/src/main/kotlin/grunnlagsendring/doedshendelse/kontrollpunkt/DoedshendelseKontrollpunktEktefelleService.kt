@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnAntallAarGiftVedDoedsfall
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harFellesBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.safeYearsBetween
import no.nav.etterlatte.grunnlagsendring.doedshendelse.varEktefelleVedDoedsfall
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
        return if (varEktefelleVedDoedsfall(avdoed, eps.foedselsnummer.verdi.value)) {
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
        val giftSivilstander =
            avdoed.sivilstand
                ?.map { it.verdi }
                ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi }
                ?.filter { it.sivilstatus in listOf(GIFT, REGISTRERT_PARTNER) }
                ?.sortedBy { it.gyldigFraOgMed }

        if (giftSivilstander.isNullOrEmpty()) {
            throw IllegalStateException("Fant ingen gift sivilstander for avdoed med tidligere ektefelle")
        }

        val gift = giftSivilstander.first().gyldigFraOgMed

        val skiltSivilstander =
            avdoed.sivilstand
                ?.map { it.verdi }
                ?.filter { it.sivilstatus in listOf(SKILT, SKILT_PARTNER) }
                ?.filterNot { it.gyldigFraOgMed != null && gift != null && it.gyldigFraOgMed!! < gift }
                ?.sortedBy { it.gyldigFraOgMed }
                ?: emptyList()

        val skilt =
            when {
                skiltSivilstander.isEmpty() -> null
                skiltSivilstander.size == 1 -> skiltSivilstander.first().gyldigFraOgMed
                skiltSivilstander.any { it.relatertVedSiviltilstand?.value == eps.foedselsnummer.verdi.value } -> {
                    skiltSivilstander.first { it.relatertVedSiviltilstand?.value == eps.foedselsnummer.verdi.value }.gyldigFraOgMed
                }
                else -> skiltSivilstander.first().gyldigFraOgMed
            }

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
}
