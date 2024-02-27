import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { InternalHeader, Spacer } from '@navikt/ds-react'
import { useAppSelector } from '~store/Store'

export const HeaderBanner = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">Gjenny</InternalHeader.Title>
      <Spacer />
      <Search />
      <ReleaseAlerts />
      <InternalHeader.User
        data-theme="dark"
        name={innloggetSaksbehandler.navn}
        description={innloggetSaksbehandler.ident}
      />
    </InternalHeader>
  )
}
