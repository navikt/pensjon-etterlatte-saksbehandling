import React from 'react'
import { Tag } from '@navikt/ds-react'
import { formaterDato } from '~utils/formattering'

export const DoedsdatoTag = ({ doedsdato }: { doedsdato?: Date }) =>
  doedsdato && (
    <Tag variant="neutral-filled" size="small">
      &#10013; {formaterDato(doedsdato)}
    </Tag>
  )
