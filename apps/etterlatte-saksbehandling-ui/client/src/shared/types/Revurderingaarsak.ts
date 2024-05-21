//Denne må matche det som er definert i backend Revurderingaarsak.kt

import { SakType } from '~shared/types/sak'

export enum Revurderingaarsak {
  REGULERING = 'REGULERING',
  ANSVARLIGE_FORELDRE = 'ANSVARLIGE_FORELDRE',
  SOESKENJUSTERING = 'SOESKENJUSTERING',
  UTLAND = 'UTLAND',
  EKSPORT = 'EKSPORT',
  IMPORT = 'IMPORT',
  BARN = 'BARN',
  DOEDSFALL = 'DOEDSFALL',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT = 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT',
  INNTEKTSENDRING = 'INNTEKTSENDRING',
  OMGJOERING_AV_FARSKAP = 'OMGJOERING_AV_FARSKAP',
  ADOPSJON = 'ADOPSJON',
  SIVILSTAND = 'SIVILSTAND',
  NY_SOEKNAD = 'NY_SOEKNAD',
  FENGSELSOPPHOLD = 'FENGSELSOPPHOLD',
  ANNEN = 'ANNEN',
  ANNEN_UTEN_BREV = 'ANNEN_UTEN_BREV',
  YRKESSKADE = 'YRKESSKADE',
  UT_AV_FENGSEL = 'UT_AV_FENGSEL',
  INSTITUSJONSOPPHOLD = 'INSTITUSJONSOPPHOLD',
  OMGJOERING_ETTER_KLAGE = 'OMGJOERING_ETTER_KLAGE',
  SLUTTBEHANDLING_UTLAND = 'SLUTTBEHANDLING_UTLAND',
  OPPHOER_UTEN_BREV = 'OPPHOER_UTEN_BREV',
  ALDERSOVERGANG = 'ALDERSOVERGANG',
  RETT_UTEN_TIDSBEGRENSNING = 'RETT_UTEN_TIDSBEGRENSNING',
  FORELDRELOES = 'FORELDRELOES',
  AVKORTING_MOT_UFOERETRYGD = 'AVKORTING_MOT_UFOERETRYGD',
  ETTEROPPGJOER = 'ETTEROPPGJOER',
  OMGJOERING_ETTER_ANKE = 'OMGJOERING_ETTER_ANKE',
  OMGJOERING_PAA_EGET_INITIATIV = 'OMGJOERING_PAA_EGET_INITIATIV',
  OMGJOERING_ETTER_KRAV_FRA_BRUKER = 'OMGJOERING_ETTER_KRAV_FRA_BRUKER',
  OPPHOER_3_AAR_ETTER_DOEDSFALL = 'OPPHOER_3_AAR_ETTER_DOEDSFALL',
  OPPHOER_AV_2_UTVIDEDE_AAR = 'OPPHOER_AV_2_UTVIDEDE_AAR',
  SANKSJON_PGA_MANGLENDE_OPPLYSNINGER = 'SANKSJON_PGA_MANGLENDE_OPPLYSNINGER',
  SOEKNAD_OM_GJENOPPTAK = 'SOEKNAD_OM_GJENOPPTAK',
  UTSENDELSE_AV_KRAVPAKKE = 'UTSENDELSE_AV_KRAVPAKKE',
  UTSENDELSE_AV_SED = 'UTSENDELSE_AV_SED',
}

export const tekstRevurderingsaarsak: Record<Revurderingaarsak, string> = {
  REGULERING: 'Regulering',
  ANSVARLIGE_FORELDRE: 'Ansvarlige foreldre',
  SOESKENJUSTERING: 'Søskenjustering',
  UTLAND: 'Utland',
  EKSPORT: 'Eksport / Utvandring',
  IMPORT: 'Import / Innvandring',
  BARN: 'Barn',
  DOEDSFALL: 'Dødsfall mottaker',
  VERGEMAAL_ELLER_FREMTIDSFULLMAKT: 'Verge / fullmakt',
  INNTEKTSENDRING: 'Endring av inntekt',
  OMGJOERING_AV_FARSKAP: 'Omgjøring av farskap',
  ADOPSJON: 'Adopsjon',
  SIVILSTAND: 'Endring av sivilstand',
  NY_SOEKNAD: 'Det har kommet inn en ny søknad',
  FENGSELSOPPHOLD: 'Fengselsopphold',
  ANNEN: 'Annen',
  ANNEN_UTEN_BREV: 'Annen (uten brev)',
  YRKESSKADE: 'Yrkesskade',
  UT_AV_FENGSEL: 'Ut av fengsel',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
  OMGJOERING_ETTER_KLAGE: 'Omgjøring etter klage',
  SLUTTBEHANDLING_UTLAND: 'Sluttbehandling utland',
  OPPHOER_UTEN_BREV: 'Opphør (uten brev)',
  ALDERSOVERGANG: 'Aldersovergang',
  RETT_UTEN_TIDSBEGRENSNING: 'Stønad uten tidsbegrensning',
  FORELDRELOES: 'Fra en forelder død til foreldreløs',
  AVKORTING_MOT_UFOERETRYGD: 'Avkorting mot uføretrygd',
  ETTEROPPGJOER: 'Etteroppgjør',
  OMGJOERING_ETTER_ANKE: 'Omgjøring etter anke',
  OMGJOERING_PAA_EGET_INITIATIV: 'Omgjøring på eget intitiativ',
  OMGJOERING_ETTER_KRAV_FRA_BRUKER: 'Omgjøring etter krav fra bruker',
  OPPHOER_3_AAR_ETTER_DOEDSFALL: 'Opphør 3 år etter dødsfall',
  OPPHOER_AV_2_UTVIDEDE_AAR: 'Opphør av 2 utvidede år',
  SANKSJON_PGA_MANGLENDE_OPPLYSNINGER: 'Sanksjon pga manglende opplysninger',
  SOEKNAD_OM_GJENOPPTAK: 'Søknad om gjenopptak',
  UTSENDELSE_AV_KRAVPAKKE: 'Utsendelse av kravpakke',
  UTSENDELSE_AV_SED: 'Utsendelse av SED',
} as const

export type RevurderingsaarsakerBySakstype = {
  [key in SakType]: Array<Revurderingaarsak>
}

export class RevurderingsaarsakerDefault implements RevurderingsaarsakerBySakstype {
  [SakType.BARNEPENSJON]: Array<Revurderingaarsak> = [];
  [SakType.OMSTILLINGSSTOENAD]: Array<Revurderingaarsak> = []
}
