export enum KATEGORI {
  FORELDRELOS = 'FORELDRELØS',
}

export function getValueOfKey(key: KATEGORI): string {
  return KATEGORI[key].valueOf()
}
