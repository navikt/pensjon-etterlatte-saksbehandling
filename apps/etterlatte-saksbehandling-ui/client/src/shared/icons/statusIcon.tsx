import styled from 'styled-components'
import { VilkaarVurderingsResultat } from '../../store/reducers/BehandlingReducer'

export const StatusIcon = (props: { status: VilkaarVurderingsResultat; large?: boolean }) => {
  const symbol = hentSymbol()

  function hentSymbol() {
    switch (props.status) {
      case VilkaarVurderingsResultat.OPPFYLT:
        return (
          <svg
            width={props.large ? '19px' : '15px'}
            height={props.large ? '19px' : '15px'}
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            focusable="false"
            role="img"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M8.028 16L20.5 4 22 5.5 8.028 19 2 13l1.5-1.5L8.028 16z"
              fill="#fff"
            ></path>
          </svg>
        )

      case VilkaarVurderingsResultat.IKKE_OPPFYLT:
        return (
          <svg
            width={props.large ? '19px' : '15px'}
            height={props.large ? '19px' : '15px'}
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            focusable="false"
            role="img"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M21 4.385L13.385 12 21 19.615 19.615 21 12 13.385 4.385 21 3 19.615 10.615 12 3 4.385 4.385 3 12 10.615 19.615 3 21 4.385z"
              fill="#fff"
            ></path>
          </svg>
        )
      case VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
        return (
          <svg
            width={props.large ? '19px' : '15px'}
            height={props.large ? '19px' : '15px'}
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            focusable="false"
            role="img"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M10.5 2 L10.5 16 L13.5 16 L13.5 2 Z M10.5 19 L10.5 22 L13.5 22 L13.5 19 Z"
              fill="#fff"
            ></path>
          </svg>
        )
    }
  }

  return (
    <Circle status={props.status} large={props.large}>
      {symbol}
    </Circle>
  )
}

const colors = {
  [VilkaarVurderingsResultat.OPPFYLT]: '#1c6937',
  [VilkaarVurderingsResultat.IKKE_OPPFYLT]: '#f34040',
  [VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING]: '#FFAA33',
}

const Circle = styled.div<{ status: VilkaarVurderingsResultat; large?: boolean }>`
  background-color: ${(props) => colors[props.status]};
  border-radius: 100%;
  height: ${(props) => (props.large ? '26px' : '20px')};
  width: ${(props) => (props.large ? '26px' : '20px')};
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 10px;
`
