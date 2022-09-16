import { ColorTagBehandling } from './styled'
import { IBehandlingsType } from '../../../store/reducers/BehandlingReducer'
import { formaterEnumTilLesbarString } from '../../../utils/formattering'

const colors = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: '#826ba1',
  [IBehandlingsType.REVURDERING]: '#c48bbf',
}

export const BehandlingsType: React.FC<{ type: IBehandlingsType }> = ({ type }) => {
  return <ColorTagBehandling color={getColor(type)}>{formaterEnumTilLesbarString(type)}</ColorTagBehandling>
}

function getColor(type: IBehandlingsType) {
  return colors[type]
}
