import AvbrytBehandling from './AvbrytBehandling'
import { Tilbake } from './tilbake'
import { ReactNode } from 'react'
import { FlexRow } from '~shared/styled'

export const BehandlingHandlingKnapper = ({ children }: { children: ReactNode }) => {
  return (
    <div>
      <FlexRow justify={'center'} $spacing>
        <Tilbake />
        {children}
      </FlexRow>
      <FlexRow justify={'center'}>
        <AvbrytBehandling />
      </FlexRow>
    </div>
  )
}
