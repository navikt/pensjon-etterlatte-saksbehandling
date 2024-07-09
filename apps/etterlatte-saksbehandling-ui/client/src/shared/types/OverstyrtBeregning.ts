export enum OverstyrtBeregningKategori {
  UKJENT_AVDOED = 'Ukjent avdød',
  AVKORTING_UFOERETRYGD = 'Avkorting uføretrygd (BP)',
  FENGSELSOPPHOLD = 'Fengselsopphold',
}

export function getValueOfKey(key: OverstyrtBeregningKategori): string {
  return OverstyrtBeregningKategori[key as unknown as keyof typeof OverstyrtBeregningKategori] || key
}
