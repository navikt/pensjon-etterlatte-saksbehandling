import styled from 'styled-components'
import { VurderingsResultat } from '~store/reducers/BehandlingReducer'

export const OppfyltIcon = (props: { status: VurderingsResultat }) => {
  return (
    <Circle status={props.status}>
      {props.status === VurderingsResultat.OPPFYLT ? (
        <svg
          width="12px"
          height="12px"
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          focusable="false"
          role="img"
        >
          <path
            fillRule="evenodd"
            clipRule="evenodd"
            d="M17 5.25a5.25 5.25 0 11-10.5 0 5.25 5.25 0 0110.5 0zm-5.338 6.361A7.389 7.389 0 004.272 19h10.165l3.65-3.65a7.386 7.386 0 00-6.425-3.739zm10.686 5.041c-.87-.87-2.28-.87-3.15 0l-3.792 3.792L15 24l3.555-.407 3.793-3.792c.87-.87.87-2.28 0-3.149z"
            fill="#fff"
          ></path>
        </svg>
      ) : (
        <>x</>
      )}
    </Circle>
  )
}

const Circle = styled.div<{ status: VurderingsResultat }>`
  background-color: ${(props) => (props.status === VurderingsResultat.OPPFYLT ? '#826BA1' : '#A32A17')};
  color: #fff;
  border-radius: 100%;
  height: 20px;
  width: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
`
