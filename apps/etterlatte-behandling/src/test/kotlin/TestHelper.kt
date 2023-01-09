package no.nav.etterlatte

import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.ManueltOpphoer
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.grunnlagsendring.samsvarAnsvarligeForeldre
import no.nav.etterlatte.grunnlagsendring.samsvarBarn
import no.nav.etterlatte.grunnlagsendring.samsvarDoedsdatoer
import no.nav.etterlatte.grunnlagsendring.samsvarUtflytting
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.SamsvarMellomPdlOgGrunnlag
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun foerstegangsbehandling(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    soeknadMottattDato: LocalDateTime = LocalDateTime.now(),
    persongalleri: Persongalleri = persongalleri(),
    gyldighetsproeving: GyldighetsResultat? = null,
    virkningstidspunkt: Virkningstidspunkt? = null,
    kommerBarnetTilgode: KommerBarnetTilgode? = null,
    vilkaarStatus: VilkaarsvurderingUtfall? = null
) = Foerstegangsbehandling(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    soeknadMottattDato = soeknadMottattDato,
    persongalleri = persongalleri,
    gyldighetsproeving = gyldighetsproeving,
    virkningstidspunkt = virkningstidspunkt,
    kommerBarnetTilgode = kommerBarnetTilgode,
    vilkaarUtfall = vilkaarStatus
)

fun revurdering(
    id: UUID = UUID.randomUUID(),
    sak: Long,
    behandlingOpprettet: LocalDateTime = LocalDateTime.now(),
    sistEndret: LocalDateTime = LocalDateTime.now(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    persongalleri: Persongalleri = persongalleri(),
    revurderingAarsak: RevurderingAarsak,
    kommerBarnetTilgode: KommerBarnetTilgode = kommerBarnetTilgode(),
    vilkaarStatus: VilkaarsvurderingUtfall? = VilkaarsvurderingUtfall.OPPFYLT
) = Revurdering(
    id = id,
    sak = sak,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    persongalleri = persongalleri,
    revurderingsaarsak = revurderingAarsak,
    kommerBarnetTilgode = kommerBarnetTilgode,
    vilkaarUtfall = vilkaarStatus
)

fun manueltOpphoer(
    sak: Long = 1,
    behandlingId: UUID = UUID.randomUUID(),
    persongalleri: Persongalleri = persongalleri(),
    opphoerAarsaker: List<ManueltOpphoerAarsak> = listOf(
        ManueltOpphoerAarsak.SOESKEN_DOED,
        ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
    ),
    fritekstAarsak: String? = "Umulig å revurdere i nytt saksbehandlingssystem"
) = ManueltOpphoer(
    id = behandlingId,
    sak = sak,
    behandlingOpprettet = LocalDateTime.now(),
    sistEndret = LocalDateTime.now(),
    status = BehandlingStatus.OPPRETTET,
    persongalleri = persongalleri,
    opphoerAarsaker = opphoerAarsaker,
    fritekstAarsak = fritekstAarsak
)

fun persongalleri(
    soeker: String = "Soeker",
    innsender: String = "Innsender",
    soesken: List<String> = listOf("Soester", "Bror"),
    avdoed: List<String> = listOf("Avdoed"),
    gjenlevende: List<String> = listOf("Gjenlevende")
) = Persongalleri(
    soeker = soeker,
    innsender = innsender,
    soesken = soesken,
    avdoed = avdoed,
    gjenlevende = gjenlevende
)

fun utlandUtflyttingTilSverige() = Utland(
    innflyttingTilNorge = null,
    utflyttingFraNorge = listOf(
        UtflyttingFraNorge("Sverige", LocalDate.of(2022, 10, 10))
    )
)

fun tomUtland() = Utland(innflyttingTilNorge = null, utflyttingFraNorge = null)

fun toFoedselsnummer() = listOf(
    Foedselsnummer.of("11057523044"),
    Foedselsnummer.of("03126822412")
)

fun tomFoedselsnummer() = null

fun samsvarMellomPdlOgGrunnlagDoed(
    doedsdato: LocalDate?
) = samsvarDoedsdatoer(doedsdato, doedsdato)

fun ikkeSamsvarMellomPdlOgGrunnlagDoed(
    doedsdato: LocalDate?
) = samsvarDoedsdatoer(doedsdato, null)

fun samsvarMellomPdlOgGrunnlagUtflytting(
    utflytting: Utland?
) = samsvarUtflytting(utflytting, utflytting)

fun ikkeSamsvarMellomPdlOgGrunnlagUtflytting(
    utflytting: Utland?
) = samsvarUtflytting(utflytting, null)

fun samsvarMellomPdlOgGrunnlagBarn(
    barn: List<Foedselsnummer>
) = samsvarBarn(barn, barn)

fun ikkeSamsvarMellomPdlOgGrunnlagBarn(
    barn: List<Foedselsnummer>
) = samsvarBarn(barn, tomFoedselsnummer())

fun samsvarMellomPdlOgGrunnlagAnsvarligeForeldre(
    ansvarligeForeldre: List<Foedselsnummer>
) = samsvarAnsvarligeForeldre(ansvarligeForeldre, ansvarligeForeldre)

fun ikkeSamsvarMellomPdlOgGrunnlagAnsvarligeForeldre(
    ansvarligeForeldre: List<Foedselsnummer>
) = samsvarAnsvarligeForeldre(ansvarligeForeldre, tomFoedselsnummer())

fun grunnlagsendringshendelseMedSamsvar(
    id: UUID = UUID.randomUUID(),
    sakId: Long = 1,
    type: GrunnlagsendringsType = GrunnlagsendringsType.DOEDSFALL,
    opprettet: LocalDateTime = LocalDateTime.now(),
    fnr: String,
    status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    behandlingId: UUID? = null,
    hendelseGjelderRolle: Saksrolle = Saksrolle.SOEKER,
    samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag?
) = Grunnlagsendringshendelse(
    id = id,
    sakId = sakId,
    type = type,
    opprettet = opprettet,
    status = status,
    behandlingId = behandlingId,
    hendelseGjelderRolle = hendelseGjelderRolle,
    samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag,
    gjelderPerson = fnr
)

fun grunnlagsendringshendelseUtenSamsvar(
    id: UUID = UUID.randomUUID(),
    sakId: Long = 1,
    type: GrunnlagsendringsType = GrunnlagsendringsType.DOEDSFALL,
    opprettet: LocalDateTime = LocalDateTime.now(),
    fnr: String?,
    status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    behandlingId: UUID? = null,
    hendelseGjelderRolle: Saksrolle = Saksrolle.SOEKER,
    samsvarMellomPdlOgGrunnlag: SamsvarMellomPdlOgGrunnlag?
) = Grunnlagsendringshendelse(
    id = id,
    sakId = sakId,
    type = type,
    opprettet = opprettet,
    status = status,
    behandlingId = behandlingId,
    hendelseGjelderRolle = hendelseGjelderRolle,
    samsvarMellomPdlOgGrunnlag = samsvarMellomPdlOgGrunnlag,
    gjelderPerson = fnr
)

fun grunnlagsinformasjonDoedshendelse(
    avdoedFnr: String = "12345678911",
    doedsdato: LocalDate = LocalDate.of(2022, 1, 1),
    endringstype: Endringstype = Endringstype.OPPRETTET
) = Doedshendelse(avdoedFnr = avdoedFnr, doedsdato = doedsdato, endringstype = endringstype)

fun grunnlagsinformasjonUtflyttingshendelse(
    fnr: String = "12345678911",
    tilflyttingsLand: String = "Sverige",
    utflyttingsdato: LocalDate = LocalDate.of(2022, 8, 8)
) = UtflyttingsHendelse(
    fnr = fnr,
    tilflyttingsLand = tilflyttingsLand,
    tilflyttingsstedIUtlandet = null,
    utflyttingsdato = utflyttingsdato,
    endringstype = Endringstype.OPPRETTET
)

fun grunnlagsinformasjonForelderBarnRelasjonHendelse(
    fnr: String = "12345678911",
    relatertPersonsIdent: String = "98765432198",
    relatertPersonsRolle: String = "MOR",
    minRolleForPerson: String = "BARN"
) = ForelderBarnRelasjonHendelse(
    fnr = fnr,
    relatertPersonsIdent = relatertPersonsIdent,
    relatertPersonsRolle = relatertPersonsRolle,
    minRolleForPerson = minRolleForPerson,
    relatertPersonUtenFolkeregisteridentifikator = null,
    endringstype = Endringstype.OPPRETTET
)

fun kommerBarnetTilgode(
    svar: JaNeiVetIkke = JaNeiVetIkke.JA,
    begrunnelse: String = "En begrunnelse",
    kilde: Grunnlagsopplysning.Saksbehandler = Grunnlagsopplysning.Saksbehandler("S01", Instant.now())
) = KommerBarnetTilgode(svar, begrunnelse, kilde)

val TRIVIELL_MIDTPUNKT = Foedselsnummer.of("19040550081")
val STOR_SNERK = Foedselsnummer.of("11057523044")
fun mockPerson(
    utland: Utland? = null,
    familieRelasjon: FamilieRelasjon? = null,
    vergemaal: List<VergemaalEllerFremtidsfullmakt>? = null
) = PersonDTO(
    fornavn = OpplysningDTO(verdi = "Ola", opplysningsid = null),
    etternavn = OpplysningDTO(verdi = "Nordmann", opplysningsid = null),
    foedselsnummer = OpplysningDTO(TRIVIELL_MIDTPUNKT, null),
    foedselsdato = OpplysningDTO(LocalDate.now().minusYears(20), UUID.randomUUID().toString()),
    foedselsaar = OpplysningDTO(verdi = 2000, opplysningsid = null),
    foedeland = OpplysningDTO("Norge", UUID.randomUUID().toString()),
    doedsdato = null,
    adressebeskyttelse = null,
    bostedsadresse = listOf(
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
                gyldigFraOgMed = LocalDateTime.now().minusYears(1),
                gyldigTilOgMed = null
            ),
            UUID.randomUUID().toString()
        )
    ),
    deltBostedsadresse = listOf(),
    kontaktadresse = listOf(),
    oppholdsadresse = listOf(),
    sivilstatus = null,
    statsborgerskap = OpplysningDTO("Norsk", UUID.randomUUID().toString()),
    utland = utland?.let { OpplysningDTO(utland, UUID.randomUUID().toString()) },
    familieRelasjon = familieRelasjon?.let { OpplysningDTO(it, UUID.randomUUID().toString()) },
    avdoedesBarn = null,
    vergemaalEllerFremtidsfullmakt = vergemaal?.map { OpplysningDTO(it, UUID.randomUUID().toString()) }
)