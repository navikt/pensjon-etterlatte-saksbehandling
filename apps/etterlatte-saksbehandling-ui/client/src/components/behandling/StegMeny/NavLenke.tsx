import { ChevronRightIcon } from '@navikt/aksel-icons'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteTypes, useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { Label, Link } from '@navikt/ds-react'
import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'

export const NavLenke = (props: {
  pathInfo: BehandlingRouteTypes
  behandling: IBehandlingReducer
  separator: boolean
}) => {
  const { routeErGyldig } = useBehandlingRoutes()

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
