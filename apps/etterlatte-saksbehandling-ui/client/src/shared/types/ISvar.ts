export enum ISvar {
  JA = 'JA',
  NEI = 'NEI',
  IKKE_VURDERT = 'IKKE_VURDERT',
}

export enum JaNei {
  JA = 'JA',
  NEI = 'NEI',
}

export const JaNeiRec: Record<JaNei, string> = {
  [JaNei.JA]: 'Ja',
  [JaNei.NEI]: 'Nei',
} as const
