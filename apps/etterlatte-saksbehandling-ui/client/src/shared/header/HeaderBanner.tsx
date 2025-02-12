import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { HStack, InternalHeader, Spacer } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AppSwitcher } from '~shared/header/AppSwitcher'

export const HeaderBanner = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erDesember = new Date().getMonth() === 11

  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">
        <HStack gap="4">
          {erDesember && (
            <div>
              <img src="/Christmas_tree_02.png" alt="juletre" style={{ height: '1.5rem', width: '1.5rem' }} />
            </div>
          )}
          Gjenny
          {erDesember && (
            <img
              style={{ position: 'absolute', top: '13px', left: '55px' }}
              src="/nisselue.png"
              alt="nisselue"
              height={20}
            />
          )}
        </HStack>
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
