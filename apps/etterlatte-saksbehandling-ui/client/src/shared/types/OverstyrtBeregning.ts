export enum KATEGORI {
  UKJENT_AVDOED = 'Ukjent avdød',
  AVKORTING_UFOERETRYGD = 'Avkorting uføretrygd',
  FENGSELSOPPHOLD = 'Fengselsopphold',
}

export function getValueOfKey(key: KATEGORI): string {
  return KATEGORI[key as unknown as keyof typeof KATEGORI] || key
}
