import React, { ReactNode } from 'react'
import { Box, Heading } from '@navikt/ds-react'
import { HouseIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'

interface Props {
  heading: string
  children: ReactNode | Array<ReactNode>
}

export const Personopplysning = ({ heading, children }: Props): ReactNode => {
  return (
    <Box background="bg-subtle" padding="3">
      <PersonopplysningHeader>
        <HouseIcon height="2.5rem" width="2.5rem" />
        <Heading size="medium">{heading}</Heading>
      </PersonopplysningHeader>
      {children}
    </Box>
  )
}

const PersonopplysningHeader = styled.div`
  display: flex;
  align-items: end;
  gap: 1rem;
`
