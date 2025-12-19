import { useMatch } from 'react-router'
import { DisabledLabel } from '~components/behandling/stegmeny/StegMeny'
import { Label } from '@navikt/ds-react'
import { NavLink } from 'react-router-dom'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import React from 'react'
import { EtteroppjoerForbehandlingSteg } from '~components/etteroppgjoer/forbehandling/stegmeny/EtteroppjoerForbehandlingStegmeny'

interface Props {
  path: EtteroppjoerForbehandlingSteg
  description: string
  enabled: boolean
  erSisteRoute?: boolean
}

export function EtteroppgjoerForbehandlingStegLenke({ path, description, enabled, erSisteRoute }: Props) {
  const match = useMatch('/etteroppgjoer/:forbehandlingId/:steg')
  const { forbehandlingId, steg } = match?.params || {}

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
              <NavLink to={`/etteroppgjoer/${forbehandlingId}/${path}`} style={{ textDecoration: 'none' }}>
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
