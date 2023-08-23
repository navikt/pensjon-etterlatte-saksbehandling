import { Accordion, Button, Detail, Tabs } from '@navikt/ds-react'
import SlateEditor from '~components/behandling/brev/SlateEditor'
import { useEffect, useState } from 'react'
import { FilePdfIcon, FloppydiskIcon, PencilIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { IBrev } from '~shared/types/Brev'
import { format } from 'date-fns'
import { hentManuellPayload, lagreManuellPayload } from '~shared/api/brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import { isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'

enum ManueltBrevFane {
  REDIGER = 'REDIGER',
  REDIGER_VEDLEGG = 'REDIGER VEDLEGG',
  FORHAANDSVIS = 'FORHAANDSVIS',
}

interface LagretStatus {
  lagret: boolean
  beskrivelse?: string
}

const formaterTidspunkt = (dato: Date) => format(new Date(dato), 'HH:mm:ss').toString()

interface RedigerbartBrevProps {
  brev: IBrev
  kanRedigeres: boolean
}

export default function RedigerbartBrev({ brev, kanRedigeres }: RedigerbartBrevProps) {
  const [fane, setFane] = useState<string>(kanRedigeres ? ManueltBrevFane.REDIGER : ManueltBrevFane.FORHAANDSVIS)
  const [content, setContent] = useState<any[]>([])
  const [vedlegg, setVedlegg] = useState<any[]>([])
  const [lagretStatus, setLagretStatus] = useState<LagretStatus>({ lagret: false })

  const [hentManuellPayloadStatus, apiHentManuellPayload] = useApiCall(hentManuellPayload)
  const [lagreManuellPayloadStatus, apiLagreManuellPayload] = useApiCall(lagreManuellPayload)

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
    })
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
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
          />
          {vedlegg && (
            <Tabs.Tab
              value={ManueltBrevFane.REDIGER_VEDLEGG}
              label={kanRedigeres ? 'Rediger vedlegg' : 'Innhold'}
              icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            />
          )}
          <Tabs.Tab
            value={ManueltBrevFane.FORHAANDSVIS}
            label={'Forhåndsvisning'}
            icon={<FilePdfIcon title="a11y-title" fontSize="1.5rem" />}
          />
        </Tabs.List>

        <Tabs.Panel value={ManueltBrevFane.REDIGER}>
          {isPendingOrInitial(hentManuellPayloadStatus) && <Spinner visible label={'Henter brevinnhold ...'} />}
          {isSuccess(hentManuellPayloadStatus) && (
            <PanelWrapper>
              <SlateEditor value={content} onChange={onChange} readonly={!kanRedigeres} />

              {kanRedigeres && (
                <ButtonRow>
                  {lagretStatus.beskrivelse && <Detail as="span">{lagretStatus.beskrivelse}</Detail>}
                  <Button
                    icon={<FloppydiskIcon title="a11y-title" />}
                    variant={'secondary'}
                    onClick={lagre}
                    disabled={!lagretStatus}
                    loading={isPending(lagreManuellPayloadStatus)}
                  >
                    Lagre endringer
                  </Button>
                </ButtonRow>
              )}
            </PanelWrapper>
          )}
        </Tabs.Panel>

        <Tabs.Panel value={ManueltBrevFane.REDIGER_VEDLEGG}>
          {isPendingOrInitial(hentManuellPayloadStatus) && <Spinner visible label={'Henter brevinnhold ...'} />}
          {isSuccess(hentManuellPayloadStatus) && (
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
                  <ButtonRow>
                    {lagretStatus.beskrivelse && <Detail as="span">{lagretStatus.beskrivelse}</Detail>}
                    <Button
                      icon={<FloppydiskIcon title="a11y-title" />}
                      variant={'secondary'}
                      onClick={lagre}
                      disabled={!lagretStatus}
                      loading={isPending(lagreManuellPayloadStatus)}
                    >
                      Lagre endringer
                    </Button>
                  </ButtonRow>
                )}
              </PanelWrapper>
            </>
          )}
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

const ButtonRow = styled.div`
  padding: 1rem;
  text-align: right;
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100%;
  z-index: 9999;

  & > button {
    margin-left: 10px;
  }
`

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
  overflow: auto;
`
