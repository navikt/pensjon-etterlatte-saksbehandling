import { useKlage } from '~components/klage/useKlage'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { formaterKanskjeStringDato, formaterVedtakType } from '~utils/formattering'
import React from 'react'
import { JaNei } from '~shared/types/ISvar'
import { useNavigate } from 'react-router-dom'
import { nesteSteg } from '~components/klage/stegmeny/KlageStegmeny'

export function KlageFormkravVisning() {
  const klage = useKlage()

  if (!klage) return

  const navigate = useNavigate()
  const formkrav = klage.formkrav?.formkrav
  const vedtak = formkrav?.vedtaketKlagenGjelder
  const saksbehandler = klage.formkrav?.saksbehandler
  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Formkrav
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <Heading size="small" spacing>
          Hvilket vedtak klages det på?
        </Heading>
        <BodyShort spacing>
          {vedtak &&
            `Vedtak ${vedtak.id} om ${formaterVedtakType(vedtak.vedtakType!!)} - ` +
              formaterKanskjeStringDato(vedtak.datoAttestert)}
        </BodyShort>

        <Heading size="small" spacing>
          Er klager part i saken?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagerPartISaken == JaNei.JA ? 'Ja' : 'Nei'} </BodyShort>

        <Heading size="small" spacing>
          Er klagen signert?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagenSignert == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Klages det på konkrete elementer i vedtaket?
        </Heading>
        <BodyShort spacing>{formkrav?.gjelderKlagenNoeKonkretIVedtaket == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Er klagen framsatt innenfor klagefristen?
        </Heading>
        <BodyShort spacing>{formkrav?.erKlagenFramsattInnenFrist == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Saksbehandler
        </Heading>
        <BodyShort spacing>{saksbehandler?.ident}</BodyShort>
      </InnholdPadding>
      <FlexRow justify="center">
        <Button onClick={() => navigate(`/klage/${klage.id}/${nesteSteg(klage, 'formkrav')}`)}>Neste side</Button>
      </FlexRow>
    </Content>
  )
}
