import styled from 'styled-components'
import { BehandlingTypeFilter, SoeknadTypeFilter } from './typer/oppgavebenken'
import React from 'react'

export const FilterElement = styled.div`
  margin-bottom: 2rem;
  justify-items: flex-start;
  justify-items: flex-start;
  width: 200px;
  margin-right: 1rem;
`

export const FilterWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
`

const colors = {
  [BehandlingTypeFilter.VELG]: '#ffffff',
  [BehandlingTypeFilter.FØRSTEGANGSBEHANDLING]: '#826ba1',
  [BehandlingTypeFilter.REVURDERING]: '#c48bbf',
  [SoeknadTypeFilter.VELG]: '#ffffff',
  [SoeknadTypeFilter.GJENLEVENDEPENSJON]: '#337885',
  [SoeknadTypeFilter.BARNEPENSJON]: '#5da499',
  [BehandlingTypeFilter.HENDELSE]: 'rgba(193, 203, 51, 1)',
  [BehandlingTypeFilter.MANUELT_OPPHOER]: 'rgba(0, 91, 130, 1)',
}

export const ColorTag: React.FC<{ type: BehandlingTypeFilter | SoeknadTypeFilter; label: string }> = ({
  type,
  label,
}) => {
  return <ColorTagWrap type={type}>{label}</ColorTagWrap>
}

const ColorTagWrap = styled.div<{ type: BehandlingTypeFilter | SoeknadTypeFilter }>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.2em 1em;
  color: #fff;
  border-radius: 15px;
  font-size: 0.8em;
  width: fit-content;
`
