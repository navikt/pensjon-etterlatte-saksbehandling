import React from 'react'
import { Tag } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'

export const DoedsdatoTag = ({ doedsdato }: { doedsdato?: Date }) =>
  doedsdato && (
    <Tag data-color="neutral" variant="strong" size="small">
      &#10013; {formaterDato(doedsdato)}
    </Tag>
  )
