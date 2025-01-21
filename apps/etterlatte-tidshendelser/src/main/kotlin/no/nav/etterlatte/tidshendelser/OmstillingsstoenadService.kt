package no.nav.etterlatte.tidshendelser

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class OmstillingsstoenadService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<SakId> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")

        val monthsToSubtract: Long =
            when (jobb.type) {
                JobbType.OMS_DOED_3AAR -> 36
                JobbType.OMS_DOED_5AAR -> 60
                JobbType.OMS_DOED_4MND -> 4
                JobbType.OMS_DOED_6MND -> 6
                JobbType.OMS_DOED_10MND -> 9
                JobbType.OMS_DOED_12MND -> 10
                JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK -> 6
                else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
            }

        val aktuellMaaned = jobb.behandlingsmaaned.minusMonths(monthsToSubtract)
        val sakerMedAvdoedDoedsdatoIMaaned =
            runBlocking {
                retryOgPakkUt {
                    grunnlagKlient.hentSakerForDoedsfall(doedsfallsmaaned = aktuellMaaned)
                }
            }
        logger.info("Hentet ${sakerMedAvdoedDoedsdatoIMaaned.size} saker hvor dødsfall forekom i $aktuellMaaned")

        // For de som har omstillingsstønad til tidligere familiepleier er det ikke dødsfallsmåned til avdøde som er
        // relevant for disse jobbene (det er ingen avdød koblet til saken). Det er i stedet måneden pleieforholdet
        // opphørte som er relevant, med samme "offset" som for dødsfallsmåned. Denne opplysningen ligger i behandling
        val sakerMedPleieforholdetOpphoerteIMaaned =
            runBlocking {
                retryOgPakkUt {
                    behandlingKlient.hentSakerForPleieforholdetOpphoerte(aktuellMaaned)
                }
            }
        logger.info("Hentet ${sakerMedPleieforholdetOpphoerteIMaaned.size} saker hvor pleieforholdet opphørte i $aktuellMaaned")
        val alleSaker = sakerMedAvdoedDoedsdatoIMaaned + sakerMedPleieforholdetOpphoerteIMaaned
        // filtrer bort saker som ikke er aktuelle
        val sakerMap =
            runBlocking {
                retryOgPakkUt {
                    behandlingKlient.hentSaker(alleSaker)
                }
            }
        val aktuelleSaker =
            alleSaker.filter {
                sakerMap[it]?.sakType == jobb.type.sakType
            }
        logger.info(
            "Hentet ${alleSaker.size} saker hvor dødsfall/pleieforholdet opphørte forekom i $aktuellMaaned, med " +
                "${aktuelleSaker.size} saker med riktig saktype",
        )

        if (aktuelleSaker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, aktuelleSaker, Steg.IDENTIFISERT_SAK)
        }

        return aktuelleSaker
    }
}
