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
  const { oppgaveId, steg } = match?.params || {}

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
              <NavLink to={`/aktivitet-vurdering/${oppgaveId}/${path}`} style={{ textDecoration: 'none' }}>
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
