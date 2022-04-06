import { Label } from '@navikt/ds-react'
import { DetailWrapper } from '../../styled'

import { format } from 'date-fns'
export const Soeknadsdato = ({ mottattDato }: { mottattDato: string }) => {
  return (
    <DetailWrapper>
      <Label size="small">Søknadsdato</Label>
      {format(new Date(mottattDato), 'dd.MM.yyyy')}
    </DetailWrapper>
  )
}
