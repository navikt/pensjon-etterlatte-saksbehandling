import styled from 'styled-components'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { SuccessColored } from '@navikt/ds-icons'
import { ErrorColored } from '@navikt/ds-icons'
import { WarningColored } from '@navikt/ds-icons'

export const StatusIcon = (props: { status: VurderingsResultat; noLeftPadding?: boolean }) => {
  const symbol = hentSymbol()

  function hentSymbol() {
    switch (props.status) {
      case VurderingsResultat.OPPFYLT:
        return <SuccessColored />
      case VurderingsResultat.IKKE_OPPFYLT:
        return <ErrorColored />
      case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
        return <WarningColored />
    }
  }

  return (
    <SvgWrapper status={props.status} noLeftPadding={props.noLeftPadding}>
      {symbol}
    </SvgWrapper>
  )
}

const SvgWrapper = styled.div<{ status: VurderingsResultat; large?: boolean; noLeftPadding?: boolean }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
  padding-left: ${(props) => (props.large || props.noLeftPadding ? 0 : '16px')};
`
