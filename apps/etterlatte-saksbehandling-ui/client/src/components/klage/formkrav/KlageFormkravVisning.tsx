import { useKlage } from '~components/klage/useKlage'
import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Heading, Table } from '@navikt/ds-react'
import { formaterKanskjeStringDato, formaterVedtakType } from '~utils/formattering'
import { isFailure, isInitial, mapApiResult } from '~shared/api/apiUtils'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import Spinner from '~shared/Spinner'
import { ExclamationmarkTriangleFillIcon } from '@navikt/aksel-icons'
import { JaNei } from '~shared/types/ISvar'

export function KlageFormkravVisning() {
  const klage = useKlage()
  const [vedtak, hentVedtakGittBehandling] = useApiCall(hentVedtakSammendrag)

  if (klage) {
    useEffect(() => {
      const behandlingId: string | null = klage?.formkrav?.formkrav?.vedtaketKlagenGjelder?.behandlingId ?? null

      if (behandlingId && (isInitial(vedtak) || isFailure(vedtak))) {
        hentVedtakGittBehandling(behandlingId)
      }
    })
  }

  if (!klage) return

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
        {mapApiResult(
          vedtak,
          <Table.DataCell colSpan={2}>
            <Spinner visible label="" margin="0" />
          </Table.DataCell>,
          (apierror) => (
            <Table.DataCell colSpan={2}>
              <ExclamationmarkTriangleFillIcon
                title={apierror.detail || 'Feil oppsto ved henting av sammendrag for vedtak'}
              />
            </Table.DataCell>
          ),
          (vedtak) => (
            <>
              <Heading size="small" spacing>
                Hvilket vedtak klages det på?
              </Heading>
              <BodyShort spacing>
                Vedtak {vedtak.id} om {formaterVedtakType(vedtak.vedtakType!!)} -{' '}
                {formaterKanskjeStringDato(vedtak.datoAttestert)}
              </BodyShort>
              <br />
            </>
          )
        )}
        <Heading size="small" spacing>
          Er klager part i saken?
        </Heading>
        <BodyShort spacing>{klage.formkrav?.formkrav?.erKlagerPartISaken == JaNei.JA ? 'Ja' : 'Nei'} </BodyShort>

        <Heading size="small" spacing>
          Er klagen signert?{' '}
        </Heading>
        <BodyShort spacing>{klage.formkrav?.formkrav?.erKlagenSignert == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>

        <Heading size="small" spacing>
          Klages det på konkrete elementer i vedtaket?{' '}
        </Heading>
        <BodyShort spacing>
          {klage.formkrav?.formkrav?.gjelderKlagenNoeKonkretIVedtaket == JaNei.JA ? 'Ja' : 'Nei'}
        </BodyShort>

        <Heading size="small" spacing>
          Er klagen framsatt innenfor klagefristen?{' '}
        </Heading>
        <BodyShort spacing>{klage.formkrav?.formkrav?.erKlagenFramsattInnenFrist == JaNei.JA ? 'Ja' : 'Nei'}</BodyShort>
      </InnholdPadding>
    </Content>
  )
}
