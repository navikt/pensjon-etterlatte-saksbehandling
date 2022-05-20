import { Content, ContentHeader } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { Alert, Button, ContentContainer, Heading, Table, Tag } from "@navikt/ds-react";
import BrevModal from "./brev-modal";
import { Information, Success } from '@navikt/ds-icons'
import NyttBrev from "./nytt-brev";
import { Border, HeadingWrapper } from "../soeknadsoversikt/styled";
import { BehandlingsStatusSmall, IBehandlingsStatus } from "../behandlings-status";
import { BehandlingsTypeSmall, IBehandlingsType } from "../behandlings-type";
import { Soeknadsdato } from "../soeknadsoversikt/soeknadoversikt/soeknadinfo/Soeknadsdato";
import { usePersonInfoFromBehandling } from "../usePersonInfoFromBehandling";
import { BehandlingHandlingKnapper } from "../handlinger/BehandlingHandlingKnapper";
import { ferdigstillBrev, hentBrevForBehandling, slettBrev } from "../../../shared/api/brev";
import { useParams } from "react-router-dom";

export const Brev = () => {
  const { behandlingId } = useParams()
  const { mottattDato } = usePersonInfoFromBehandling()

  const [brevListe, setBrevListe] = useState<any[]>([])
  const [error, setError] = useState(false)

  useEffect(() => {
    hentBrevForBehandling(behandlingId!!)
        .then(res => setBrevListe(res))
        .catch(() => setError(true))
  }, [])

  const ferdigstill = (brevId: any): Promise<void> => {
    return ferdigstillBrev(brevId)
        .then((brev: any) => {
          const nyListe: any[] = brevListe.filter((v: any) => v.id !== brevId)

          nyListe.push(brev)

          setBrevListe(nyListe)
        })
  }

  const leggTilNytt = (brev: any) => {
    const nyListe = [...brevListe]
    nyListe.push(brev)
    setBrevListe(nyListe)
  }

  const slett = (brevId: any): Promise<void> => {
    return slettBrev(brevId)
        .then(() => {
          const nyListe = brevListe?.filter(brev => brev.id !== brevId)

          setBrevListe(nyListe)
        })
  }

  const hentStatusTag = (status: string) => {
    if (['OPPRETTET', 'OPPDATERT'].includes(status)) {
      return (
          <Tag variant={'warning'} size={'small'} style={{ width: '100%' }}>
            Ikke sendt &nbsp;<Information/>
          </Tag>
      )
    } else if (status === 'FERDIGSTILT') {
      return (
          <Tag variant={'info'} size={'small'} style={{ width: '100%' }}>
            Ferdigstilt &nbsp;<Information/>
          </Tag>
      )
    } else if (status === 'SENDT') {
      return (
          <Tag variant={'success'} size={'small'} style={{ width: '100%' }}>
            Sendt &nbsp;<Success/>
          </Tag>
      )
    } else {
      return (
          <Tag variant={'error'} size={'small'} style={{ width: '100%' }}>
            Slettet &nbsp;<Information/>
          </Tag>
      )
    }
  }

  return (
      <Content>
        <ContentHeader>
          <HeadingWrapper>
            <Heading spacing size={'xlarge'} level={'5'}>
              Brev
            </Heading>
            <div className="details">
              <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG}/>
              <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON}/>
            </div>
          </HeadingWrapper>
          <Soeknadsdato mottattDato={mottattDato}/>
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
                      {brev.mottaker.fornavn} {brev.mottaker.etternavn}
                    </Table.DataCell>
                    <Table.DataCell>
                      {hentStatusTag(brev.status)}
                    </Table.DataCell>
                    <Table.DataCell>
                      <BrevModal
                          brev={brev}
                          ferdigstill={ferdigstill}
                          slett={slett}
                      />
                    </Table.DataCell>
                  </Table.Row>
              ))}
            </Table.Body>
          </Table>

          {error && (
              <Alert variant={'error'} style={{ marginTop: '10px'}}>
                Det har oppstått en feil...
              </Alert>
          )}
        </ContentContainer>

        <ContentContainer>
          <NyttBrev leggTilNytt={leggTilNytt}/>
        </ContentContainer>

        <Border/>

        <BehandlingHandlingKnapper>
          <Button variant={'primary'} disabled={true}>
            Fullfør behandling
          </Button>
        </BehandlingHandlingKnapper>
      </Content>
  )
}
