import React from 'react'
import { Button } from '@navikt/ds-react'

const SaksbehandlerFilterListe: React.FC<{ value: string }> = ({ value }) => {
  //const saksbehandlerReducer = useContext(AppContext).state.saksbehandlerReducer.navn

  const verdi = value ? (
    value
  ) : (
    <Button size={'small'} onClick={() => {}} variant={'secondary'}>
      Plukk
    </Button>
  )

  return <div>{verdi}</div>
}

export default SaksbehandlerFilterListe
