import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: string
  sak: number
  gyldighetsprøving?: IGyldighetResultat
  kommerSoekerTilgode?: IKommerSoekerTilgode
  vilkårsprøving?: IVilkaarResultat
  beregning?: IBeregning
  avkortning?: any //todo legg med type når denne er klar
  saksbehandlerId?: string
  fastsatt: boolean
  datoFattet?: string //kommer som Instant fra backend
  datoAttestert?: string //kommer som Instant fra backend
  attestant?: string
  soeknadMottattDato: string
  virkningstidspunkt: string
  status: IBehandlingStatus
  hendelser: IHendelse[]
  familieforhold?: IFamilieforhold
  behandlingType: IBehandlingsType
  søker?: IPdlPerson
}

export interface IHendelse {
  id: number
  hendelse: IHendelseType
  opprettet: string
  ident?: string
  identType?: null
  kommentar: string
  valgtBegrunnelse: string
}

export enum IHendelseType {
  BEHANDLING_OPPRETTET = 'BEHANDLING:OPPRETTET',
  VEDTAK_VILKAARSVURDERT = 'VEDTAK:VILKAARSVURDERT',
  VEDTAK_BEREGNET = 'VEDTAK:BEREGNET',
  VEDTAK_AVKORTET = 'VEDTAK:AVKORTET',
  VEDTAK_FATTET = 'VEDTAK:FATTET',
  VEDTAK_UNDERKJENT = 'VEDTAK:UNDERKJENT',
  VEDTAK_ATTESTERT = 'VEDTAK:ATTESTERT',
}

export enum IBehandlingsType {
  FØRSTEGANGSBEHANDLING = 'FØRSTEGANGSBEHANDLING',
  REVURDERING = 'REVURDERING',
  MANUELT_OPPHOER = 'MANUELT_OPPHOER',
}

export enum IBehandlingStatus {
  OPPRETTET = 'OPPRETTET',
  GYLDIG_SOEKNAD = 'GYLDIG_SOEKNAD',
  IKKE_GYLDIG_SOEKNAD = 'IKKE_GYLDIG_SOEKNAD',
  UNDER_BEHANDLING = 'UNDER_BEHANDLING',
  FATTET_VEDTAK = 'FATTET_VEDTAK',
  RETURNERT = 'RETURNERT',
  ATTESTERT = 'ATTESTERT',
  IVERKSATT = 'IVERKSATT',
  AVBRUTT = 'AVBRUTT',
}

export interface IBeregning {
  id: string
  type: string
  endringkode: string
  resultat: string
  beregningsperioder: IBeregningsperiode[]
  beregnetDato: string
  grunnlagVerson: number
}

export interface IBeregningsperiode {
  delytelseId: string
  type: string
  datoFOM: string
  datoTOM: string
  grunnbelopMnd: number
  grunnbelop: number
  soeskenFlokk: IPerson[] | null
  utbetaltBeloep: number
}

export interface IKommerSoekerTilgode {
  kommerSoekerTilgodeVurdering: IVilkaarResultat
  familieforhold: IFamiliemedlemmer
}

export interface IFamiliemedlemmer {
  avdoed: IPersoninfoAvdoed
  soeker: IPersoninfoSoeker
  gjenlevendeForelder: IPersoninfoGjenlevendeForelder
}

export interface IPersoninfoAvdoed {
  navn: string
  fnr: string
  rolle: PersonRolle
  bostedadresser: IAdresse[]
  doedsdato: string
}

export interface IPersoninfoSoeker {
  navn: string
  fnr: string
  rolle: PersonRolle
  bostedadresser: IAdresse[]
  soeknadAdresse: IUtlandsadresseSoeknad
  foedselsdato: string
}

export interface IAdresser {
  bostedadresse: IAdresse[]
  oppholdadresse: IAdresse[]
  kontaktadresse: IAdresse[]
}

export interface IAdresse {
  adresseLinje1: string
  adresseLinje2?: string
  adresseLinje3?: string
  aktiv: boolean
  coAdresseNavn?: string
  gyldigFraOgMed: string
  gyldigTilOgMed?: string
  kilde: string
  land?: string
  postnr: string
  poststed?: string
  type: string // adresseType
}

export interface IUtlandsadresseSoeknad {
  adresseIUtlandet: string
  land?: string
  adresse?: string
}

export interface IPersoninfoGjenlevendeForelder {
  navn: string
  fnr: string
  rolle: PersonRolle
  bostedadresser: IAdresse[]
}

export interface IBehandlingsopplysning {
  id: string
  kilde: {
    type: KildeType
  }
  opplysningsType: OpplysningsType
  opplysningType?: any
  meta: any
  opplysning: any
  attestering: any
}

export enum OpplysningsType {
  soeker_pdl = 'SOEKER_PDL_V1',
  soeker_soeknad = 'SOEKER_SOEKNAD_V1',
  gjenlevende_forelder_pdl = 'GJENLEVENDE_FORELDER_PDL_V1',
  gjenlevende_forelder_soeknad = 'GJENLEVENDE_FORELDER_SOEKNAD_V1',
  avdoed_forelder_pdl = 'AVDOED_PDL_V1',
  avdoed_forelder_soeknad = 'AVDOED_SOEKNAD_V1',

  soeknad_mottatt = 'SOEKNAD_MOTTATT_DATO',
  innsender = 'INNSENDER_SOEKNAD_V1',
}

export enum KildeType {
  saksbehandler = 'saksbehandler',
  privatperson = 'privatperson',
  a_ordningen = 'a-ordningen',
  aa_registeret = 'aa-registeret',
  vilkaarskomponenten = 'vilkaarskomponenten',
  pdl = 'pdl',
}

export enum VurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export interface IGyldighetResultat {
  resultat: VurderingsResultat | undefined
  vurderinger: IGyldighetproving[]
  vurdertDato: string
}

export interface IGyldighetproving {
  navn: GyldigFramsattType
  resultat: VurderingsResultat
  basertPaaOpplysninger: any
}

export enum GyldigFramsattType {
  INNSENDER_ER_FORELDER = 'INNSENDER_ER_FORELDER',
  HAR_FORELDREANSVAR_FOR_BARNET = 'HAR_FORELDREANSVAR_FOR_BARNET',
  INGEN_ANNEN_VERGE_ENN_FORELDER = 'INGEN_ANNEN_VERGE_ENN_FORELDER',
}

export interface IVilkaarResultat {
  resultat: VurderingsResultat | undefined
  vilkaar: IVilkaarsproving[]
  vurdertDato: string
}

export interface IVilkaarsproving {
  navn: VilkaarsType
  resultat: VurderingsResultat
  utfall: Utfall | undefined
  kriterier: IKriterie[]
  vurdertDato: string
}

export enum Utfall {
  OPPFYLT = 'OPPFYLT',
  BEHANDLE_I_PSYS = 'BEHANDLE_I_PSYS',
  TRENGER_AVKLARING = 'TRENGER_AVKLARING',
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
  AVDOEDES_FORUTGAAENDE_MEDLEMSKAP = 'AVDOEDES_FORUTGAAENDE_MEDLEMSKAP',
  BARNETS_MEDLEMSKAP = 'BARNETS_MEDLEMSKAP',
  SAMME_ADRESSE = 'GJENLEVENDE_OG_BARN_SAMME_BOSTEDADRESSE',
  BARN_BOR_PAA_AVDOEDES_ADRESSE = 'BARN_BOR_PAA_AVDOEDES_ADRESSE',
  BARN_INGEN_OPPGITT_UTLANDSADRESSE = 'BARN_INGEN_OPPGITT_UTLANDSADRESSE',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
  FORMAAL_FOR_YTELSEN = 'FORMAAL_FOR_YTELSEN',
  SAKEN_KAN_BEHANDLES_I_SYSTEMET = 'SAKEN_KAN_BEHANDLES_I_SYSTEMET',
}

export interface IKriterie {
  navn: Kriterietype
  resultat: VurderingsResultat
  basertPaaOpplysninger: IKriterieOpplysning[]
}

export enum Kriterietype {
  AVDOED_ER_FORELDER = 'AVDOED_ER_FORELDER',
  DOEDSFALL_ER_REGISTRERT_I_PDL = 'DOEDSFALL_ER_REGISTRERT_I_PDL',
  SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO = 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO',
  SOEKER_ER_I_LIVE = 'SOEKER_ER_I_LIVE',
  SOEKER_IKKE_ADRESSE_I_UTLANDET = 'SOEKER_IKKE_ADRESSE_I_UTLANDET',
  GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET = 'GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
  AVDOED_IKKE_REGISTRERT_NOE_I_MEDL = 'AVDOED_IKKE_REGISTRERT_NOE_I_MEDL',
  AVDOED_NORSK_STATSBORGER = 'AVDOED_NORSK_STATSBORGER',
  AVDOED_INGEN_INN_ELLER_UTVANDRING = 'AVDOED_INGEN_INN_ELLER_UTVANDRING',
  AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR = 'AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR',
  AVDOED_KUN_NORSKE_BOSTEDSADRESSER = 'AVDOED_KUN_NORSKE_BOSTEDSADRESSER',
  AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER = 'AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER',
  AVDOED_KUN_NORSKE_KONTAKTADRESSER = 'AVDOED_KUN_NORSKE_KONTAKTADRESSER',
  AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD = 'AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD',
  AVDOED_OPPFYLLER_MEDLEMSKAP = 'AVDOED_OPPFYLLER_MEDLEMSKAP',
}

export interface IKriterieOpplysning {
  kriterieOpplysningsType: KriterieOpplysningsType
  kilde: any
  opplysning: any
}

export enum KriterieOpplysningsType {
  ADRESSER = 'ADRESSER',
  ADRESSELISTE = 'ADRESSELISTE',
  DOEDSDATO = 'DOEDSDATO',
  FOEDSELSDATO = 'FOEDSELSDATO',
  FORELDRE = 'FORELDRE',

  SOEKER_UTENLANDSOPPHOLD = 'SOEKER_UTENLANDSOPPHOLD',
  ADRESSE_GAPS = 'ADRESSE_GAPS',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
  AVDOED_MEDLEMSKAP = 'AVDOED_MEDLEMSKAP',
  AVDOED_UTENLANDSOPPHOLD = 'AVDOED_UTENLANDSOPPHOLD',
  STATSBORGERSKAP = 'STATSBORGERSKAP',
  UTLAND = 'UTLAND',
}

export interface IPerson {
  type: PersonType
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: string
}

export interface IFamilieforhold {
  avdoede: Grunnlagsopplysning<IPdlPerson>
  gjenlevende: Grunnlagsopplysning<IPdlPerson>
}

export interface IFamilieRelasjon {
  ansvarligeForeldre?: string[]
  foreldre?: string[]
  barn?: string[]
}

export interface Grunnlagsopplysning<T> {
  id: string
  kilde: string
  opplysningsType: string
  opplysning: T
}

export interface IPdlPerson {
  fornavn: string
  etternavn: string
  foedselsnummer: string
  foedselsdato: Date
  doedsdato: Date
  bostedsadresse?: IAdresse[]
  deltBostedsadresse?: IAdresse[]
  kontaktadresse?: IAdresse[]
  oppholdsadresse?: IAdresse[]
  avdoedesBarn?: IPdlPerson[]
  familieRelasjon?: IFamilieRelasjon
  // ...
}

export enum PersonType {
  INNSENDER = 'INNSENDER',
  GJENLEVENDE = ' GJENLEVENDE',
  GJENLEVENDE_FORELDER = 'GJENLEVENDE_FORELDER',
  AVDOED = 'AVDOED',
  SAMBOER = 'SAMBOER',
  VERGE = 'VERGE',
  BARN = 'BARN',
  FORELDER = 'FORELDER',
}

export enum PersonRolle {
  BARN = 'BARN',
  AVDOED = 'AVDOED',
  GJENLEVENDE = 'GJENLEVENDE',
}

export enum ISvar {
  JA = 'JA',
  NEI = 'NEI',
  IKKE_VURDERT = 'IKKE_VURDERT',
}

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  status: IBehandlingStatus.UNDER_BEHANDLING, //test
  saksbehandlerId: '',
  attestant: '',
  vilkårsprøving: undefined,
  gyldighetsprøving: undefined,
  kommerSoekerTilgode: undefined,
  beregning: undefined,
  fastsatt: false,
  soeknadMottattDato: '',
  virkningstidspunkt: '',
  hendelser: [],
  familieforhold: undefined,
  behandlingType: IBehandlingsType.FØRSTEGANGSBEHANDLING,
  søker: undefined,
}

export const addBehandlingAction = (data: IDetaljertBehandling) => ({ type: 'add_behandling', data })
export const resetBehandlingAction = () => ({ type: 'reset_behandling' })

export const behandlingReducer = (state = detaljertBehandlingInitialState, action: IAction): any => {
  switch (action.type) {
    case 'add_behandling':
      return {
        ...state,
        ...action.data,
      }
    case 'reset_behandling':
      return detaljertBehandlingInitialState
    default:
      state
  }
}
