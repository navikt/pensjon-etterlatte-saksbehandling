import { isBefore } from 'date-fns'
import { formaterDato } from '~utils/formatering/dato'
import styled from 'styled-components'

const FristSpan = styled.span<{ fristHarPassert: boolean }>`
  color: ${(p) => p.fristHarPassert && 'var(--a-text-danger)'};
`

export const FristWrapper = ({ dato }: { dato?: string }) => {
  const fristHarPassert = !!dato && isBefore(new Date(dato), new Date())

  return <FristSpan fristHarPassert={fristHarPassert}>{dato ? formaterDato(dato) : 'Ingen frist'}</FristSpan>
}
