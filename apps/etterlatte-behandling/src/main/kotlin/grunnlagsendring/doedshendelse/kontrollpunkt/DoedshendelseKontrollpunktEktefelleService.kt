@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt

import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnAntallAarGiftVedDoedsfall
import no.nav.etterlatte.grunnlagsendring.doedshendelse.finnEktefelleSafe
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.harFellesBarn
import no.nav.etterlatte.grunnlagsendring.doedshendelse.safeYearsBetween
import no.nav.etterlatte.grunnlagsendring.doedshendelse.varEktefelleVedDoedsfall
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Sivilstatus.GIFT
import no.nav.etterlatte.libs.common.person.Sivilstatus.REGISTRERT_PARTNER
import no.nav.etterlatte.libs.common.person.Sivilstatus.SEPARERT
import no.nav.etterlatte.libs.common.person.Sivilstatus.SEPARERT_PARTNER
import no.nav.etterlatte.libs.common.person.Sivilstatus.SKILT
import no.nav.etterlatte.libs.common.person.Sivilstatus.SKILT_PARTNER
import java.time.LocalDate
import kotlin.math.absoluteValue

internal class DoedshendelseKontrollpunktEktefelleService {
    fun identifiser(
        eps: PersonDTO,
        avdoed: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> = kontrollerEpsVarighet(avdoed, eps)

    private fun kontrollerEpsVarighet(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): List<DoedshendelseKontrollpunkt> =
        if (varEktefelleVedDoedsfall(avdoed, eps.foedselsnummer.verdi.value)) {
            when (val antallAarGiftVedDoedsfall = finnAntallAarGiftVedDoedsfall(avdoed, eps)) {
                null ->
                    listOf(
                        DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                            doedsdato = avdoed.doedsdato!!.verdi,
                            fnr = eps.foedselsnummer.verdi.value,
                        ),
                    )
                else -> kontrollerEktefelle(antallAarGiftVedDoedsfall, avdoed, eps)
            }
        } else {
            listOf(kontrollerTidligereEktefelle(avdoed, eps))
        }

    private fun kontrollerEktefelle(
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

    private fun kontrollerTidligereEktefelle(
        avdoed: PersonDTO,
        eps: PersonDTO,
    ): DoedshendelseKontrollpunkt {
        finnEktefelleSafe(eps)?.let { epsEktefelle ->
            if (epsEktefelle != avdoed.foedselsnummer.verdi.value) {
                return DoedshendelseKontrollpunkt.EpsErGiftPaaNytt(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                    nyEktefelleFnr = epsEktefelle,
                )
            }
        }

        val sivilstanderMedEps =
            avdoed.sivilstand
                ?.map { it.verdi }
                ?.filter { it.relatertVedSiviltilstand == eps.foedselsnummer.verdi || it.relatertVedSiviltilstand == null }
                ?: emptyList()

        val gift: LocalDate =
            sivilstanderMedEps
                .filter { it.sivilstatus in listOf(GIFT, SEPARERT, REGISTRERT_PARTNER, SEPARERT_PARTNER) }
                .sortedBy { it.gyldigFraOgMed }
                .firstOrNull()
                ?.gyldigFraOgMed
                ?: return DoedshendelseKontrollpunkt.EktefelleMedUkjentGiftemaalLengde(
                    doedsdato = avdoed.doedsdato!!.verdi,
                    fnr = avdoed.foedselsnummer.verdi.value,
                )

        val skilt: LocalDate =
            sivilstanderMedEps
                .filter { it.sivilstatus in listOf(SKILT, SKILT_PARTNER) }
                .filterNot { it.gyldigFraOgMed != null && it.gyldigFraOgMed!! < gift }
                .sortedByDescending { it.gyldigFraOgMed }
                .firstOrNull()
                ?.gyldigFraOgMed
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
