import { useContext, useEffect, useState } from 'react'
import { ISaksbehandler } from '../../store/reducers/SaksbehandlerReducer'
import { AppContext } from '../../store/AppContext'
import { hentInnloggetSaksbehandler } from '../api/user'
import { IApiResponse } from '../api/types'

const useInnloggetSaksbehandler = () => {
  const { dispatch } = useContext(AppContext)

  const [loaded, setLoaded] = useState<boolean>(false)

  useEffect(() => {
    hentInnloggetSaksbehandler().then((response: IApiResponse<ISaksbehandler>) => {
      dispatch({ type: 'setSaksbehandler', data: response.data })
      setLoaded(true)
    })
  }, [])

  return loaded
}

export default useInnloggetSaksbehandler
