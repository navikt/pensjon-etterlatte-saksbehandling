import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne'
import { Inngangsvilkaar } from './inngangsvilkaar'
import { Soeknadsoversikt } from './soeknadsoversikt'
import { Brev } from './brev'
//import { Utbetalingsoversikt } from './utbetalingsoversikt'
//import { Vedtak } from './vedtak'

export const useBehandlingRoutes = () => {
  const [currentRoute, setCurrentRoute] = useState<string | undefined>()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/:section')

  const behandlingRoutes = [
    { path: 'soeknadsoversikt', element: <Soeknadsoversikt /> },
    { path: 'inngangsvilkaar', element: <Inngangsvilkaar /> },
    { path: 'beregne', element: <Beregne /> },
    //{ path: 'vedtak', element: <Vedtak /> },
    //{ path: 'utbetalingsoversikt', element: <Utbetalingsoversikt /> },
    { path: 'brev', element: <Brev /> },
  ]

  const firstPage = behandlingRoutes.findIndex((item) => item.path === currentRoute) === 0
  const lastPage = behandlingRoutes.findIndex((item) => item.path === currentRoute) === 5

  useEffect(() => {
    setCurrentRoute(match?.params.section)
  }, [match])

  const next = () => {
    const index = behandlingRoutes.findIndex((item) => item.path === currentRoute)
    const nextPath = behandlingRoutes[index + 1].path
    setCurrentRoute(nextPath)
    navigate(`/behandling/${match?.params.behandlingId}/${nextPath}`)
    window.scrollTo(0, 0)
  }

  const back = () => {
    const index = behandlingRoutes.findIndex((item) => item.path === currentRoute)
    const previousPath = behandlingRoutes[index - 1].path
    setCurrentRoute(previousPath)
    navigate(`/behandling/${match?.params.behandlingId}/${previousPath}`)
    window.scrollTo(0, 0)
  }

  return { next, back, lastPage, firstPage, behandlingRoutes, currentRoute }
}
