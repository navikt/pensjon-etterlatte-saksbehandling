import styled from 'styled-components'
import { OppgavetypeFilter, SoeknadstypeFilter } from './typer/oppgavebenken'

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
  [OppgavetypeFilter.VELG]: '#ffffff',
  [OppgavetypeFilter.FOERSTEGANGSBEHANDLING]: '#826ba1',
  [SoeknadstypeFilter.VELG]: '#ffffff',
  [SoeknadstypeFilter.GJENLEVENDEPENSJON]: '#337885',
  [SoeknadstypeFilter.BARNEPENSJON]: '#5da499',
}

export const ColorTag: React.FC<{ type: OppgavetypeFilter | SoeknadstypeFilter; label: string }> = ({
  type,
  label,
}) => {
  return <ColorTagWrap type={type}>{label}</ColorTagWrap>
}

const ColorTagWrap = styled.div<{ type: OppgavetypeFilter | SoeknadstypeFilter }>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.2em 1em;
  color: #fff;
  border-radius: 15px;
  font-size: 0.8em;
  width: fit-content;
`
