import { useMatch } from 'react-router'
import { DisabledLabel } from '~components/behandling/stegmeny/StegMeny'
import { Label } from '@navikt/ds-react'
import { NavLink } from 'react-router-dom'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import React from 'react'
import { EtteroppjoerSteg } from '~components/etteroppgjoer/stegmeny/EtteroppjoerForbehandlingSteg'

interface Props {
  path: EtteroppjoerSteg
  description: string
  enabled: boolean
  erSisteRoute?: boolean
}

export function EtteroppgjoerStegLenke({ path, description, enabled, erSisteRoute }: Props) {
  const match = useMatch('/etteroppgjoer/:etteroppgjoerId/:steg')
  const { etteroppgjoerId, steg } = match?.params || {}

  if (!enabled) {
    return <DisabledLabel>{description}</DisabledLabel>
  }

  return (
    <>
      {!enabled ? (
        <DisabledLabel>{description}</DisabledLabel>
      ) : (
        <>
          {steg === path ? (
            <Label>{description}</Label>
          ) : (
            <Label>
              <NavLink to={`/etteroppgjoer/${etteroppgjoerId}/${path}`} style={{ textDecoration: 'none' }}>
                {description}
              </NavLink>
            </Label>
          )}
        </>
      )}
      {!erSisteRoute && <ChevronRightIcon aria-hidden />}
    </>
  )
}
