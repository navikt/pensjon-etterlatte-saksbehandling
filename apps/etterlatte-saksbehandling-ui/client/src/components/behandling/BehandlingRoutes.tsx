import { useEffect, useState } from 'react'
import { useMatch, useNavigate } from 'react-router'
import { Beregne } from './beregne'
import { Brev } from './brev'
import { Inngangsvilkaar } from './inngangsvilkaar'
import { Soeknadsoversikt } from './soeknadsoversikt'
import { Utbetalingsoversikt } from './utbetalingsoversikt'
import { Vedtak } from './vedtak'

export const useBehandlingRoutes = () => {
  const [currentRoute, setCurrentRoute] = useState<string | undefined>()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/:section')

  const behandlingRoutes = [
    { path: 'soeknadsoversikt', element: <Soeknadsoversikt /> },
    { path: 'inngangsvilkaar', element: <Inngangsvilkaar /> },
    { path: 'beregne', element: <Beregne /> },
    { path: 'vedtak', element: <Vedtak /> },
    { path: 'utbetalingsoversikt', element: <Utbetalingsoversikt /> },
    { path: 'brev', element: <Brev /> },
  ]
  
  useEffect(() => {
    setCurrentRoute(match?.params.section)
  },[]);

  const next = () => {
    const index = behandlingRoutes.findIndex((item) => item.path === currentRoute)
    const nextPath = behandlingRoutes[index + 1].path
    setCurrentRoute(nextPath);
    navigate(`/behandling/${match?.params.behandlingId}/${nextPath}`)
  }

  return { next, behandlingRoutes, currentRoute }
}
