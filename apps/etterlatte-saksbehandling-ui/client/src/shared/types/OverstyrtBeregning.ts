export enum overstyrtBeregningKategori {
  UKJENT_AVDOED = 'Ukjent avdød',
  AVKORTING_UFOERETRYGD = 'Avkorting uføretrygd',
  FENGSELSOPPHOLD = 'Fengselsopphold',
}

export function getValueOfKey(key: overstyrtBeregningKategori): string {
  return overstyrtBeregningKategori[key as unknown as keyof typeof overstyrtBeregningKategori] || key
}
