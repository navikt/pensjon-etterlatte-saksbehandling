import styled from 'styled-components'

export const FormWrapper = styled.div<{ column?: boolean }>`
  display: flex;
  flex-direction: ${(props) => (!!props.column ? 'column' : 'row')};
  gap: 2rem;
`
