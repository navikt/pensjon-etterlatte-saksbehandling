import React from 'react'
import { Tag } from '@navikt/ds-react'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

export const VergemaalTag = ({ vergemaal }: { vergemaal?: VergemaalEllerFremtidsfullmakt }) =>
  vergemaal && (
    <Tag variant="warning-filled" size="small">
      Vergemål
    </Tag>
  )
