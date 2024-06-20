export enum KATEGORI {
  FORELDRELOES = 'FORELDRELØS',
}

export function getValueOfKey(key: KATEGORI): string {
  return KATEGORI[key].valueOf()
}
