import { BehandlingTypeFilter, SoeknadTypeFilter } from '~components/oppgavebenken/typer/oppgavebenken'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import styled from 'styled-components'
import { INasjonalitetsType } from '~components/behandling/fargetags/nasjonalitetsType'

enum Variants {
    NEUTRAL= 'neutral',
    INFO_FILLED= 'info-filled',
    ALT1 = 'alt1',
    ALT1_FILLED= 'alt1-filled',
    ALT2= 'alt2',
    ALT2_FILLED= 'alt2-filled',
    ALT3= 'alt3',
    ALT3_FILLED= 'alt3-filled',
}

export const tagColors = {
  [BehandlingTypeFilter.VELG]: Variants.NEUTRAL,
  [BehandlingTypeFilter.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [BehandlingTypeFilter.REVURDERING]: Variants.ALT3_FILLED,
  [SoeknadTypeFilter.VELG]: Variants.NEUTRAL,
  [SoeknadTypeFilter.GJENLEVENDEPENSJON]: Variants.INFO_FILLED,
  [SoeknadTypeFilter.BARNEPENSJON]: Variants.ALT2_FILLED,
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: Variants.ALT3_FILLED,
  [IBehandlingsType.REVURDERING]: Variants.ALT3_FILLED,
  [IBehandlingsType.MANUELT_OPPHOER]: Variants.ALT3_FILLED,
  [ISaksType.BARNEPENSJON]: Variants.ALT2_FILLED,
  [ISaksType.GJENLEVENDEPENSJON]: Variants.INFO_FILLED,
  [INasjonalitetsType.NASJONAL]: Variants.ALT2,
  [INasjonalitetsType.UTLAND]: Variants.ALT3,
}

export const TagList = styled.ul`
  display: flex;
  list-style-type: none;
  gap: 0.5em;
`

