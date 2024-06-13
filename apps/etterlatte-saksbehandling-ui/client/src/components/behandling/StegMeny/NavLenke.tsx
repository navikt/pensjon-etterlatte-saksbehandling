import { ChevronRightIcon } from '@navikt/aksel-icons'
import { hentGyldigeNavigeringsStatuser } from '~components/behandling/felles/utils'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { BehandlingRouteTypes } from '~components/behandling/BehandlingRoutes'
import { Label, Link } from '@navikt/ds-react'
import { DisabledLabel } from '~components/behandling/StegMeny/stegmeny'

export const NavLenke = (props: {
  pathInfo: BehandlingRouteTypes
  behandling: IBehandlingReducer
  separator: boolean
}) => {
  const { pathInfo, separator } = props
  const { status } = props.behandling

  const routeErDisabled =
    pathInfo.kreverBehandlingsstatus &&
    !hentGyldigeNavigeringsStatuser(status).includes(pathInfo.kreverBehandlingsstatus(props.behandling))
  return (
    <>
      {routeErDisabled ? (
        <DisabledLabel>{pathInfo.description}</DisabledLabel>
      ) : (
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
      )}
      {separator && <ChevronRightIcon aria-hidden />}
    </>
  )
}
