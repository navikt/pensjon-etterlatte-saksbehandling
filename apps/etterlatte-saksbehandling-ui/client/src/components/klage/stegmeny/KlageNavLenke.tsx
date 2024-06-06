import React from 'react'
import { Label, Link } from '@navikt/ds-react'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'

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
