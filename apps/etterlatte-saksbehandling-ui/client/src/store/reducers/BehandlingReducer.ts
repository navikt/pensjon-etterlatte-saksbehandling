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
  pdl = 'pdl',
  privatperson = 'privatperson',
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
  kriterier: IKriterie[]
  vurdertDato: string
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
  AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD = 'AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD',
  AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR = 'AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
}

export interface IKriterieOpplysning {
  kriterieOpplysningsType: KriterieOpplysningsType
  kilde: any
  opplysning: any
}

export enum KriterieOpplysningsType {
  ADRESSER = 'ADRESSER',
  DOEDSDATO = 'DOEDSDATO',
  FOEDSELSDATO = 'FOEDSELSDATO',
  FORELDRE = 'FORELDRE',
  AVDOED_UTENLANDSOPPHOLD = 'AVDOED_UTENLANDSOPPHOLD',
  SOEKER_UTENLANDSOPPHOLD = 'SOEKER_UTENLANDSOPPHOLD',
  ADRESSE_GAPS = 'ADRESSE_GAPS',
  SAKSBEHANDLER_RESULTAT = 'SAKSBEHANDLER_RESULTAT',
}

export interface IPerson {
  type: PersonType
  fornavn: string
  etternavn: string
  foedselsnummer: string
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
}

export const behandlingReducer = (state = detaljertBehandlingInitialState, action: IAction): any => {
  switch (action.type) {
    case 'add_behandling':
      return {
        ...state,
        ...action.data,
      }
    default:
      state
  }
}
