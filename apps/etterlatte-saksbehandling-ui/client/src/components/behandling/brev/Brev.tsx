import { Content, ContentHeader } from '~shared/styled'
import { useEffect, useState } from 'react'
import { Alert, ContentContainer, Heading, Table, Tag } from '@navikt/ds-react'
import BrevModal from './brev-modal'
import { Information, Success } from '@navikt/ds-icons'
import NyttBrev from './nytt-brev/nytt-brev'
import { Border, HeadingWrapper } from '../soeknadsoversikt/styled'
import { ISaksType } from '../fargetags/saksType'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { ferdigstillBrev, hentBrevForBehandling, Mottaker, slettBrev } from '~shared/api/brev'
import { useParams } from 'react-router-dom'
import { Soeknadsdato } from '../soeknadsoversikt/soeknadoversikt/Soeknadsdato'
import { Journalpost } from '../types'
import { formaterBehandlingstype, formaterDato, formaterSakstype } from '~utils/formattering'
import InnkommendeBrevModal from './innkommende-brev-modal'
import styled from 'styled-components'
import Spinner from '~shared/Spinner'
import LastOppBrev from './nytt-brev/last-opp'
import { SendTilAttesteringModal } from '../handlinger/sendTilAttesteringModal'
import { hentDokumenter } from '~shared/api/dokument'
import { tagColors, TagList } from '~shared/Tags'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

const IngenInnkommendeBrevRad = styled.td`
  text-align: center;
  padding-top: 16px;
  font-style: italic;
`

export interface IBrev {
  id: number
  behandlingId: string
  tittel: string
  status: string
  mottaker: Mottaker
  erVedtaksbrev: boolean
}

export const Brev = (props: { behandling: IDetaljertBehandling }) => {
  const { soeknadMottattDato, behandlingType } = props.behandling
  const { behandlingId, fnr } = useParams()
  const [brevListe, setBrevListe] = useState<IBrev[]>([])
  const [innkommendeBrevListe, setInnkommendeBrevListe] = useState<Journalpost[]>([])
  const [error, setError] = useState(false)
  const [innkommendeError, setInnkommendeError] = useState(false)
  const [innkommendeHentet, setInnkommendeHentet] = useState(false)

  useEffect(() => {
    hentBrevForBehandling(behandlingId!!)
      .then((res) => {
        if (res.status === 'ok') setBrevListe(res.data)
        else throw Error(res.error)
      })
      .catch(() => setError(true))

    if (!fnr) return

    hentDokumenter(fnr)
      .then((res) => {
        if (res.status === 'ok') setInnkommendeBrevListe(res.data)
        else throw Error(res.error)
      })
      .catch(() => setInnkommendeError(true))
      .finally(() => setInnkommendeHentet(true))
  }, [])

  const ferdigstill = (brevId: number): Promise<void> => {
    return ferdigstillBrev(String(brevId)).then((res) => {
      if (res.status === 'ok') {
        const nyListe: IBrev[] = brevListe.filter((v: IBrev) => v.id !== brevId)
        nyListe.push(res.data)
        setBrevListe(nyListe)
      } else {
        setError(res.error)
      }
    })
  }

  const leggTilNytt = (brev: IBrev) => {
    const nyListe = [...brevListe]
    nyListe.push(brev)
    setBrevListe(nyListe)
  }
  const slett = (brevId: number): Promise<void> => {
    return slettBrev(String(brevId)).then(() => {
      const nyListe = brevListe?.filter((brev) => brev.id !== brevId)

      setBrevListe(nyListe)
    })
  }

  const hentStatusTag = (status: string) => {
    if (['OPPRETTET', 'OPPDATERT'].includes(status)) {
      return (
        <Tag variant={'warning'} size={'small'} style={{ width: '100%' }}>
          Ikke sendt &nbsp;
          <Information />
        </Tag>
      )
    } else if (status === 'FERDIGSTILT') {
      return (
        <Tag variant={'info'} size={'small'} style={{ width: '100%' }}>
          Ferdigstilt &nbsp;
          <Information />
        </Tag>
      )
    } else if (status === 'JOURNALFOERT') {
      return (
        <Tag variant={'success'} size={'small'} style={{ width: '100%' }}>
          Journalført &nbsp;
          <Success />
        </Tag>
      )
    } else if (status === 'DISTRIBUERT') {
      return (
        <Tag variant={'success'} size={'small'} style={{ width: '100%' }}>
          Distribuert &nbsp;
          <Success />
        </Tag>
      )
    } else {
      return (
        <Tag variant={'error'} size={'small'} style={{ width: '100%' }}>
          Slettet &nbsp;
          <Information />
        </Tag>
      )
    }
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size={'xlarge'} level={'5'}>
            Brev-oversikt
          </Heading>
          <div className="details">
            <TagList>
              <li>
                <Tag variant={tagColors[behandlingType]} size={'small'}>
                  {formaterBehandlingstype(behandlingType)}
                </Tag>
              </li>
              <li>
                <Tag variant={tagColors[ISaksType.BARNEPENSJON]} size={'small'}>
                  {formaterSakstype(ISaksType.BARNEPENSJON)}
                </Tag>
              </li>
            </TagList>
          </div>
        </HeadingWrapper>
        {soeknadMottattDato && <Soeknadsdato mottattDato={soeknadMottattDato} />}
      </ContentHeader>

      <ContentContainer>
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>ID</Table.HeaderCell>
              <Table.HeaderCell>Filnavn</Table.HeaderCell>
              <Table.HeaderCell>Mottaker navn</Table.HeaderCell>
              {/* TODO: Burde vi vise hvilken rolle mottakeren har? Søker, innsender, etc..? */}
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Handlinger</Table.HeaderCell>
            </Table.Row>
          </Table.Header>

          <Table.Body>
            {brevListe?.map((brev, i) => (
              <Table.Row key={i}>
                <Table.DataCell>{brev.id}</Table.DataCell>
                <Table.DataCell>{brev.tittel}</Table.DataCell>
                <Table.DataCell>
                  {/*{brev.mottaker.fornavn} {brev.mottaker.etternavn}*/}- Kommer navn her -
                </Table.DataCell>
                <Table.DataCell>{hentStatusTag(brev.status)}</Table.DataCell>
                <Table.DataCell>
                  <BrevModal brev={brev} ferdigstill={ferdigstill} slett={slett} />
                </Table.DataCell>
              </Table.Row>
            ))}

            {brevListe.length === 0 && !error && (
              <Table.Row>
                <IngenInnkommendeBrevRad colSpan={5}>Ingen brev er opprettet</IngenInnkommendeBrevRad>
              </Table.Row>
            )}
          </Table.Body>
        </Table>

        {error && (
          <Alert variant={'error'} style={{ marginTop: '10px' }}>
            Det har oppstått en feil...
          </Alert>
        )}
      </ContentContainer>

      <ContentContainer>
        <NyttBrev leggTilNytt={leggTilNytt} />
        <LastOppBrev leggTilNytt={leggTilNytt} />
      </ContentContainer>

      <Border />
      <ContentHeader>
        <Heading spacing size={'large'} level={'5'}>
          Innkommende brev
        </Heading>
      </ContentHeader>
      <ContentContainer>
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>ID</Table.HeaderCell>
              <Table.HeaderCell>Filnavn</Table.HeaderCell>
              <Table.HeaderCell>Avsender</Table.HeaderCell>
              <Table.HeaderCell>Mottatt</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Handlinger</Table.HeaderCell>
            </Table.Row>
          </Table.Header>

          <Table.Body>
            {innkommendeBrevListe?.map((brev, i) => (
              <Table.Row key={i}>
                <Table.DataCell>{brev.journalpostId}</Table.DataCell>
                <Table.DataCell>{brev.tittel}</Table.DataCell>
                <Table.DataCell>{brev.avsenderMottaker.navn}</Table.DataCell>
                <Table.DataCell>{formaterDato(new Date(brev.datoOpprettet))}</Table.DataCell>
                <Table.DataCell>{brev.journalstatus}</Table.DataCell>
                <Table.DataCell>
                  <InnkommendeBrevModal
                    tittel={brev.tittel}
                    journalpostId={brev.journalpostId}
                    dokumentInfoId={brev.dokumenter[0].dokumentInfoId}
                  />
                </Table.DataCell>
              </Table.Row>
            ))}

            {innkommendeBrevListe.length === 0 && !innkommendeError && (
              <Table.Row>
                <IngenInnkommendeBrevRad colSpan={6}>
                  {innkommendeHentet ? (
                    'Ingen innkommende brev ble funnet'
                  ) : (
                    <Spinner visible={!innkommendeHentet} label="Henter innkommende brev" />
                  )}
                </IngenInnkommendeBrevRad>
              </Table.Row>
            )}
          </Table.Body>
        </Table>

        {innkommendeError && (
          <Alert variant={'error'} style={{ marginTop: '10px' }}>
            Det har oppstått en feil ved henting av innkommende brev...
          </Alert>
        )}
      </ContentContainer>
      <Border />
      <BehandlingHandlingKnapper>
        <SendTilAttesteringModal />
      </BehandlingHandlingKnapper>
    </Content>
  )
}
