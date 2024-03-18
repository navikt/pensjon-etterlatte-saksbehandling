import React, { ReactNode } from 'react'
import { CopyButton } from '@navikt/ds-react'

export const KopierFnr = ({ fnr }: { fnr: string }): ReactNode => {
  return <CopyButton copyText={fnr} text={fnr} size="small" iconPosition="right" />
}
