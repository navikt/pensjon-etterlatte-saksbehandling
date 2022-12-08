import { ColorTagBehandling } from './styled'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { formaterEnumTilLesbarString } from '~utils/formattering'

const colors = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: '#826ba1',
  [IBehandlingsType.REVURDERING]: '#c48bbf',
  [IBehandlingsType.MANUELT_OPPHOER]: '#c48bbf',
}

export const BehandlingsType: React.FC<{ type: IBehandlingsType }> = ({ type }) => {
  return <ColorTagBehandling color={getColor(type)}>{formaterEnumTilLesbarString(type)}</ColorTagBehandling>
}

function getColor(type: IBehandlingsType) {
  return colors[type]
}
