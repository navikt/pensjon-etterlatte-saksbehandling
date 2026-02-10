import React from 'react'
import { Tag } from '@navikt/ds-react'
import { VergemaalEllerFremtidsfullmakt } from '~components/person/typer'

export const VergemaalTag = ({ vergemaal }: { vergemaal?: VergemaalEllerFremtidsfullmakt }) =>
  vergemaal && (
    <Tag data-color="warning" variant="strong" size="small">
      Vergemål
    </Tag>
  )
