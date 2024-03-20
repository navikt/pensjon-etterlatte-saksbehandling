import { InitiellVurdering } from '~components/klage/vurdering/InitiellVurdering'
import { InitiellVurderingVisning } from '~components/klage/vurdering/InitiellVurderingVisning'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import React from 'react'
import { JaNei } from '~shared/types/ISvar'
import { KlageAvvisning } from '~components/klage/vurdering/KlageAvvisning'
import { HeadingWrapper } from '~components/person/SakOversikt'
import { Button, Heading } from '@navikt/ds-react'
import { ContentHeader, FlexRow } from '~shared/styled'
import { InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { EndeligVurdering } from '~components/klage/vurdering/EndeligVurdering'
import { EndeligVurderingVisning } from '~components/klage/vurdering/EndeligVurderingVisning'
import { Klage } from '~shared/types/Klage'
import { forrigeSteg, klageKanSeBrev, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { useNavigate } from 'react-router'
import { NavigateFunction } from 'react-router/dist/lib/hooks'

export function KlageVurdering({ kanRedigere }: { kanRedigere: boolean }) {
  const klage = useKlage()
  const navigate = useNavigate()

  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

  if (kanRedigere && skalAvvises(klage)) {
    return <KlageAvvisning klage={klage} />
  }

  return (
    <>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurder klagen
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        {kanRedigere ? (
          <>
            <InitiellVurdering klage={klage} />
            {klage.initieltUtfall && <EndeligVurdering klage={klage} />}
          </>
        ) : (
          <>
            {klage.initieltUtfall && <InitiellVurderingVisning klage={klage} />}
            <EndeligVurderingVisning klage={klage} />
          </>
        )}

        {/* Hvis vi ikke viser redigering av endelig utfall (inkl. knapper) så legges knappene inn her*/}
        {(!klage.initieltUtfall || !kanRedigere) && <Navigeringsknapper klage={klage} navigate={navigate} />}
      </InnholdPadding>
    </>
  )
}

function skalAvvises(klage: Klage) {
  const formkrav = klage.formkrav?.formkrav
  return formkrav?.erKlagenFramsattInnenFrist === JaNei.NEI
}

function Navigeringsknapper({ klage, navigate }: { klage: Klage; navigate: NavigateFunction }) {
  return (
    <FlexRow justify="center">
      <Button className="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
        Gå tilbake
      </Button>
      <Button className="button" variant="primary" onClick={() => navigate(nesteSteg(klage, 'vurdering'))}>
        {klageKanSeBrev(klage) ? 'Gå til brev' : 'Gå til oppsummering'}
      </Button>
    </FlexRow>
  )
}
