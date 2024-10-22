import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'
import { Label } from '@navikt/ds-react'
import { ChevronRightIcon } from '@navikt/aksel-icons'
import React from 'react'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { NavLink, useMatch } from 'react-router-dom'

interface Props {
  path: AktivitetspliktSteg
  description: string
  enabled: boolean
  erSisteRoute?: boolean
}

export function AktivitetNavLenke({ path, description, enabled, erSisteRoute }: Props) {
  const match = useMatch('/aktivitet-vurdering/:oppgaveId/:steg')
  const currentPath = match?.params?.steg

  return (
    <>
      {!enabled ? (
        <DisabledLabel>{description}</DisabledLabel>
      ) : (
        <>
          {currentPath === path ? (
            <Label>{description}</Label>
          ) : (
            <Label>
              <NavLink to={path} style={{ textDecoration: 'none' }}>
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
