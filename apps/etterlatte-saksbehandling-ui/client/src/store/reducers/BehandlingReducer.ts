import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: string
  sak: number
  status: IBehandlingStatus
  saksbehandler: //for test
  | {
        ident: string
        navn: string
      }
    | undefined
  attestant: //for test
  | {
        ident: string
        navn: string
      }
    | undefined
  vilkårsprøving: IVilkaarResultat
  gyldighetsprøving: IGyldighetResultat
  kommerSoekerTilgode: IKommerSoekerTilgode
  beregning?: IBeregning
  fastsatt: boolean
  soeknadMottattDato: string
  virkningstidspunkt: string
}

//todo: synk med backend OG oppgavebenk her, og finn ut hva vi skal vise i frontend
export enum IBehandlingStatus {
  under_behandling = 'under_behandling',
  attestering = 'attestering',
  underkjent = 'underkjent',
  innvilget = 'innvilget',
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
  SOEKER_IKKE_ADRESSE_I_UTLANDET = 'SOEKER_IKKE_ADRESSE_I_UTLANDET',
  GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET = 'GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET',
  AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD = 'AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD',
  AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR = 'AVDOED_SAMMENHENGENDE_ADRESSE_NORGE_SISTE_FEM_AAR',
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

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  status: IBehandlingStatus.under_behandling, //test
  saksbehandler: {
    // for test
    ident: '',
    navn: 'Truls Veileder',
  },
  attestant: {
    // for test
    ident: '',
    navn: 'Linn Normann',
  },
  vilkårsprøving: { resultat: undefined, vilkaar: [], vurdertDato: '' },
  gyldighetsprøving: { resultat: undefined, vurderinger: [], vurdertDato: '' },
  kommerSoekerTilgode: {
    kommerSoekerTilgodeVurdering: { resultat: undefined, vilkaar: [], vurdertDato: '' },
    familieforhold: {
      avdoed: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        bostedadresser: [],
        doedsdato: '',
      },
      soeker: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        bostedadresser: [],
        soeknadAdresse: { adresseIUtlandet: '' },
        foedselsdato: '',
      },
      gjenlevendeForelder: {
        navn: '',
        fnr: '',
        rolle: PersonRolle.AVDOED,
        bostedadresser: [],
      },
    },
  },
  beregning: undefined,
  fastsatt: false,
  soeknadMottattDato: '',
  virkningstidspunkt: '',
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
