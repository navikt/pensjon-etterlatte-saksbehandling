import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { InternalHeader, Spacer } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AppSwitcher } from '~shared/header/AppSwitcher'

export const HeaderBanner = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erDesember = new Date().getMonth() === 11

  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">
        Gjenny
        {erDesember && (
          <img
            style={{ position: 'absolute', left: '15px', top: '15px' }}
            src="/nisselue.png"
            alt="nisselue"
            height={20}
          />
        )}
      </InternalHeader.Title>
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
