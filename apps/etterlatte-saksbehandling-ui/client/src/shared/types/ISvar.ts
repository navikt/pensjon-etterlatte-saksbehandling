import { hasValue } from '~components/behandling/felles/utils'

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

export const mapBooleanToJaNei = (value: boolean | null | undefined): JaNei | undefined => {
  if (!hasValue(value)) {
    return undefined
  } else if (value) {
    return JaNei.JA
  } else {
    return JaNei.NEI
  }
}
