import { IAction } from '../AppContext'

export interface IDetaljertBehandling {
  id: string
  sak: number
  grunnlag: IBehandlingsopplysning[]
  vilkårsprøving: IVilkaarsproving[]
  beregning: any
  fastsatt: boolean
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
  avdoed_doedsfall = 'AVDOED_DOEDSFALL_V1',
  avdoed_personinfo = 'AVDOED_PERSONINFO_V1',
  soeker_foedselsdato = 'SOEKER_FOEDSELSDATO_V1',
  soeker_personinfo = 'SOEKER_PERSONINFO_V1',
  soeker_relasjon_foreldre = 'SOEKER_RELASJON_FORELDRE_V1',
  soeker_relasjon_soeksken = 'SOEKER_RELASJON_SOESKEN_V1',
  soeker_bostedadresse = 'SOEKER_BOSTEDADRESSE_V1',
  avdoed_bostedadresse = 'AVDOED_BOSTEDADRESSE_V1',
  gjenlevende_forelder_bostedsadresse = 'GJENLEVENDE_FORELDER_BOSTEDADRESSE_V1',
  soeknad_mottatt = 'SOEKNAD_MOTTATT_DATO',
  omsorg = 'SOEKER_DAGLIG_OMSORG_V1',
  innsender = 'INNSENDER_PERSONINFO_V1',
  gjenlevende_forelder_personinfo = 'GJENLEVENDE_FORELDER_PERSONINFO_V1',
}

export enum KildeType {
  pdl = 'pdl',
  privatperson = 'privatperson',
}

export interface IVilkaarsproving {
  navn: VilkaarsType
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IKriterie[]
}

export enum VilkaarsType {
  SOEKER_ER_UNDER_20 = 'SOEKER_ER_UNDER_20',
  DOEDSFALL_ER_REGISTRERT = 'DOEDSFALL_ER_REGISTRERT',
  AVDOEDES_FORUTGAAENDE_MELDLEMSKAP = 'AVDOEDES_FORUTGAAENDE_MELDLEMSKAP',
  BARNETS_MEDLEMSKAP = 'BARNETS_MEDLEMSKAP',
}

export enum VilkaarVurderingsResultat {
  OPPFYLT = 'OPPFYLT',
  IKKE_OPPFYLT = 'IKKE_OPPFYLT',
  KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING = 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING',
}

export interface IKriterie {
  navn: Kriterietype
  resultat: VilkaarVurderingsResultat
  basertPaaOpplysninger: IVilkaaropplysing[]
}

export interface IVilkaaropplysing {
  opplysningsType: OpplysningsType
  kilde: string
  opplysing: any
}

export enum Kriterietype {
  AVDOED_ER_FORELDER = 'AVDOED_ER_FORELDER',
  DOEDSFALL_ER_REGISTRERT_I_PDL = 'DOEDSFALL_ER_REGISTRERT_I_PDL',
  SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO = 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO',
  SOEKER_IKKE_OPPGITT_ADRESSE_I_UTLANDET_I_SOEKNAD = 'SOEKER_IKKE_OPPGITT_ADRESSE_I_UTLANDET_I_SOEKNAD',
  SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET = 'SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET',
  SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET = 'SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET',
  SOEKER_IKKE_KONTAKTADRESSE_I_UTLANDET = 'SOEKER_IKKE_KONTAKTADRESSE_I_UTLANDET',
}

export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: '',
  sak: 0,
  grunnlag: [],
  vilkårsprøving: [],
  beregning: undefined,
  fastsatt: false,
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
