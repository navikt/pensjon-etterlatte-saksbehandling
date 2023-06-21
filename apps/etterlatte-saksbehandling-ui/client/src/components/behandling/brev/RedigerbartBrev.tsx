import { Button, Detail, Tabs } from '@navikt/ds-react'
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
  const [lagretStatus, setLagretStatus] = useState<LagretStatus>({ lagret: false })

  const [hentManuellPayloadStatus, apiHentManuellPayload] = useApiCall(hentManuellPayload)
  const [lagreManuellPayloadStatus, apiLagreManuellPayload] = useApiCall(lagreManuellPayload)

  useEffect(() => {
    if (!brev.id) return

    apiHentManuellPayload({ brevId: brev.id, sakId: brev.sakId }, (payload: any) => {
      setContent(payload)
    })
  }, [brev.id])

  const lagre = () => {
    apiLagreManuellPayload({ brevId: brev.id, sakId: brev.sakId, payload: content }, () => {
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

  return (
    <Container>
      <Tabs value={fane} onChange={setFane}>
        <Tabs.List>
          <Tabs.Tab
            value={ManueltBrevFane.REDIGER}
            label={kanRedigeres ? 'Rediger' : 'Innhold'}
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
          />

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

        <Tabs.Panel value={ManueltBrevFane.FORHAANDSVIS}>
          <PanelWrapper>
            <ForhaandsvisningBrev brev={brev} />
          </PanelWrapper>
        </Tabs.Panel>
      </Tabs>
    </Container>
  )
}

const ButtonRow = styled.div`
  margin: 1rem;
  text-align: right;

  & > button {
    margin-left: 10px;
  }
`

const Container = styled.div`
  margin: auto;
  height: 100%;
  width: 100%;

  .navds-tabs,
  .navds-tabs__tabpanel {
    height: inherit;
    width: inherit;
  }
`

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`
