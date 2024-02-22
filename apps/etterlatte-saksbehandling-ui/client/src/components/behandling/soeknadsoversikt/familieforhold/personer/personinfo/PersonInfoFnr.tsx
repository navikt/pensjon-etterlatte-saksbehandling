import { PersonDetailWrapper } from '~components/behandling/soeknadsoversikt/styled'

type Props = {
  fnr: string | undefined
}

export const PersonInfoFnr = ({ fnr }: Props) => {
  return (
    <PersonDetailWrapper adresse={false}>
      <div>
        <strong>Fødselsnummer</strong>
      </div>
      {fnr}
    </PersonDetailWrapper>
  )
}
