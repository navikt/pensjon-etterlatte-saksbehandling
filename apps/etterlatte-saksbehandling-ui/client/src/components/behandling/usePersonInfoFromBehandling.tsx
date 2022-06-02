import { useContext } from 'react'
import { AppContext } from '../../store/AppContext'
import { hentVirkningstidspunkt } from './soeknadsoversikt/utils'

export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const behandling = ctx.state.behandlingReducer

  const mottattDato = '2022-02-14T14:37:24.573612786' //TODO: legg med i backend

  //TODO regne ut virkningstidspunkt i backend
  const virkningstidspunkt: string = hentVirkningstidspunkt(
    behandling.kommerSoekerTilgode.familieforhold.avdoed.doedsdato,
    mottattDato
  )

  return {
    mottattDato,
    virkningstidspunkt,
  }
}
