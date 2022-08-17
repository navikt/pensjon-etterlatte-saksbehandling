import styled from 'styled-components'

export enum ISaksType {
  BARNEPENSJON = 'barnepensjon', GJENLEVENDEPENSJON = 'gjenlevendeperson',
}

const colors = {
  [ISaksType.BARNEPENSJON]: '#CCE1FF', [ISaksType.GJENLEVENDEPENSJON]: '66A3C4'
}

export const SaksTypeSmall: React.FC<{type: ISaksType}> = ({type}) => {
  return <SaksTypeWrapSmall type={type}>{type}</SaksTypeWrapSmall>
}

export const SaksTypeWrapSmall = styled.div<{type: ISaksType}>`
  background-color: ${(props) => colors[props.type]};
  padding: 0.1em 0.5em;
  text-align: center;
  border-radius: 15px;
  font-size: 14px;
  text-transform: capitalize;
  float: right;
  margin-left: 0.7em;
  margin-right: 0.5em;  
`
