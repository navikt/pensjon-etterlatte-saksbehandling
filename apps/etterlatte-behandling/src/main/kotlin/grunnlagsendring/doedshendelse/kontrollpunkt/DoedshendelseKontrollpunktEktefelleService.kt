@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnAntallAarGiftVedDoedsfall
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harFellesBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.safeYearsBetween
import no.nav.etterlatte.grunnlagsendring.doedshendelse.varEktefelleVedDoedsfall
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Sivilstatus.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.math.absoluteValue

internal class DoedshendelseKontrollpunktEktefelleService {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val sivilstanderMedEps =
            avdoed.sivilstand
                ?.map { it.verdi }
                ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi || it.relatertVedSiviltilstand == null }
                ?: emptyList()

        val gift: LocalDate =
            sivilstanderMedEps
                .filter { it.sivilstatus in listOf(GIFT, SEPARERT, REGISTRERT_PARTNER, SEPARERT_PARTNER) }
                .sortedBy { it.gyldigFraOgMed }
                .firstOrNull()?.gyldigFraOgMed
                ?: return DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                )

        val skilt: LocalDate =
            sivilstanderMedEps
                .filter { it.sivilstatus in listOf(SKILT, SKILT_PARTNER) }
                .filterNot { it.gyldigFraOgMed != null && it.gyldigFraOgMed!! < gift }
                .sortedByDescending { it.gyldigFraOgMed }
                .firstOrNull()?.gyldigFraOgMed
                ?: return DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                )

        val antallAarGift = safeYearsBetween(gift, skilt).absoluteValue

        if (harFellesBarn(avdoed, eps)) {
            return if (antallAarGift >= 15) {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn15AarFellesBarn(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                )
            } else {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMindreEnn15AarFellesBarn
            }
        } else {
            return if (antallAarGift >= 25) {
                DoedshendelseKontrollpunkt.TidligereEpsGiftMerEnn25Aar(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                )
            } else {
                DoedshendelseKontrollpunkt.TidligereEpsGiftUnder25AarUtenFellesBarn
            }
        }
    }
}
