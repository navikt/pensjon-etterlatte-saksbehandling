package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.sak.SakTilgangDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.testcontainers.shaded.org.apache.commons.lang3.NotImplementedException
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

private val user =
    mockk<SaksbehandlerMedEnheterOgRoller> {
        every { enheterMedSkrivetilgang() } returns listOf(Enheter.defaultEnhet.enhetNr)
    }

fun mockedSakTilgangDao(): SakTilgangDao =
    mockk {
        every {
            hentSakMedGraderingOgSkjerming(
                any(),
            )
        } returns SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
        every {
            hentSakMedGraderingOgSkjermingPaaBehandling(
                any(),
            )
        } returns SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
        every {
            hentSakMedGraderingOgSkjermingPaaOppgave(
                any(),
            )
        } returns SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
        every {
            hentSakMedGraderingOgSkjermingPaaKlage(
                any(),
            )
        } returns SakMedGraderingOgSkjermet(sakId1, null, null, Enheter.defaultEnhet.enhetNr)
    }

fun lagContext(
    testUser: User,
    databaseContext: DatabaseKontekst =
        object : DatabaseKontekst {
            override fun activeTx(): Connection = throw IllegalArgumentException()

            override fun harIntransaction(): Boolean = throw NotImplementedException("not implemented")

            override fun <T> inTransaction(block: () -> T): T = block()
        },
    sakTilgangDao: SakTilgangDao = mockedSakTilgangDao(),
) = Context(
    testUser,
    databaseContext,
    sakTilgangDao,
    HardkodaSystembruker.testdata,
)

fun nyKontekstMedBrukerOgDatabaseContext(
    testUser: User,
    databaseContext: DatabaseKontekst,
) {
    Kontekst.set(
        lagContext(
            testUser,
            databaseContext,
        ),
    )
}

fun nyKontekstMedBrukerOgDatabase(
    testUser: User,
    dataSource: DataSource,
): Context {
    val context = lagContext(testUser, DatabaseContext(dataSource))
    Kontekst.set(context)
    return context
}

fun nyKontekstMedBruker(testUser: User) = Kontekst.set(lagContext(testUser))

fun Route.attachMockContextWithDb(
    testUser: User,
    dataSource: DataSource,
) {
    intercept(ApplicationCallPipeline.Call) {
        val context1 = lagContext(testUser, DatabaseContext(dataSource))

        withContext(
            Dispatchers.Default +
                Kontekst.asContextElement(
                    value = context1,
                ),
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}

fun Route.attachMockContextWithDbAndSakdao(
    testUser: User,
    sakTilgangDao: SakTilgangDao,
) {
    intercept(ApplicationCallPipeline.Call) {
        val context1 = lagContext(testUser, sakTilgangDao = sakTilgangDao)

        withContext(
            Dispatchers.Default +
                Kontekst.asContextElement(
                    value = context1,
                ),
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}

fun Route.attachMockContext(testUser: User? = null) {
    intercept(ApplicationCallPipeline.Call) {
        val context1 = lagContext(testUser ?: user)

        withContext(
            Dispatchers.Default +
                Kontekst.asContextElement(
                    value = context1,
                ),
        ) {
            proceed()
        }
        Kontekst.remove()
    }
}

fun mockSaksbehandler(
    ident: String = "ident",
    harRolleAttestant: Boolean = false,
    harRolleStrengtFortrolig: Boolean = false,
    harRolleEgenAnsatt: Boolean = false,
    enheter: List<Enhetsnummer> = listOf(Enheter.defaultEnhet.enhetNr),
): SaksbehandlerMedEnheterOgRoller =
    mockk<SaksbehandlerMedEnheterOgRoller> {
        every { saksbehandlerMedRoller } returns
            mockk<SaksbehandlerMedRoller> {
                every { saksbehandler } returns simpleSaksbehandler()
                every { harRolleAttestant() } returns harRolleAttestant
                every { harRolleStrengtFortrolig() } returns harRolleStrengtFortrolig
                every { harRolleEgenAnsatt() } returns harRolleEgenAnsatt
            }
        every { name() } returns ident
        every { enheter() } returns enheter
        every { brukerTokenInfo } returns mockk(relaxed = true)
    }

fun opprettBehandling(
    type: BehandlingType,
    sakId: SakId,
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    virkningstidspunkt: Virkningstidspunkt? = null,
    revurderingAarsak: Revurderingaarsak? = null,
    prosesstype: Prosesstype = Prosesstype.MANUELL,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    opphoerFraOgMed: YearMonth? = null,
    relatertBehandlingId: String? = null,
) = OpprettBehandling(
    type = type,
    sakId = sakId,
    status = status,
    soeknadMottattDato = soeknadMottattDato,
    virkningstidspunkt = virkningstidspunkt,
    revurderingsAarsak = revurderingAarsak,
    prosesstype = prosesstype,
    vedtaksloesning = kilde,
    sendeBrev = true,
    opphoerFraOgMed = opphoerFraOgMed,
    relatertBehandlingId = relatertBehandlingId,
    opprinnelse = BehandlingOpprinnelse.UKJENT,
)

fun foerstegangsbehandling(
    id: UUID = UUID.randomUUID(),
    sakId: SakId,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    persongalleri: Persongalleri = defaultPersongalleriGydligeFnr,
    gyldighetsproeving: GyldighetsResultat? = null,
    virkningstidspunkt: Virkningstidspunkt? = null,
    utlandstilknytning: Utlandstilknytning? = null,
    boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet? = null,
    kommerBarnetTilgode: KommerBarnetTilgode? = null,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    enhet: Enhetsnummer = Enheter.defaultEnhet.enhetNr,
    relatertBehandlingId: String? = null,
    opphoerFraOgMed: YearMonth? = null,
    adressebeskyttelse: AdressebeskyttelseGradering? = null,
    erSkjermet: Boolean = false,
) = Foerstegangsbehandling(
    id = id,
    sak =
        Sak(
            ident = persongalleri.soeker,
            sakType = sakType,
            id = sakId,
            enhet = enhet,
            adressebeskyttelse = adressebeskyttelse,
            erSkjermet = erSkjermet,
        ),
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    soeknadMottattDato = soeknadMottattDato,
    gyldighetsproeving = gyldighetsproeving,
    virkningstidspunkt = virkningstidspunkt,
    utlandstilknytning = utlandstilknytning,
    boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
    kommerBarnetTilgode = kommerBarnetTilgode,
    vedtaksloesning = kilde,
    sendeBrev = true,
    opphoerFraOgMed = opphoerFraOgMed,
    relatertBehandlingId = relatertBehandlingId,
    opprinnelse = BehandlingOpprinnelse.UKJENT,
)

fun sak(
    sakId: SakId = sakId1,
    sakType: SakType = SakType.OMSTILLINGSSTOENAD,
    enhet: Enhetsnummer = Enheter.defaultEnhet.enhetNr,
    adressebeskyttelse: AdressebeskyttelseGradering? = null,
    erSkjermet: Boolean = false,
): Sak =
    Sak(
        ident = "Soeker",
        sakType = sakType,
        id = sakId,
        enhet = enhet,
        adressebeskyttelse = adressebeskyttelse,
        erSkjermet = erSkjermet,
    )

fun revurdering(
    id: UUID = UUID.randomUUID(),
    sakId: SakId,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    persongalleri: Persongalleri = defaultPersongalleriGydligeFnr,
    revurderingAarsak: Revurderingaarsak,
    kommerBarnetTilgode: KommerBarnetTilgode = kommerBarnetTilgode(id),
    virkningstidspunkt: Virkningstidspunkt? = null,
    utlandstilknytning: Utlandstilknytning? = null,
    boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet? = null,
    prosesstype: Prosesstype = Prosesstype.MANUELL,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    enhet: Enhetsnummer = Enheter.defaultEnhet.enhetNr,
    revurderingInfo: RevurderingInfoMedBegrunnelse? = null,
    begrunnelse: String? = null,
    relatertBehandlingId: String? = null,
    opphoerFraOgMed: YearMonth? = null,
    tidligereFamiliepleier: TidligereFamiliepleier? = null,
    adressebeskyttelse: AdressebeskyttelseGradering? = null,
    erSkjermet: Boolean = false,
) = Revurdering.opprett(
    id = id,
    sak =
        Sak(
            ident = persongalleri.soeker,
            sakType = sakType,
            id = sakId,
            enhet = enhet,
            adressebeskyttelse = adressebeskyttelse,
            erSkjermet = erSkjermet,
        ),
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    kommerBarnetTilgode = kommerBarnetTilgode,
    virkningstidspunkt = virkningstidspunkt,
    utlandstilknytning = utlandstilknytning,
    boddEllerArbeidetUtlandet = boddEllerArbeidetUtlandet,
    revurderingsaarsak = revurderingAarsak,
    prosesstype = prosesstype,
    vedtaksloesning = kilde,
    revurderingInfo = revurderingInfo,
    begrunnelse = begrunnelse,
    relatertBehandlingId = relatertBehandlingId,
    sendeBrev = true,
    opphoerFraOgMed = opphoerFraOgMed,
    tidligereFamiliepleier = tidligereFamiliepleier,
    opprinnelse = BehandlingOpprinnelse.UKJENT,
)

val soeker = "11057523044"
val defaultPersongalleriGydligeFnr =
    Persongalleri(
        soeker,
        INNSENDER_FOEDSELSNUMMER.value,
        listOf(HELSOESKEN2_FOEDSELSNUMMER.value, HALVSOESKEN_FOEDSELSNUMMER.value),
        listOf(AVDOED_FOEDSELSNUMMER.value),
        listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
    )

fun persongalleri(
    soeker: String = no.nav.etterlatte.soeker,
    innsender: String = INNSENDER_FOEDSELSNUMMER.value,
    soesken: List<String> = listOf(HELSOESKEN2_FOEDSELSNUMMER.value, HALVSOESKEN_FOEDSELSNUMMER.value),
    avdoed: List<String> = listOf(AVDOED_FOEDSELSNUMMER.value),
    gjenlevende: List<String> = listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
) = Persongalleri(
    soeker = soeker,
    innsender = innsender,
    soesken = soesken,
    avdoed = avdoed,
    gjenlevende = gjenlevende,
)

fun samsvarMellomPdlOgGrunnlagDoed(doedsdato: LocalDate?) = samsvarDoedsdatoer(doedsdato, doedsdato)

fun ikkeSamsvarMellomPdlOgGrunnlagDoed(doedsdato: LocalDate?) = samsvarDoedsdatoer(doedsdato, null)

fun grunnlagsendringshendelseMedSamsvar(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = sakId1,
    type: GrunnlagsendringsType = GrunnlagsendringsType.DOEDSFALL,
    opprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    gjelderPerson: String,
    status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    behandlingId: UUID? = null,
    hendelseGjelderRolle: Saksrolle = Saksrolle.SOEKER,
    samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag?,
) = Grunnlagsendringshendelse(
    id = id,
    sakId = sakId,
    type = type,
    opprettet = opprettet,
    status = status,
    behandlingId = behandlingId,
    hendelseGjelderRolle = hendelseGjelderRolle,
    samsvarMellomKildeOgGrunnlag = samsvarMellomKildeOgGrunnlag,
    gjelderPerson = gjelderPerson,
)

fun grunnlagsinformasjonDoedshendelse(
    avdoedFnr: String = "12345678911",
    doedsdato: LocalDate = LocalDate.of(2022, 1, 1),
    endringstype: Endringstype = Endringstype.OPPRETTET,
) = DoedshendelsePdl(hendelseId = "1", fnr = avdoedFnr, doedsdato = doedsdato, endringstype = endringstype)

fun grunnlagsinformasjonUtflyttingshendelse(
    fnr: String = "12345678911",
    tilflyttingsLand: String = "Sverige",
    utflyttingsdato: LocalDate = LocalDate.of(2022, 8, 8),
) = UtflyttingsHendelse(
    hendelseId = "1",
    endringstype = Endringstype.OPPRETTET,
    fnr = fnr,
    tilflyttingsLand = tilflyttingsLand,
    tilflyttingsstedIUtlandet = null,
    utflyttingsdato = utflyttingsdato,
)

fun grunnlagsinformasjonForelderBarnRelasjonHendelse(
    fnr: String = "12345678911",
    relatertPersonsIdent: String = "98765432198",
    relatertPersonsRolle: String = "MOR",
    minRolleForPerson: String = "BARN",
) = ForelderBarnRelasjonHendelse(
    hendelseId = "1",
    endringstype = Endringstype.OPPRETTET,
    fnr = fnr,
    relatertPersonsIdent = relatertPersonsIdent,
    relatertPersonsRolle = relatertPersonsRolle,
    minRolleForPerson = minRolleForPerson,
    relatertPersonUtenFolkeregisteridentifikator = null,
)

fun grunnlagsOpplysningMedPersonopplysning(personopplysning: Person) =
    Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1"),
        opplysningType = Opplysningstype.DOEDSDATO,
        meta = ObjectMapper().createObjectNode(),
        opplysning = personopplysning.toJsonNode(),
        attestering = null,
        fnr = null,
        periode = null,
    )

fun personOpplysning(
    doedsdato: LocalDate? = null,
    foedselsdato: LocalDate? = LocalDate.parse("2020-06-10"),
) = Person(
    fornavn = "Test",
    etternavn = "Testulfsen",
    foedselsnummer = SOEKER_FOEDSELSNUMMER,
    foedselsdato = foedselsdato,
    foedselsaar = 1985,
    foedeland = null,
    doedsdato = doedsdato,
    adressebeskyttelse = AdressebeskyttelseGradering.UGRADERT,
    bostedsadresse = null,
    deltBostedsadresse = null,
    kontaktadresse = null,
    oppholdsadresse = null,
    sivilstatus = null,
    sivilstand = null,
    statsborgerskap = null,
    utland = null,
    familieRelasjon = FamilieRelasjon(null, null, null),
    avdoedesBarn = null,
    avdoedesBarnUtenIdent = null,
    vergemaalEllerFremtidsfullmakt = null,
    pdlStatsborgerskap = null,
)

fun kommerBarnetTilgode(
    behandlingId: UUID,
    svar: JaNei = JaNei.JA,
    begrunnelse: String = "En begrunnelse",
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler.create("S01"),
) = KommerBarnetTilgode(svar, begrunnelse, kilde, behandlingId)

val KONTANT_FOT = Folkeregisteridentifikator.of("10418305857")
val JOVIAL_LAMA = Folkeregisteridentifikator.of("09498230323")
val LITE_BARN = Folkeregisteridentifikator.of("22511075258")

fun mockPerson(
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
    vergemaal: List<VergemaalEllerFremtidsfullmakt>? = null,
) = PersonDTO(
    fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
    mellomnavn = OpplysningDTO(verdi = "Mellom", opplysningsid = null),
    etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
    foedselsnummer = OpplysningDTO(KONTANT_FOT, null),
    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
    foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
    foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
    doedsdato = null,
    adressebeskyttelse = null,
    bostedsadresse =
        listOf(
            OpplysningDTO(
                Adresse(
                    type = AdresseType.VEGADRESSE,
                    aktiv = true,
                    coAdresseNavn = "Hos Geir",
                    adresseLinje1 = "Testveien 4",
                    adresseLinje2 = null,
                    adresseLinje3 = null,
                    postnr = "1234",
                    poststed = null,
                    land = "NOR",
                    kilde = "FREG",
                    gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                    gyldigTilOgMed = null,
                ),
                UUID.randomUUID().toString(),
            ),
        ),
    deltBostedsadresse = emptyList(),
    kontaktadresse = emptyList(),
    oppholdsadresse = emptyList(),
    sivilstatus = null,
    sivilstand = null,
    statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
    utland = utland?.let { OpplysningDTO(utland, UUID.randomUUID().toString()) },
    familieRelasjon = familieRelasjon?.let { OpplysningDTO(it, UUID.randomUUID().toString()) },
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaal?.map { OpplysningDTO(it, UUID.randomUUID().toString()) },
    pdlStatsborgerskap = null,
)

fun mockDoedshendelsePerson(
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
): PersonDoedshendelseDto =
    mockPerson(utland, familieRelasjon).let {
        PersonDoedshendelseDto(
            foedselsnummer = it.foedselsnummer,
            foedselsdato = it.foedselsdato,
            foedselsaar = it.foedselsaar,
            doedsdato = it.doedsdato,
            bostedsadresse = it.bostedsadresse,
            deltBostedsadresse = it.deltBostedsadresse,
            kontaktadresse = it.kontaktadresse,
            oppholdsadresse = it.oppholdsadresse,
            sivilstand = it.sivilstand,
            utland = it.utland,
            familieRelasjon = it.familieRelasjon,
            avdoedesBarn = it.avdoedesBarn,
        )
    }

fun kommerBarnetTilGodeVurdering(behandlingId: UUID) =
    KommerBarnetTilgode(
        svar = JaNei.JA,
        begrunnelse = "begrunnelse",
        kilde = Grunnlagsopplysning.Saksbehandler(ident = "ident", tidspunkt = Tidspunkt(instant = Instant.now())),
        behandlingId = behandlingId,
    )

fun virkningstidspunktVurdering() =
    Virkningstidspunkt(
        YearMonth.of(2023, 1),
        Grunnlagsopplysning.Saksbehandler(
            "ident",
            Tidspunkt.now(),
        ),
        "begrunnelse",
    )

fun gyldighetsresultatVurdering() =
    GyldighetsResultat(
        VurderingsResultat.OPPFYLT,
        vurderinger = emptyList(),
        vurdertDato = Tidspunkt.now().toLocalDatetimeUTC(),
    )
