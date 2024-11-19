import { isBefore } from 'date-fns'
import { formaterDato } from '~utils/formatering/dato'
import { ErrorMessage } from '@navikt/ds-react'

export const FristWrapper = ({ dato }: { dato?: string }) => {
  const fristHarPassert = !!dato && isBefore(new Date(dato), new Date())

  const frist = dato ? formaterDato(dato) : 'Ingen frist'

  if (fristHarPassert) {
    return <ErrorMessage>{frist}</ErrorMessage>
  } else {
    return <span>{frist}</span>
  }
}
