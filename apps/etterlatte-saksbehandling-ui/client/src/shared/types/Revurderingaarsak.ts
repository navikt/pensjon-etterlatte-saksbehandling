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
  ANNEN: 'Annen (med brev)',
  ANNEN_UTEN_BREV: 'Annen (uten brev)',
  YRKESSKADE: 'Yrkesskade',
  UT_AV_FENGSEL: 'Ut av fengsel',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
  OMGJOERING_ETTER_KLAGE: 'Omgjøring etter klage',
  SLUTTBEHANDLING_UTLAND: 'Sluttbehandling utland',
  OPPHOER_UTEN_BREV: 'Opphør (uten brev)',
  ALDERSOVERGANG: 'Aldersovergang',
  RETT_UTEN_TIDSBEGRENSNING: 'Stønad uten tidsbegrensning',
} as const

export type RevurderingsaarsakerBySakstype = {
  [key in SakType]: Array<Revurderingaarsak>
}

export class RevurderingsaarsakerDefault implements RevurderingsaarsakerBySakstype {
  [SakType.BARNEPENSJON]: Array<Revurderingaarsak> = [];
  [SakType.OMSTILLINGSSTOENAD]: Array<Revurderingaarsak> = []
}
