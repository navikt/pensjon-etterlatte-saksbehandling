import styled from 'styled-components'
import { formaterEnumTilLesbarString } from '../../utils/formattering'
import { IBehandlingsType } from '../../store/reducers/BehandlingReducer'

const colors = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: '#826ba1',
  [IBehandlingsType.REVURDERING]: '#c48bbf',
}

export const BehandlingsType: React.FC<{ type: IBehandlingsType }> = ({ type }) => {
  return <BehandlingsTypeColorTag type={type}>{formaterEnumTilLesbarString(type)}</BehandlingsTypeColorTag>
}

export const BehandlingsTypeColorTag = styled.div<{ type: IBehandlingsType }>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.2em 1em;
  color: #fff;
  border-radius: 15px;
  font-size: 0.8em;
  width: fit-content;
  float: right;
`
