import { Accordion, Tabs, VStack } from '@navikt/ds-react'
import SlateEditor from '~components/behandling/brev/SlateEditor'
import React, { useEffect, useState } from 'react'
import { FilePdfIcon, PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { IBrev } from '~shared/types/Brev'
import { hentManuellPayload, lagreManuellPayload, tilbakestillManuellPayload } from '~shared/api/brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { isPending, isPendingOrInitial, isSuccess, isSuccessOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TilbakestillOgLagreRad } from '~components/behandling/brev/TilbakestillOgLagreRad'
import { formaterTidspunktTimeMinutterSekunder } from '~utils/formatering/dato'
import VedleggInfo from '~components/vedlegg/VedleggInfo'

enum ManueltBrevFane {
  REDIGER = 'REDIGER',
  REDIGER_VEDLEGG = 'REDIGER VEDLEGG',
  FORHAANDSVIS = 'FORHAANDSVIS',
}

export interface LagretStatus {
  lagret: boolean
  beskrivelse?: string
}

interface RedigerbartBrevProps {
  brev: IBrev
  kanRedigeres: boolean
  lukkAdvarselBehandlingEndret?: () => void
  tilbakestillingsaction: () => void
  skalGaaViaBehandling?: boolean
}

export default function RedigerbartBrev({
  brev,
  kanRedigeres,
  lukkAdvarselBehandlingEndret,
  tilbakestillingsaction,
  skalGaaViaBehandling,
}: RedigerbartBrevProps) {
  const [fane, setFane] = useState<string>(kanRedigeres ? ManueltBrevFane.REDIGER : ManueltBrevFane.FORHAANDSVIS)
  const [content, setContent] = useState<any[]>([])
  const [vedlegg, setVedlegg] = useState<any[]>([])
  const [lagretStatus, setLagretStatus] = useState<LagretStatus>({ lagret: false })

  const [hentManuellPayloadStatus, apiHentManuellPayload] = useApiCall(hentManuellPayload)
  const [lagreManuellPayloadStatus, apiLagreManuellPayload] = useApiCall(lagreManuellPayload)
  const [tilbakestillManuellPayloadStatus, apiTilbakestillManuellPayload] = useApiCall(tilbakestillManuellPayload)

  useEffect(() => {
    if (!brev.id) return

    apiHentManuellPayload({ brevId: brev.id, sakId: brev.sakId }, (payload: any) => {
      setContent(payload.hoveddel)
      setVedlegg(payload.vedlegg)
    })
  }, [brev.id])

  const lagre = () => {
    apiLagreManuellPayload({ brevId: brev.id, sakId: brev.sakId, payload: content, payload_vedlegg: vedlegg }, () => {
      setLagretStatus({
        lagret: true,
        beskrivelse: `Lagret kl. ${formaterTidspunktTimeMinutterSekunder(new Date())}`,
      })
      if (lukkAdvarselBehandlingEndret) lukkAdvarselBehandlingEndret()
    })
  }

  const tilbakestill = () => {
    if (!brev.behandlingId) {
      throw new Error(`Prøvde å tilbakestille et brev med id ${brev.id} som ikke er koblet på en behandling. 
      Dette skal ikke kunne skje.`)
    }

    apiTilbakestillManuellPayload(
      {
        brevId: brev.id,
        sakId: brev.sakId,
        behandlingId: brev.behandlingId,
        brevtype: brev.brevtype,
      },
      (payload: any) => {
        setContent(payload.hoveddel)
        setVedlegg(payload.vedlegg)
        setLagretStatus({
          lagret: true,
          beskrivelse: `Lagret kl. ${formaterTidspunktTimeMinutterSekunder(new Date())}`,
        })
        if (tilbakestillingsaction) tilbakestillingsaction()
        if (lukkAdvarselBehandlingEndret) lukkAdvarselBehandlingEndret()
      }
    )
  }

  const onChange = (value: any[]) => {
    setLagretStatus({
      lagret: false,
      beskrivelse: `Sist endret kl. ${formaterTidspunktTimeMinutterSekunder(new Date())} (ikke lagret)`,
    })
    setContent(value)
  }

  const onChangeVedlegg = (value: any[], key?: string) => {
    if (!key) return
    setLagretStatus({
      lagret: false,
      beskrivelse: `Sist endret kl. ${formaterTidspunktTimeMinutterSekunder(new Date())} (ikke lagret)`,
    })
    const oppdatertVedlegg = vedlegg.map((ved) => {
      if (ved.key === key) return { ...ved, payload: value }
      return ved
    })
    setVedlegg(oppdatertVedlegg)
  }

  return (
    <Container>
      <Tabs value={fane} onChange={setFane}>
        <Tabs.List>
          <Tabs.Tab
            value={ManueltBrevFane.REDIGER}
            label={kanRedigeres ? 'Rediger' : 'Innhold'}
            icon={<PencilIcon fontSize="1.5rem" aria-hidden />}
          />
          {vedlegg?.length > 0 && (
            <Tabs.Tab
              value={ManueltBrevFane.REDIGER_VEDLEGG}
              label={kanRedigeres ? 'Rediger vedlegg' : 'Innhold vedlegg'}
              icon={<PencilIcon fontSize="1.5rem" aria-hidden />}
            />
          )}
          <Tabs.Tab
            value={ManueltBrevFane.FORHAANDSVIS}
            label="Forhåndsvisning"
            icon={<FilePdfIcon fontSize="1.5rem" aria-hidden />}
          />
        </Tabs.List>

        <Tabs.Panel value={ManueltBrevFane.REDIGER}>
          {(isPendingOrInitial(hentManuellPayloadStatus) || isPending(tilbakestillManuellPayloadStatus)) && (
            <Spinner label="Henter brevinnhold ..." />
          )}
          {isSuccess(hentManuellPayloadStatus) && isSuccessOrInitial(tilbakestillManuellPayloadStatus) && (
            <>
              <SlateEditor value={content} onChange={onChange} readonly={!kanRedigeres} />

              {kanRedigeres && (
                <TilbakestillOgLagreRad
                  lagretStatus={lagretStatus}
                  lagre={lagre}
                  tilbakestill={tilbakestill}
                  tilbakestillManuellPayloadStatus={isPending(tilbakestillManuellPayloadStatus)}
                  lagreManuellPayloadStatus={isPending(lagreManuellPayloadStatus)}
                  tilbakestillSynlig={!!brev.behandlingId}
                />
              )}
            </>
          )}
          {isFailureHandler({
            apiResult: tilbakestillManuellPayloadStatus,
            errorMessage: 'Det skjedde en feil ved tilbakestillting av brev',
          })}
        </Tabs.Panel>

        <Tabs.Panel value={ManueltBrevFane.REDIGER_VEDLEGG}>
          {isPendingOrInitial(hentManuellPayloadStatus) ||
            (isPending(tilbakestillManuellPayloadStatus) && <Spinner label="Henter brevinnhold ..." />)}
          {isSuccess(hentManuellPayloadStatus) && isSuccessOrInitial(tilbakestillManuellPayloadStatus) && (
            <>
              <Accordion indent={false}>
                {vedlegg?.length > 0 &&
                  vedlegg.map((brevVedlegg) => (
                    <Accordion.Item key={brevVedlegg.key}>
                      <Accordion.Header>{brevVedlegg.tittel}</Accordion.Header>

                      <Accordion.Content>
                        <VStack gap="space-4" paddingBlock="space-4" paddingInline="space-0">
                          <VedleggInfo vedleggTittel={brevVedlegg.tittel} />
                          <SlateEditor
                            value={brevVedlegg.payload}
                            onChange={onChangeVedlegg}
                            readonly={!kanRedigeres}
                            editKey={brevVedlegg.key}
                          />
                        </VStack>
                      </Accordion.Content>
                    </Accordion.Item>
                  ))}
              </Accordion>

              {kanRedigeres && (
                <TilbakestillOgLagreRad
                  lagretStatus={lagretStatus}
                  lagre={lagre}
                  tilbakestill={tilbakestill}
                  tilbakestillManuellPayloadStatus={isPending(tilbakestillManuellPayloadStatus)}
                  lagreManuellPayloadStatus={isPending(lagreManuellPayloadStatus)}
                  tilbakestillSynlig={!!brev.behandlingId}
                />
              )}
            </>
          )}
          {isFailureHandler({
            apiResult: tilbakestillManuellPayloadStatus,
            errorMessage: 'Det skjedde en feil ved tilbakestilling av brev',
          })}
        </Tabs.Panel>

        <Tabs.Panel value={ManueltBrevFane.FORHAANDSVIS}>
          <ForhaandsvisningBrev skalGaaViaBehandling={skalGaaViaBehandling} brev={brev} />
        </Tabs.Panel>
      </Tabs>
    </Container>
  )
}

const Container = styled.div`
  width: 100%;
`
