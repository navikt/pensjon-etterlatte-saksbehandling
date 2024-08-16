import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { InternalHeader, Spacer } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AppSwitcher } from '~shared/header/AppSwitcher'

export const HeaderBanner = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">Gjenny</InternalHeader.Title>
      <Spacer />
      <Search />
      <AppSwitcher />
      <ReleaseAlerts />
      <InternalHeader.User
        data-theme="dark"
        name={innloggetSaksbehandler.navn}
        description={innloggetSaksbehandler.ident}
      />
    </InternalHeader>
  )
}
