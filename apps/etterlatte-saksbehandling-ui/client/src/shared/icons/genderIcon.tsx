import styled from 'styled-components'

export enum GenderList {
  male = 'male',
  female = 'female',
}

export const GenderIcon = (props: { gender: GenderList }) => {
  if (props.gender === GenderList.female) {
    return (
      <Gender gender={props.gender}>
        <svg
          width="1em"
          height="1em"
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          focusable="false"
          role="img"
        >
          <path
            fillRule="evenodd"
            clipRule="evenodd"
            d="M15 3a3 3 0 11-6 0 3 3 0 016 0zM8.847 7.991A2 2 0 0110.574 7h2.852a2 2 0 011.727.991L21 18h-6v6h-2v-6h-2v6H9v-6H3L8.847 7.991z"
            fill="#fff"
          ></path>
        </svg>
      </Gender>
    )
  }

  return (
    <Gender gender={props.gender}>
      <svg
        width="1em"
        height="1em"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        focusable="false"
        role="img"
      >
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M15 3a3 3 0 11-6 0 3 3 0 016 0zM7 9a2 2 0 012-2h6a2 2 0 012 2v9h-2v6h-2v-6h-2v6H9v-6H7V9z"
          fill="#fff"
        ></path>
      </svg>
    </Gender>
  )
}

const Gender = styled.div<{ gender: GenderList }>`
  line-height: 30px;
  background-color: ${(props) => (props.gender === GenderList.female ? '#c86151' : 'blue')};
  padding: 3px;
  width: 30px;
  height: 30px;
  border-radius: 100%;
  text-align: center;
`
