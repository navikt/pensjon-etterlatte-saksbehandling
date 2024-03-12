import React, { ReactNode } from 'react'
import { Box, Heading } from '@navikt/ds-react'
import styled from 'styled-components'

interface Props {
  heading: string
  icon: ReactNode
  children: ReactNode | Array<ReactNode>
}

export const Personopplysning = ({ heading, children, icon }: Props): ReactNode => {
  return (
    <Box background="bg-subtle" padding="3">
      <PersonopplysningHeader>
        {icon}
        <Heading size="small">{heading}</Heading>
      </PersonopplysningHeader>
      <PersonopplysningContent>{children}</PersonopplysningContent>
    </Box>
  )
}

const PersonopplysningHeader = styled.div`
  display: flex;
  align-items: end;
  gap: 1rem;
`

const PersonopplysningContent = styled.div`
  padding-top: 1rem;
  padding-left: 3rem;
`
