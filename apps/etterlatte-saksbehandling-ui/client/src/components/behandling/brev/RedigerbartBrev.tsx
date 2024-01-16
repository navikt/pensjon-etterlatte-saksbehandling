import { Accordion, Tabs } from '@navikt/ds-react'
import SlateEditor from '~components/behandling/brev/SlateEditor'
import React, { useEffect, useState } from 'react'
import { FilePdfIcon, PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { IBrev } from '~shared/types/Brev'
import { format } from 'date-fns'
import { hentManuellPayload, lagreManuellPayload, tilbakestillManuellPayload } from '~shared/api/brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { isPending, isPendingOrInitial, isSuccess, isSuccessOrInitial } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { TilbakestillOgLagreRad } from '~components/behandling/brev/TilbakestillOgLagreRad'

enum ManueltBrevFane {
  REDIGER = 'REDIGER',
  REDIGER_VEDLEGG = 'REDIGER VEDLEGG',
  FORHAANDSVIS = 'FORHAANDSVIS',
}

export interface LagretStatus {
  lagret: boolean
  beskrivelse?: string
}

const formaterTidspunkt = (dato: Date) => format(new Date(dato), 'HH:mm:ss').toString()

interface RedigerbartBrevProps {
  brev: IBrev
  kanRedigeres: boolean
  lukkAdvarselBehandlingEndret?: () => void
}

export default function RedigerbartBrev({ brev, kanRedigeres, lukkAdvarselBehandlingEndret }: RedigerbartBrevProps) {
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
      setLagretStatus({ lagret: true, beskrivelse: `Lagret kl. ${formaterTidspunkt(new Date())}` })
      if (lukkAdvarselBehandlingEndret) lukkAdvarselBehandlingEndret()
    })
  }

  const tilbakestill = () => {
    apiTilbakestillManuellPayload(
      { brevId: brev.id, sakId: brev.sakId, behandlingId: brev.behandlingId },
      (payload: any) => {
        setContent(payload.hoveddel)
        setVedlegg(payload.vedlegg)
        setLagretStatus({ lagret: true, beskrivelse: `Lagret kl. ${formaterTidspunkt(new Date())}` })
        if (lukkAdvarselBehandlingEndret) lukkAdvarselBehandlingEndret()
      }
    )
  }

  const onChange = (value: any[]) => {
    setLagretStatus({
      lagret: false,
      beskrivelse: `Sist endret kl. ${formaterTidspunkt(new Date())} (ikke lagret)`,
    })
    setContent(value)
  }

  const onChangeVedlegg = (value: any[], key?: string) => {
    if (!key) return
    setLagretStatus({
      lagret: false,
      beskrivelse: `Sist endret kl. ${formaterTidspunkt(new Date())} (ikke lagret)`,
    })
    const oppdatertVedlegg = vedlegg.map((ved) => {
      if (ved.key === key) return { ...ved, payload: value }
      return ved
    })
    setVedlegg(oppdatertVedlegg)
  }

  return (
    <Container forhaandsvisning={fane === ManueltBrevFane.FORHAANDSVIS}>
      <Tabs value={fane} onChange={setFane}>
        <Tabs.List>
          <Tabs.Tab
            value={ManueltBrevFane.REDIGER}
            label={kanRedigeres ? 'Rediger' : 'Innhold'}
            icon={<PencilIcon fontSize="1.5rem" aria-hidden />}
          />
          {vedlegg && (
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
            <Spinner visible label="Henter brevinnhold ..." />
          )}
          {isSuccess(hentManuellPayloadStatus) && isSuccessOrInitial(tilbakestillManuellPayloadStatus) && (
            <PanelWrapper>
              <SlateEditor value={content} onChange={onChange} readonly={!kanRedigeres} />

              {kanRedigeres && (
                <TilbakestillOgLagreRad
                  lagretStatus={lagretStatus}
                  lagre={lagre}
                  tilbakestill={tilbakestill}
                  tilbakestillManuellPayloadStatus={isPending(tilbakestillManuellPayloadStatus)}
                  lagreManuellPayloadStatus={isPending(lagreManuellPayloadStatus)}
                  tilbakeStillSynlig={brev.behandlingId !== null}
                />
              )}
            </PanelWrapper>
          )}
          {isFailureHandler({
            apiResult: tilbakestillManuellPayloadStatus,
            errorMessage: 'Det skjedde en feil ved tilbakestillting av brev',
          })}
        </Tabs.Panel>

        <Tabs.Panel value={ManueltBrevFane.REDIGER_VEDLEGG}>
          {isPendingOrInitial(hentManuellPayloadStatus) ||
            (isPending(tilbakestillManuellPayloadStatus) && <Spinner visible label="Henter brevinnhold ..." />)}
          {isSuccess(hentManuellPayloadStatus) && isSuccessOrInitial(tilbakestillManuellPayloadStatus) && (
            <>
              <PanelWrapper>
                <Accordion>
                  {vedlegg &&
                    vedlegg.map((brevVedlegg) => (
                      <Accordion.Item key={brevVedlegg.key}>
                        <Accordion.Header>{brevVedlegg.tittel}</Accordion.Header>
                        <Accordion.Content>
                          <SlateEditor
                            value={brevVedlegg.payload}
                            onChange={onChangeVedlegg}
                            readonly={!kanRedigeres}
                            editKey={brevVedlegg.key}
                          />
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
                    tilbakeStillSynlig={brev.behandlingId !== null}
                  />
                )}
              </PanelWrapper>
            </>
          )}
          {isFailureHandler({
            apiResult: tilbakestillManuellPayloadStatus,
            errorMessage: 'Det skjedde en feil ved tilbakestilling av brev',
          })}
        </Tabs.Panel>

        <Tabs.Panel value={ManueltBrevFane.FORHAANDSVIS}>
          <PanelWrapper forhaandsvisning>
            <ForhaandsvisningBrev brev={brev} />
          </PanelWrapper>
        </Tabs.Panel>
      </Tabs>
    </Container>
  )
}

interface StyledProps {
  forhaandsvisning?: boolean
}

const Container = styled.div<StyledProps>`
  margin: auto;
  height: 100%;
  width: 100%;
  position: relative;
  .navds-tabs,
  .navds-tabs__tabpanel {
    height: inherit;
    width: inherit;
    max-height: ${(p) => (p.forhaandsvisning ? '75vh' : 'calc(75vh - 8rem)')};
  }
`

const PanelWrapper = styled.div<StyledProps>`
  height: 100%;
  width: 100%;
  max-height: ${(p) => (p.forhaandsvisning ? 'calc(75vh - 3rem)' : 'calc(75vh - 5rem)')};
`
