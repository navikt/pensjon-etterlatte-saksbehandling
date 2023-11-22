import { useEffect } from 'react'
import { hentInnloggetSaksbehandler } from '../api/user'
import { useAppDispatch } from '~store/Store'
import { setSaksbehandler } from '~store/reducers/InnloggetSaksbehandlerReducer'
import { isSuccess, useApiCall } from './useApiCall'

const useInnloggetSaksbehandler = () => {
  const dispatch = useAppDispatch()
  const [saksbehandler, hentSaksbehandler] = useApiCall(hentInnloggetSaksbehandler)

  useEffect(() => {
    hentSaksbehandler({}, (response) => {
      if (response) {
        dispatch(setSaksbehandler(response))
      }
    })
  }, [])

  return isSuccess(saksbehandler)
}

export default useInnloggetSaksbehandler
