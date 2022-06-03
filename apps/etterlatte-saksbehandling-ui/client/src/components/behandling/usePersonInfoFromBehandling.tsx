import { useContext } from 'react'
import { AppContext } from '../../store/AppContext'

export const usePersonInfoFromBehandling = () => {
  const ctx = useContext(AppContext)

  const behandling = ctx.state.behandlingReducer
  console.log(behandling)

  const mottattDato = '2022-02-14T14:37:24.573612786' //TODO: legg med i backend

  //TODO regne ut virkningstidspunkt i backend
  const virkningstidspunkt = '2022-02-14T14:37:24.573612786'

  return {
    mottattDato,
    virkningstidspunkt,
  }
}
