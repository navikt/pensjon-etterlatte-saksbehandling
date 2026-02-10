import React from 'react'
import { Tag } from '@navikt/ds-react'
import { AktivitetspliktUnntakType } from '~shared/types/Aktivitetsplikt'

export const AktivitetspliktUnntakTypeTag = ({ unntak }: { unntak: AktivitetspliktUnntakType }) => {
  switch (unntak) {
    case AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT:
      return (
        <Tag data-color="neutral" variant="outline">
          Varig
        </Tag>
      )
    default:
      return (
        <Tag data-color="neutral" variant="outline">
          Midlertidig
        </Tag>
      )
  }
}
