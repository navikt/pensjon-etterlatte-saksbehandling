import { Textarea } from '@navikt/ds-react'
import styled from 'styled-components'

export const SoeknadsoversiktTextArea = styled(Textarea).attrs({
  label: 'Begrunnelse',
  hideLabel: false,
  placeholder: 'Forklar begrunnelsen',
  minRows: 3,
  size: 'small',
  autocomplete: 'off',
})`
  margin-bottom: 10px;
`
