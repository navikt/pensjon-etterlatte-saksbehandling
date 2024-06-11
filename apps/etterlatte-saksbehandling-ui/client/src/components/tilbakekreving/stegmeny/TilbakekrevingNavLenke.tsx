import React from 'react'
import { Label, Link } from '@navikt/ds-react'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'

export const TILBAKEKREVINGSTEG = ['vurdering', 'utbetalinger', 'oppsummering', 'brev'] as const

export type Tilbakekrevingsteg = (typeof TILBAKEKREVINGSTEG)[number]

export interface TilbakekrevingNavLenkeProps {
  path: Tilbakekrevingsteg
  description: string
  enabled: boolean
  separator: boolean
}

export function TilbakekrevingNavLenke(props: TilbakekrevingNavLenkeProps) {
  const { path, description, enabled, separator } = props

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
      {separator && <ChevronRightIcon aria-hidden />}
    </>
  )
}
