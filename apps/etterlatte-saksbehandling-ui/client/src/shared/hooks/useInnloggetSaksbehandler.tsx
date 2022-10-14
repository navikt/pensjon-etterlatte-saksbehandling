import { useEffect, useState } from 'react'
import { ISaksbehandler } from '../../store/reducers/SaksbehandlerReducer'
import { hentInnloggetSaksbehandler } from '../api/user'
import { IApiResponse } from '../api/types'
import { useAppDispatch } from '../../store/Store'
import { setSaksbehandler } from '../../store/reducers/SaksbehandlerReducer'

const useInnloggetSaksbehandler = () => {
  const dispatch = useAppDispatch()
  const [loaded, setLoaded] = useState<boolean>(false)

  useEffect(() => {
    hentInnloggetSaksbehandler().then((response: IApiResponse<ISaksbehandler>) => {
      if (response.data) {
        dispatch(setSaksbehandler(response.data))
      }
      setLoaded(true)
    })
  }, [])

  return loaded
}

export default useInnloggetSaksbehandler
