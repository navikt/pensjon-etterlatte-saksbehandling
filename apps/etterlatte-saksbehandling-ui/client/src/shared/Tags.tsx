import { IBehandlingsType, IUtenlandstilsnittType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import styled from 'styled-components'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'

export enum Variants {
  NEUTRAL = 'neutral',
  INFO = 'info',
  INFO_FILLED = 'info-filled',
  ALT1 = 'alt1',
  ALT1_FILLED = 'alt1-filled',
  ALT2 = 'alt2',
  ALT2_FILLED = 'alt2-filled',
  ALT3 = 'alt3',
  ALT3_FILLED = 'alt3-filled',
}

export const tagColors: Record<SakType | INasjonalitetsType | IBehandlingsType | IUtenlandstilsnittType, Variants> = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [IBehandlingsType.REVURDERING]: Variants.ALT3_FILLED,
  [SakType.BARNEPENSJON]: Variants.INFO,
  [SakType.OMSTILLINGSSTOENAD]: Variants.NEUTRAL,
  [IBehandlingsType.MANUELT_OPPHOER]: Variants.ALT3_FILLED,
  [INasjonalitetsType.NASJONAL]: Variants.INFO_FILLED,
  [INasjonalitetsType.UTLAND]: Variants.ALT3,
  [IUtenlandstilsnittType.NASJONAL]: Variants.INFO_FILLED,
  [IUtenlandstilsnittType.UTLANDSTILSNITT]: Variants.ALT3,
  [IUtenlandstilsnittType.BOSATT_UTLAND]: Variants.ALT2,
}

export const TagList = styled.ul`
  display: flex;
  list-style-type: none;
  gap: 0.5em;
  margin-top: 0.5em;
`
