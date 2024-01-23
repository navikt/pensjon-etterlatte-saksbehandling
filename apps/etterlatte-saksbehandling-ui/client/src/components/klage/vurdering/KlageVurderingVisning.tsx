import { useKlage } from '~components/klage/useKlage'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import React from 'react'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { useNavigate } from 'react-router-dom'
import { forrigeSteg, nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { formaterKlageutfall, VisInnstilling, VisOmgjoering } from '~components/klage/vurdering/KlageVurderingFelles'

export function KlageVurderingVisning() {
  const klage = useKlage()
  const navigate = useNavigate()

  if (!klage) return

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Vurdering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <BodyShort spacing>Utfallet av klagen er {formaterKlageutfall(klage)}.</BodyShort>

        <VisInnstilling klage={klage} kanRedigere={false} />

        <VisOmgjoering klage={klage} kanRedigere={false} />
      </InnholdPadding>
      <FlexRow justify="center">
        <Button
          className="button"
          variant="secondary"
          onClick={() => navigate(`/klage/${klage.id}/${forrigeSteg(klage, 'vurdering')}`)}
        >
          Gå tilbake
        </Button>
        <Button
          className="button"
          variant="primary"
          onClick={() => navigate(`/klage/${klage.id}/${nesteSteg(klage, 'vurdering')}`)}
        >
          Neste side
        </Button>
      </FlexRow>
    </Content>
  )
}
