import { VurderingsResultat } from '~shared/types/VurderingsResultat'

export interface IKriterie {
  navn: Kriterietype
  resultat: VurderingsResultat
  basertPaaOpplysninger: IKriterieOpplysning[]
}

export enum Kriterietype {
  AVDOED_ER_FORELDER = 'AVDOED_ER_FORELDER',
  DOEDSFALL_ER_REGISTRERT_I_PDL = 'DOEDSFALL_ER_REGISTRERT_I_PDL',
  SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO = 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO', //BEHOLDE PGA gammel vilkårsvurdering i backend?
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
