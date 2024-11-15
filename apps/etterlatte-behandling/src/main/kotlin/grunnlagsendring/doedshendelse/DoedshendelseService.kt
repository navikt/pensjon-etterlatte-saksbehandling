package no.nav.etterlatte.grunnlagsendring.doedshendelse

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.sikkerLogg
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl as PdlDoedshendelse

class DoedshendelseService(
    private val doedshendelseDao: DoedshendelseDao,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun settHendelseTilFerdigOgOppdaterBrevId(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert) =
        doedshendelseDao.oppdaterBrevDistribuertDoedshendelse(doedshendelseBrevDistribuert)

    fun opprettDoedshendelseForBeroertePersoner(doedshendelse: PdlDoedshendelse) {
        logger.info("Mottok dødsmelding fra PDL, finner berørte personer og lagrer ned dødsmelding.")

        val avdoed =
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                doedshendelse.fnr,
                PersonRolle.AVDOED,
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )

        if (avdoed.doedsdato == null) {
            sikkerLogg.info("Mottok dødshendelse for ${avdoed.foedselsnummer}, men personen er i live i følge PDL.")
            logger.info("Mottok dødshendelse fra PDL for en levende person. Avbryter. Se sikkerlogg for detaljer.")
            return
        }
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        val gyldigeDoedshendelserForAvdoed =
            inTransaction { doedshendelseDao.hentDoedshendelserForPerson(avdoedFnr) }
                .filter { it.utfall !== Utfall.AVBRUTT }

        val barn = finnBeroerteBarn(avdoed)
        val samboere = finnSamboereForAvdoedMedFellesBarn(avdoed)
        val ektefeller = finnBeroerteEktefeller(avdoed, samboere)
        val alleBeroerte = barn + ektefeller + samboere

        if (gyldigeDoedshendelserForAvdoed.isEmpty()) {
            sikkerLogg.info("Fant ${alleBeroerte.size} berørte personer for avdød (${avdoed.foedselsnummer.verdi.value})")
            inTransaction {
                lagreDoedshendelser(alleBeroerte, avdoed, doedshendelse.endringstype)
            }
        } else {
            haandterEksisterendeHendelser(doedshendelse, gyldigeDoedshendelserForAvdoed, avdoed, alleBeroerte)
        }
    }

    private fun haandterEksisterendeHendelser(
        doedshendelse: PdlDoedshendelse,
        doedshendelserForAvdoed: List<DoedshendelseInternal>,
        avdoed: PersonDTO,
        beroerte: List<PersonFnrMedRelasjon>,
    ) {
        val aapneDoedshendelser = doedshendelserForAvdoed.filter { it.status !== Status.FERDIG }

        when (doedshendelse.endringstype) {
            Endringstype.OPPRETTET, Endringstype.KORRIGERT -> {
                inTransaction {
                    aapneDoedshendelser
                        .map { it.tilOppdatert(avdoed.doedsdato!!.verdi, doedshendelse.endringstype) }
                        .forEach { doedshendelseDao.oppdaterDoedshendelse(it) }

                    haandterNyeBerorte(doedshendelserForAvdoed, beroerte, avdoed, doedshendelse.endringstype)
                }
            }

            Endringstype.ANNULLERT -> {
                inTransaction {
                    aapneDoedshendelser
                        .map { it.tilAnnulert() }
                        .forEach { doedshendelseDao.oppdaterDoedshendelse(it) }
                }
            }

            Endringstype.OPPHOERT -> throw RuntimeException("Fikk opphør på dødshendelse, skal ikke skje ifølge PDL docs")
        }
    }

    private fun haandterNyeBerorte(
        doedshendelserForAvdoed: List<DoedshendelseInternal>,
        beroerte: List<PersonFnrMedRelasjon>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val eksisterendeBeroerte = doedshendelserForAvdoed.map { it.beroertFnr }
        val nyeBeroerte =
            beroerte
                .map { PersonFnrMedRelasjon(it.fnr, it.relasjon) }
                .filter { !eksisterendeBeroerte.contains(it.fnr) }
        nyeBeroerte.forEach { person ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = person.fnr,
                    relasjon = person.relasjon,
                    endringstype = endringstype,
                ),
            )
        }
    }

    private fun lagreDoedshendelser(
        beroerte: List<PersonFnrMedRelasjon>,
        avdoed: PersonDTO,
        endringstype: Endringstype,
    ) {
        val avdoedFnr = avdoed.foedselsnummer.verdi.value
        beroerte.forEach { person ->
            doedshendelseDao.opprettDoedshendelse(
                DoedshendelseInternal.nyHendelse(
                    avdoedFnr = avdoedFnr,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = person.fnr,
                    relasjon = person.relasjon,
                    endringstype = endringstype,
                ),
            )
        }
        doedshendelseDao.opprettDoedshendelse(
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = avdoedFnr,
                avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                beroertFnr = avdoedFnr,
                relasjon = Relasjon.AVDOED,
                endringstype = endringstype,
            ),
        )
    }

    private fun finnBeroerteBarn(avdoed: PersonDTO): List<PersonFnrMedRelasjon> {
        val maanedenEtterDoedsfall =
            avdoed.doedsdato!!
                .verdi
                .plusMonths(1)
                .withDayOfMonth(1)

        return with(avdoed.avdoedesBarn ?: emptyList()) {
            this
                .filter { barn -> barn.doedsdato == null }
                .filter { barn -> barn.under20PaaDato(maanedenEtterDoedsfall) }
                .map { PersonFnrMedRelasjon(it.foedselsnummer.value, Relasjon.BARN) }
        }
    }

    private fun finnSamboereForAvdoedMedFellesBarn(avdoed: PersonDTO): List<PersonFnrMedRelasjon> {
        val avdoedesBarn = avdoed.avdoedesBarn
        val andreForeldreForAvdoedesBarn =
            avdoedesBarn
                ?.mapNotNull { barn ->
                    val annenForelder =
                        barn.familieRelasjon
                            ?.foreldre
                            ?.filter { it.value != avdoed.foedselsnummer.verdi.value }
                            ?.map { it.value }
                    logger.info("Fant annen forelder til barn ${barn.foedselsnummer.value}: $annenForelder")
                    annenForelder
                }?.flatten()
                ?.distinct()
                ?.filterNot {
                    varEktefelleVedDoedsfall(avdoed, it).also { varEktefelle ->
                        logger.info("$it var ektefelle med avdød (${avdoed.foedselsnummer.verdi}): $varEktefelle")
                    }
                }

        logger.info("Fant ${andreForeldreForAvdoedesBarn?.size} andre foreldre for avdødes (${avdoed.foedselsnummer.verdi}) barn.")

        return harSammeAdresseSomAvdoed(avdoed, andreForeldreForAvdoedesBarn)
    }

    private fun harSammeAdresseSomAvdoed(
        avdoed: PersonDTO,
        andreForeldreForAvdoedesBarn: List<String>?,
    ): List<PersonFnrMedRelasjon> =
        andreForeldreForAvdoedesBarn
            ?.map {
                val annenForelder =
                    runBlocking {
                        pdlTjenesterKlient.hentPdlModellForSaktype(it, PersonRolle.TILKNYTTET_BARN, SakType.OMSTILLINGSSTOENAD)
                    }
                AvdoedOgAnnenForelderMedFellesbarn(avdoed, annenForelder)
            }?.filter { erSamboere(it) }
            ?.map { PersonFnrMedRelasjon(it.gjenlevendeForelder.foedselsnummer.verdi.value, Relasjon.SAMBOER) }
            ?: emptyList()

    private fun erSamboere(avdoedOgAnnenForelderMedFellesbarn: AvdoedOgAnnenForelderMedFellesbarn): Boolean {
        val gjenlevendeBosted =
            avdoedOgAnnenForelderMedFellesbarn
                .gjenlevendeForelder.bostedsadresse
                ?.map { it.verdi }
                ?.firstOrNull { it.aktiv }
        val avdoedBosted =
            avdoedOgAnnenForelderMedFellesbarn
                .avdoedPerson.bostedsadresse
                ?.map { it.verdi }
                ?.sortedByDescending { it.gyldigFraOgMed }
                ?.firstOrNull()

        val adresserLike = gjenlevendeBosted != null && avdoedBosted != null && isAdresserLike(gjenlevendeBosted, avdoedBosted)
        logger.info(
            "Avdød (${avdoedOgAnnenForelderMedFellesbarn.avdoedPerson.foedselsnummer.verdi}) og annen forelder " +
                "(${avdoedOgAnnenForelderMedFellesbarn.gjenlevendeForelder.foedselsnummer.verdi}) har samme adresse: $adresserLike",
        )
        return adresserLike
    }

    private fun isAdresserLike(
        adresse1: Adresse,
        adresse2: Adresse,
    ) = adresse1.adresseLinje1 == adresse2.adresseLinje1 &&
        adresse1.adresseLinje2 == adresse2.adresseLinje2 &&
        adresse1.adresseLinje3 == adresse2.adresseLinje3 &&
        adresse1.postnr == adresse2.postnr

    private fun finnBeroerteEktefeller(
        avdoed: PersonDTO,
        samboere: List<PersonFnrMedRelasjon>,
    ): List<PersonFnrMedRelasjon> {
        val avdoedesSivilstander = avdoed.sivilstand ?: emptyList()

        return avdoedesSivilstander
            .asSequence()
            .filter { it.verdi.relatertVedSiviltilstand?.value != null }
            .filter {
                it.verdi.sivilstatus in
                    listOf(
                        Sivilstatus.GIFT,
                        Sivilstatus.SEPARERT,
                        Sivilstatus.SKILT,
                        Sivilstatus.REGISTRERT_PARTNER,
                        Sivilstatus.SEPARERT_PARTNER,
                        Sivilstatus.SKILT_PARTNER,
                    )
            }.map { PersonFnrMedRelasjon(it.verdi.relatertVedSiviltilstand!!.value, Relasjon.EKTEFELLE) }
            .filter { ektefelle -> samboere.none { samboer -> samboer.fnr == ektefelle.fnr } }
            .distinct()
            .toList()
    }
}

private fun Person.under20PaaDato(dato: LocalDate): Boolean {
    // Dersom vi ikke har en fødselsdato antar vi at personen kan ha bursdag på nyttårsaften,
    // for å sikre at vi får med alle som er under 20 år.
    val benyttetFoedselsdato = foedselsdato ?: LocalDate.of(foedselsaar, 12, 31)

    return ChronoUnit.YEARS.between(benyttetFoedselsdato, dato).absoluteValue < 20
}
