import styled from 'styled-components'
import { VilkaarVurderingsResultat } from '../../store/reducers/BehandlingReducer'

export const StatusIcon = (props: { status: VilkaarVurderingsResultat; large?: boolean }) => {
  const symbol = hentSymbol()

  function hentSymbol() {
    switch (props.status) {
      case VilkaarVurderingsResultat.OPPFYLT:
        return (
          <svg
            width={props.large ? '26px' : '20px'}
            height={props.large ? '26px' : '20px'}
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            focusable="false"
            role="img"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M12 0c6.627 0 12 5.373 12 12s-5.373 12-12 12S0 18.627 0 12 5.373 0 12 0zm5.047 7.671l1.399 1.43-8.728 8.398L6 14.02l1.395-1.434 2.319 2.118 7.333-7.032z"
              fill={colors[props.status]}
            ></path>
          </svg>
        )

      case VilkaarVurderingsResultat.IKKE_OPPFYLT:
        return (
          <svg
            width={props.large ? '26px' : '20px'}
            height={props.large ? '26px' : '20px'}
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            focusable="false"
            role="img"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M12 0c6.627 0 12 5.373 12 12s-5.373 12-12 12S0 18.627 0 12 5.373 0 12 0zm3.571 7L17 8.429 13.428 12 17 15.571 15.571 17 12 13.428 8.429 17 7 15.571 10.572 12 7 8.429 8.429 7 12 10.572 15.571 7z"
              fill={colors[props.status]}
            ></path>
          </svg>
        )
      case VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
        return (
          <>
            <svg
              width={props.large ? '26px' : '20px'}
              height={props.large ? '26px' : '20px'}
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              focusable="false"
              role="img"
            >
              <circle cx="50%" cy="50%" r="10" fill="black" />
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M12 0c6.627 0 12 5.373 12 12s-5.373 12-12 12S0 18.627 0 12 5.373 0 12 0zm0 16a1.5 1.5 0 110 3 1.5 1.5 0 010-3zm1-11v9h-2V5h2z"
                fill={colors[props.status]}
              ></path>
            </svg>
          </>
        )
    }
  }

  return (
    <SvgWrapper status={props.status} large={props.large}>
      {symbol}
    </SvgWrapper>
  )
}

const colors = {
  [VilkaarVurderingsResultat.OPPFYLT]: '#06893a',
  [VilkaarVurderingsResultat.IKKE_OPPFYLT]: '#ba3a26',
  [VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING]: '#FF9100',
}

const SvgWrapper = styled.div<{ status: VilkaarVurderingsResultat; large?: boolean }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
  padding-left: ${(props) => (props.large ? 0 : '16px')};
`
