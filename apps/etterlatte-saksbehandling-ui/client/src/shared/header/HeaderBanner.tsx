import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { HStack, InternalHeader, Spacer } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AppSwitcher } from '~shared/header/AppSwitcher'

export const HeaderBanner = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erDesember = new Date().getMonth() === 11

  const erPaaske = false

  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">
        <HStack gap="space-4">
          {erPaaske && (
            <div>
              <img src="/paaskekylling.png" alt="Påskekylling" style={{ height: '1.75rem', width: '1.75rem' }} />
            </div>
          )}
          {erDesember && (
            <div>
              <img src="/Christmas_tree_02.png" alt="juletre" style={{ height: '1.5rem', width: '1.5rem' }} />
            </div>
          )}
          Gjenny
          {erPaaske && (
            <div>
              <img src="/paaskeegg.png" alt="Påskeegg" style={{ height: '1.75rem', width: '1.75rem' }} />
            </div>
          )}
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
