package no.nav.etterlatte

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.routing.Route
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
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.libs.common.Vedtaksloesning
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
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

private val user = mockk<SaksbehandlerMedEnheterOgRoller>()

fun Route.attachMockContext(saksbehandlerMedEnheterOgRoller: SaksbehandlerMedEnheterOgRoller? = null) {
    intercept(ApplicationCallPipeline.Call) {
        val context1 =
            Context(
                saksbehandlerMedEnheterOgRoller ?: user,
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            )

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

fun opprettBehandling(
    type: BehandlingType,
    sakId: Long,
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    virkningstidspunkt: Virkningstidspunkt? = null,
    revurderingAarsak: Revurderingaarsak? = null,
    opphoerAarsaker: List<String>? = null,
    fritekstAarsak: String? = null,
    prosesstype: Prosesstype = Prosesstype.MANUELL,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
) = OpprettBehandling(
    type = type,
    sakId = sakId,
    status = status,
    soeknadMottattDato = soeknadMottattDato,
    virkningstidspunkt = virkningstidspunkt,
    revurderingsAarsak = revurderingAarsak,
    opphoerAarsaker = opphoerAarsaker,
    fritekstAarsak = fritekstAarsak,
    prosesstype = prosesstype,
    kilde = kilde,
)

fun foerstegangsbehandling(
    id: UUID = UUID.randomUUID(),
    sakId: Long,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    persongalleri: Persongalleri = persongalleri(),
    gyldighetsproeving: GyldighetsResultat? = null,
    virkningstidspunkt: Virkningstidspunkt? = null,
    utlandstilknytning: Utlandstilknytning? = null,
    boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet? = null,
    kommerBarnetTilgode: KommerBarnetTilgode? = null,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    enhet: String = Enheter.defaultEnhet.enhetNr,
) = Foerstegangsbehandling(
    id = id,
    sak =
        Sak(
            ident = persongalleri.soeker,
            sakType = sakType,
            id = sakId,
            enhet = enhet,
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
    kilde = kilde,
)

fun revurdering(
    id: UUID = UUID.randomUUID(),
    sakId: Long,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    persongalleri: Persongalleri = persongalleri(),
    revurderingAarsak: Revurderingaarsak,
    kommerBarnetTilgode: KommerBarnetTilgode = kommerBarnetTilgode(id),
    virkningstidspunkt: Virkningstidspunkt? = null,
    utlandstilknytning: Utlandstilknytning? = null,
    boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet? = null,
    prosesstype: Prosesstype = Prosesstype.MANUELL,
    kilde: Vedtaksloesning = Vedtaksloesning.GJENNY,
    enhet: String = Enheter.defaultEnhet.enhetNr,
    revurderingInfo: RevurderingInfoMedBegrunnelse? = null,
    begrunnelse: String? = null,
) = Revurdering.opprett(
    id = id,
    sak =
        Sak(
            ident = persongalleri.soeker,
            sakType = SakType.BARNEPENSJON,
            id = sakId,
            enhet = enhet,
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
    kilde = kilde,
    revurderingInfo = revurderingInfo,
    begrunnelse = begrunnelse,
)

fun persongalleri(
    soeker: String = "Soeker",
    innsender: String = "Innsender",
    soesken: List<String> = listOf("Soester", "Bror"),
    avdoed: List<String> = listOf("Avdoed"),
    gjenlevende: List<String> = listOf("Gjenlevende"),
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
    sakId: Long = 1,
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
) = Doedshendelse(hendelseId = "1", fnr = avdoedFnr, doedsdato = doedsdato, endringstype = endringstype)

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
        opplysning = personopplysning,
        attestering = null,
        fnr = null,
        periode = null,
    )

fun personOpplysning(doedsdato: LocalDate? = null) =
    Person(
        fornavn = "Test",
        etternavn = "Testulfsen",
        foedselsnummer = SOEKER_FOEDSELSNUMMER,
        foedselsdato = LocalDate.parse("2020-06-10"),
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
    deltBostedsadresse = listOf(),
    kontaktadresse = listOf(),
    oppholdsadresse = listOf(),
    sivilstatus = null,
    sivilstand = null,
    statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
    utland = utland?.let { OpplysningDTO(utland, UUID.randomUUID().toString()) },
    familieRelasjon = familieRelasjon?.let { OpplysningDTO(it, UUID.randomUUID().toString()) },
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaal?.map { OpplysningDTO(it, UUID.randomUUID().toString()) },
    pdlStatsborgerskap = null,
)

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
        vurderinger = listOf(),
        vurdertDato = Tidspunkt.now().toLocalDatetimeUTC(),
    )

@Suppress("ktlint:standard:max-line-length")
val saksbehandlerToken =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImF6dXJlLWlkIGZvciBzYWtzYmVoYW5kbGVyIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJOQVZpZGVudCI6IlNha3NiZWhhbmRsZXIwMSJ9.271mDij4YsO4Kk8w8AvX5BXxlEA8U-UAOtdG1Ix_kQY"
