import { OppgaveTypeFilter, SoeknadTypeFilter } from '~components/oppgavebenken/typer/oppgavebenken'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { ISaksType } from '~components/behandling/fargetags/saksType'
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
  OppgaveTypeFilter | SoeknadTypeFilter | ISaksType | INasjonalitetsType | IBehandlingsType,
  Variants
> = {
  [OppgaveTypeFilter.ALLE]: Variants.NEUTRAL,
  [OppgaveTypeFilter.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [OppgaveTypeFilter.REVURDERING]: Variants.ALT3_FILLED,
  [OppgaveTypeFilter.ENDRING_PAA_SAK]: Variants.ALT3_FILLED,
  [SoeknadTypeFilter.ALLE]: Variants.NEUTRAL,
  [SoeknadTypeFilter.BARNEPENSJON]: Variants.INFO,
  [SoeknadTypeFilter.OMSTILLINGSSTOENAD]: Variants.NEUTRAL,
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [IBehandlingsType.REVURDERING]: Variants.ALT3_FILLED,
  [IBehandlingsType.MANUELT_OPPHOER]: Variants.ALT3_FILLED,
  [ISaksType.BARNEPENSJON]: Variants.INFO,
  [ISaksType.OMSTILLINGSSTOENAD]: Variants.NEUTRAL,
  [INasjonalitetsType.NASJONAL]: Variants.INFO_FILLED,
  [INasjonalitetsType.UTLAND]: Variants.ALT3,
}

export const TagList = styled.ul`
  display: flex;
  list-style-type: none;
  gap: 0.5em;
  margin-top: 0.5em;
`
