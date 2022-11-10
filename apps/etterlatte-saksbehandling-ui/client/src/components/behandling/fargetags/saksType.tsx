import { formaterEnumTilLesbarString } from '~utils/formattering'
import { ColorTagBehandling } from './styled'

export enum ISaksType {
  BARNEPENSJON = 'barnepensjon',
  GJENLEVENDEPENSJON = 'gjenlevendeperson',
}

const colors = {
  [ISaksType.BARNEPENSJON]: '#5da499',
  [ISaksType.GJENLEVENDEPENSJON]: '#337885',
}

export const SaksType: React.FC<{ type: ISaksType }> = ({ type }) => {
  return <ColorTagBehandling color={getColor(type)}>{formaterEnumTilLesbarString(type)}</ColorTagBehandling>
}

function getColor(type: ISaksType) {
  return colors[type]
}
