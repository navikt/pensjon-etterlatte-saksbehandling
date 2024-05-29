import { Box } from '@navikt/ds-react'

type Props = {
  fnr: string | undefined
}

export const PersonInfoFnr = ({ fnr }: Props) => {
  return (
    <Box paddingBlock="2 0">
      <div>
        <strong>Fødselsnummer</strong>
      </div>
      {fnr}
    </Box>
  )
}
