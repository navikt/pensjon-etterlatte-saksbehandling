import { OppgaveTypeFilter, SoeknadTypeFilter } from '~components/oppgavebenken/typer/oppgavebenken'
import { IBehandlingsType, IUtenlandstilsnittType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import styled from 'styled-components'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'

enum Variants {
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

export const tagColors: Record<
  OppgaveTypeFilter | SoeknadTypeFilter | SakType | INasjonalitetsType | IBehandlingsType | IUtenlandstilsnittType,
  Variants
> = {
  [OppgaveTypeFilter.ALLE]: Variants.NEUTRAL, // SoeknadTypeFilter.ALLE
  [OppgaveTypeFilter.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED, // IBehandlingsType.FØRSTEGANGSBEHANDLING
  [OppgaveTypeFilter.REVURDERING]: Variants.ALT3_FILLED, // IBehandlingsType.REVURDERING
  [OppgaveTypeFilter.ENDRING_PAA_SAK]: Variants.ALT3_FILLED,
  [SoeknadTypeFilter.BARNEPENSJON]: Variants.INFO, // SakType.BARNEPENSJON
  [SoeknadTypeFilter.OMSTILLINGSSTOENAD]: Variants.NEUTRAL, // SakType.OMSTILLINGSSTOENAD
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
