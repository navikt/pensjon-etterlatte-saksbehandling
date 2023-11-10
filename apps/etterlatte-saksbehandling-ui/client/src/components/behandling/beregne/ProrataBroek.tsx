import { IProrataBroek } from '~shared/api/trygdetid'

export const ProrataBroek = ({ broek }: { broek: IProrataBroek }) => {
  return (
    <>
      {broek.teller} / {broek.nevner}
    </>
  )
}
