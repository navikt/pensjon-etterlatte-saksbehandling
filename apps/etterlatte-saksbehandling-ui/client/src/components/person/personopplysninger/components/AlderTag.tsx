import React, { ReactNode } from 'react'
import styled from 'styled-components'
import { intervalToDuration } from 'date-fns'
import { Tag } from '@navikt/ds-react'

export const AlderTag = ({ foedselsdato }: { foedselsdato: Date }): ReactNode => {
  function utregnAlder(foedselsdato: Date): number {
    return intervalToDuration({
      start: foedselsdato,
      end: new Date(),
    }).years!
  }

  return (
    <AlderWrapper>
      <Tag data-color="info" variant="outline" size="small">
        {utregnAlder(foedselsdato)} år
      </Tag>
    </AlderWrapper>
  )
}

const AlderWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`
