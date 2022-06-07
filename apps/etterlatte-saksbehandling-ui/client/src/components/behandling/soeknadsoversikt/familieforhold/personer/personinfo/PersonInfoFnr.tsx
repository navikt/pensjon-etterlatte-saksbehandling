import { PersonDetailWrapper } from '../../../styled'

type Props = {
  fnr: string | undefined
}

export const PersonInfoFnr: React.FC<Props> = ({ fnr }) => {
  return (
    <PersonDetailWrapper adresse={false}>
      <div>
        <strong>Fødselsnummer</strong>
      </div>
      {fnr}
    </PersonDetailWrapper>
  )
}
