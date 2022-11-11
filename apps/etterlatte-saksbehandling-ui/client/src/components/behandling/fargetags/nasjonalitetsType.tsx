import { formaterEnumTilLesbarString } from '~utils/formattering'
import { ColorTagBehandling } from './styled'

export enum INasjonalitetsType {
  NASJONAL = 'nasjonal',
  UTLAND = 'utland',
}

const colors = {
  [INasjonalitetsType.NASJONAL]: '#6da16b',
  [INasjonalitetsType.UTLAND]: '#c4c38b',
}

export const NasjonalitetsType: React.FC<{ type: INasjonalitetsType }> = ({ type }) => {
  return <ColorTagBehandling color={getColor(type)}>{formaterEnumTilLesbarString(type)}</ColorTagBehandling>
}

function getColor(type: INasjonalitetsType) {
  return colors[type]
}
