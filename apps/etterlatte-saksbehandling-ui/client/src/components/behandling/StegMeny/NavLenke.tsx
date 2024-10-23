import { ChevronRightIcon } from '@navikt/aksel-icons'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteContext, BehandlingRouteType } from '~components/behandling/BehandlingRoutes'
import { Label, Link } from '@navikt/ds-react'
import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'
import { useContext } from 'react'

export const NavLenke = (props: {
  pathInfo: BehandlingRouteType
  behandling: IBehandlingReducer
  separator: boolean
}) => {
  const { routeErGyldig } = useContext(BehandlingRouteContext)

  const { pathInfo, separator } = props

  return (
    <>
      {routeErGyldig(pathInfo.path) ? (
        <>
          {location.pathname.split('/')[3] === pathInfo.path ? (
            <Label>{pathInfo.description}</Label>
          ) : (
            <Label>
              <Link href={pathInfo.path} underline={false}>
                {pathInfo.description}
              </Link>
            </Label>
          )}
        </>
      ) : (
        <DisabledLabel>{pathInfo.description}</DisabledLabel>
      )}
      {separator && <ChevronRightIcon aria-hidden />}
    </>
  )
}
