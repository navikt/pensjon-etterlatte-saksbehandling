import styled from 'styled-components'
import { VurderingsResultat } from '~store/reducers/BehandlingReducer'

export const GyldighetIcon = (props: { status: VurderingsResultat; large?: boolean }) => {
  return (
    <SvgWrapper status={props.status} large={props.large}>
      {props.status === VurderingsResultat.OPPFYLT ? (
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
            fill="#06893a"
          ></path>
        </svg>
      ) : (
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
            fill="#FF9100"
          ></path>
        </svg>
      )}
    </SvgWrapper>
  )
}

const SvgWrapper = styled.div<{ status: VurderingsResultat; large?: boolean }>`
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 15px;
  margin-left: 1em;
`
