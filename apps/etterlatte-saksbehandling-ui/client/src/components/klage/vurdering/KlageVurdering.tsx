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
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { useNavigate } from 'react-router'

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
            {klage.initieltUtfall ? (
              <EndeligVurdering klage={klage} />
            ) : (
              <FlexRow justify="center">
                <Button type="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
                  Gå tilbake
                </Button>
              </FlexRow>
            )}
          </>
        ) : (
          <>
            {klage.initieltUtfall && <InitiellVurderingVisning klage={klage} />}
            <EndeligVurderingVisning klage={klage} />
          </>
        )}
      </InnholdPadding>
    </>
  )
}

function skalAvvises(klage: Klage) {
  const formkrav = klage.formkrav?.formkrav
  return formkrav?.erKlagenFramsattInnenFrist === JaNei.NEI
}
