import { Detail } from '@navikt/ds-react'
import { DetailWrapper } from '../../styled'

import { format } from 'date-fns'
export const Soeknadsdato = ({ mottattDato }: { mottattDato: string }) => {
  return (
    <DetailWrapper>
      <Detail size="medium">Søknadsdato</Detail>
      {format(new Date(mottattDato), 'dd.MM.yyyy')}
    </DetailWrapper>
  )
}
