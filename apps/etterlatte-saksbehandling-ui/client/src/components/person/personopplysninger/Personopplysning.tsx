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
    <PersonopplysningBox padding="3">
      <PersonopplysningHeader>
        {icon}
        <Heading size="small">{heading}</Heading>
      </PersonopplysningHeader>
      <PersonopplysningContent>{children}</PersonopplysningContent>
    </PersonopplysningBox>
  )
}

const PersonopplysningBox = styled(Box)`
  background-color: var(--a-gray-50);
`

const PersonopplysningHeader = styled.div`
  display: flex;
  align-items: end;
  gap: 1rem;

  svg {
    max-width: 2rem;
    max-height: 2rem;
    width: 100%;
    height: 100%;
  }
`

const PersonopplysningContent = styled.div`
  padding-top: 1rem;
  padding-left: 3rem;
`
