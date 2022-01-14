import React, { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { SaksbehandlerFilter, saksbehandlerFilter } from '../typer/oppgavebenken'

const SaksbehandlerFilterListe: React.FC<{ value: string }> = ({ value }) => {
  const saksbehandlerReducer = useContext(AppContext).state.saksbehandlerReducer.navn
  return <div>{saksbehandlerFilter(saksbehandlerReducer)[value as SaksbehandlerFilter]?.navn}</div>
}

export default SaksbehandlerFilterListe
