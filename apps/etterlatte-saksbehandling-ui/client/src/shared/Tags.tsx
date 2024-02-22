import { IBehandlingsType, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import styled from 'styled-components'

export enum Variants {
  NEUTRAL = 'neutral',
  INFO = 'info',
  INFO_FILLED = 'info-filled',
  ALT1 = 'alt1',
  ALT1_FILLED = 'alt1-filled',
  ALT2 = 'alt2',
  ALT2_FILLED = 'alt2-filled',
  ALT2_MODERATE = 'alt2-moderate',
  ALT3 = 'alt3',
  ALT3_FILLED = 'alt3-filled',
}

export const tagColors: Record<SakType | IBehandlingsType | UtlandstilknytningType, Variants> = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [IBehandlingsType.REVURDERING]: Variants.ALT3_FILLED,
  [SakType.BARNEPENSJON]: Variants.INFO,
  [SakType.OMSTILLINGSSTOENAD]: Variants.NEUTRAL,
  [IBehandlingsType.MANUELT_OPPHOER]: Variants.ALT3_FILLED,
  [UtlandstilknytningType.NASJONAL]: Variants.INFO_FILLED,
  [UtlandstilknytningType.UTLANDSTILSNITT]: Variants.ALT3,
  [UtlandstilknytningType.BOSATT_UTLAND]: Variants.ALT2,
}

export const TagList = styled.ul`
  display: flex;
  list-style-type: none;
  gap: 0.5em;
  margin-top: 0.5em;
`
