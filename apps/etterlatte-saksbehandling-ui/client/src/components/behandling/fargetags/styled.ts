import styled from 'styled-components'

export const ColorTagBehandling = styled.div<{ color: string }>`
  background-color: ${(props) => props.color};
  padding: 0.2em 1em;
  color: #fff;
  border-radius: 15px;
  font-size: 0.8em;
  width: fit-content;
  float: right;
  margin-left: 0.7em;
`
