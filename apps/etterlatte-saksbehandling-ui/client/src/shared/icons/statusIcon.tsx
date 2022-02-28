import styled from 'styled-components'
import { VilkaarVurderingsResultat } from '../../store/reducers/BehandlingReducer'

export const StatusIcon = (props: { status: VilkaarVurderingsResultat; large?: boolean }) => {
  return (
    <Circle status={props.status} large={props.large}>
      {props.status === VilkaarVurderingsResultat.OPPFYLT ? (
        <svg
          width={props.large ? '20px' : '15px'}
          height={props.large ? '20px' : '15px'}
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
      ) : (
        <>!</>
      )}
    </Circle>
  )
}

const Circle = styled.div<{ status: VilkaarVurderingsResultat; large?: boolean }>`
  background-color: ${(props) => (props.status === VilkaarVurderingsResultat.OPPFYLT ? '#1c6937' : '#FFAA33')};
  border-radius: 100%;
  height: ${(props) => (props.large ? '25px' : '20px')};
  width: ${(props) => (props.large ? '25px' : '20px')};
  display: inline-flex;
  justify-content: center;
  align-items: center;
  margin-right: 10px;
`
