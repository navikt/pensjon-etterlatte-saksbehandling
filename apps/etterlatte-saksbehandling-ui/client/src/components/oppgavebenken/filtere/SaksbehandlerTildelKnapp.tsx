import React from 'react'
import { Button } from '@navikt/ds-react'

const SaksbehandlerTildelKnapp: React.FC<{ value: string }> = ({ value }) => {
  //const saksbehandlerReducer = useContext(AppContext).state.saksbehandlerReducer.navn
  // todo onclick skal lagre navnet på saksbehandler på en sak
  const verdi = value ? (
    value
  ) : (
    <Button size={'small'} onClick={() => {}} variant={'secondary'}>
      Tildel meg
    </Button>
  )

  return <div>{verdi}</div>
}

export default SaksbehandlerTildelKnapp
