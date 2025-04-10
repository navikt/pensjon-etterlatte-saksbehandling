import { Search } from './Search'
import { ReleaseAlerts } from './ReleaseAlerts'
import { HStack, InternalHeader, Spacer } from '@navikt/ds-react'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AppSwitcher } from '~shared/header/AppSwitcher'
import confetti from 'canvas-confetti'
export const HeaderBanner = () => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erDesember = new Date().getMonth() === 11

  const erApril = new Date().getMonth() === 3

  const skytKonfetti = () => {
    function randomInRange(min: number, max: number) {
      return Math.random() * (max - min) + min
    }

    confetti({
      angle: randomInRange(55, 125),
      spread: randomInRange(50, 70),
      particleCount: randomInRange(50, 100),
      origin: { y: 0.6 },
    })
  }

  return (
    <InternalHeader data-theme="light">
      <InternalHeader.Title href="/">
        <HStack gap="4">
          {erApril && (
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
          {erApril && (
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
      {innloggetSaksbehandler.ident === 'K105127' && (
        <InternalHeader.Title onClick={skytKonfetti}>Roger modus aktivert</InternalHeader.Title>
      )}
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
