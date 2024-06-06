import React from 'react'
import { Label, Link } from '@navikt/ds-react'
import styled from 'styled-components'
import { ChevronRightIcon } from '@navikt/aksel-icons'

export const KLAGESTEG = ['formkrav', 'vurdering', 'brev', 'oppsummering'] as const

export type Klagesteg = (typeof KLAGESTEG)[number]

interface Props {
  path: Klagesteg
  description: string
  enabled: boolean
  erSisteRoute?: boolean
}

export function KlageNavLenke({ path, description, enabled, erSisteRoute }: Props) {
  return (
    <>
      {!enabled ? (
        <DisabledLabel>{description}</DisabledLabel>
      ) : (
        <>
          {location.pathname.split('/')[3] === path ? (
            <Label>{description}</Label>
          ) : (
            <Label>
              <Link href={path} underline={false}>
                {description}
              </Link>
            </Label>
          )}
        </>
      )}
      {!erSisteRoute && <ChevronRightIcon aria-hidden />}
    </>
  )
}

const DisabledLabel = styled(Label)`
  color: var(--a-gray-400);
  cursor: not-allowed;
`
