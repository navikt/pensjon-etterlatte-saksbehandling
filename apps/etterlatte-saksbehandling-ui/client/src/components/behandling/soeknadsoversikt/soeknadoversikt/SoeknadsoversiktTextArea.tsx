import { Textarea } from '@navikt/ds-react'
import styled from 'styled-components'

export const SoeknadsoversiktTextArea = styled(Textarea).attrs((props) => ({
  label: 'Begrunnelse',
  hideLabel: false,
  placeholder: props.placeholder || 'Forklar begrunnelsen',
  minRows: 3,
  size: 'small',
  autoComplete: 'off',
}))`
  margin-bottom: 10px;
`
