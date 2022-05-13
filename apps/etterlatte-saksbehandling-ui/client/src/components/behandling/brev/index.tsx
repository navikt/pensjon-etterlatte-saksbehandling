import { Content, ContentHeader } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { Button, ContentContainer, Heading, Table, Tag } from "@navikt/ds-react";
import BrevModal from "./brev-modal";
import { Information, Success } from '@navikt/ds-icons'
import NyttBrev from "./nytt-brev";
import { Border, HeadingWrapper } from "../soeknadsoversikt/styled";
import { BehandlingsStatusSmall, IBehandlingsStatus } from "../behandlings-status";
import { BehandlingsTypeSmall, IBehandlingsType } from "../behandlings-type";
import { Soeknadsdato } from "../soeknadsoversikt/soeknadoversikt/soeknadinfo/Soeknadsdato";
import { usePersonInfoFromBehandling } from "../usePersonInfoFromBehandling";
import { BehandlingHandlingKnapper } from "../handlinger/BehandlingHandlingKnapper";
import { hentAlleBrev } from "../../../shared/api/brev";
import { useParams } from "react-router-dom";

export const Brev = () => {
  const { behandlingId } = useParams()
  const { mottattDato } = usePersonInfoFromBehandling()

  const [brevListe, setBrevListe] = useState<any[]>()

  useEffect(() => {
    hentAlleBrev(behandlingId!!)
        .then(res => setBrevListe(res))
  }, [])

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
                <Table.HeaderCell>Fødselsnummer</Table.HeaderCell>
                {/* TODO: Burde vi vise hvilken rolle mottakeren har? Søker, innsender, etc..? */}
                {/*<Table.HeaderCell>Rolle</Table.HeaderCell>*/}
                <Table.HeaderCell>Status</Table.HeaderCell>
                <Table.HeaderCell>Handlinger</Table.HeaderCell>
              </Table.Row>
            </Table.Header>

            <Table.Body>
              {brevListe?.map((brev, i) => (
                  <Table.Row key={i}>
                    <Table.DataCell>{brev.id}</Table.DataCell>
                    <Table.DataCell>Vedtak om innvilget barnepensjon</Table.DataCell>
                    <Table.DataCell>
                      {brev.mottaker.fornavn} {brev.mottaker.etternavn}
                    </Table.DataCell>
                    <Table.DataCell>
                      {brev.mottaker.foedselsnummer}
                    </Table.DataCell>
                    <Table.DataCell>
                      {hentStatusTag(brev.status)}
                    </Table.DataCell>
                    <Table.DataCell>
                      <BrevModal brevId={brev.id} status={brev.status}/>
                    </Table.DataCell>
                  </Table.Row>
              ))}
            </Table.Body>
          </Table>
        </ContentContainer>

        <ContentContainer>
          <NyttBrev/>
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
