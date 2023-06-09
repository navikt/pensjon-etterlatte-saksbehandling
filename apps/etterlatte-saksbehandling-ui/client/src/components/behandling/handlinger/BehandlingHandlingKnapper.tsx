import AvbrytBehandling from './AvbrytBehandling'
import styled from 'styled-components'
import { Tilbake } from './tilbake'
import { ReactNode } from 'react'

export const BehandlingHandlingKnapper = ({ children }: { children: ReactNode }) => {
  return (
    <KnapperWrapper>
      <div>
        <Tilbake />
        {children}
      </div>
      <AvbrytBehandling />
    </KnapperWrapper>
  )
}

export const KnapperWrapper = styled.div`
  margin: 3em 0em 2em 0em;
  text-align: center;

  .button {
    padding-left: 2em;
    padding-right: 2em;
    min-width: 200px;
    margin: 0em 1em 0em 1em;
  }
  .textButton {
    margin-top: 1em;
    text-decoration: none;
    cursor: pointer;
    font-weight: bold;
    &:hover {
      text-decoration: underline;
    }
  }
`
