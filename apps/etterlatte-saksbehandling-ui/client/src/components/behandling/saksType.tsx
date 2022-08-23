import styled from 'styled-components'
import { formaterEnumTilLesbarString } from '../../utils/formattering'

export enum ISaksType {
  BARNEPENSJON = 'barnepensjon',
  GJENLEVENDEPENSJON = 'gjenlevendeperson',
}

const colors = {
  [ISaksType.BARNEPENSJON]: '#5da499',
  [ISaksType.GJENLEVENDEPENSJON]: '#337885',
}

export const SaksType: React.FC<{ type: ISaksType }> = ({ type }) => {
  return <SaksTypeColorTag type={type}>{formaterEnumTilLesbarString(type)}</SaksTypeColorTag>
}

export const SaksTypeColorTag = styled.div<{ type: ISaksType }>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.2em 1em;
  color: #fff;
  border-radius: 15px;
  font-size: 0.8em;
  width: fit-content;
  float: right;
  margin-left: 0.7em;
  margin-right: 0.5em;
`
