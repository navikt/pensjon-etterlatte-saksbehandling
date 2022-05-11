import { Content, ContentHeader } from '../../../shared/styled'
import { useEffect, useState } from 'react'
import { Button, ContentContainer, Heading, Table, Tag } from "@navikt/ds-react";
import { OpplysningsType } from "../../../store/reducers/BehandlingReducer";
import BrevModal from "./brev-modal";
import { Information } from '@navikt/ds-icons'
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
  // const { state } = useContext(AppContext)

  // const gyldigeTyper = [OpplysningsType.innsender, OpplysningsType.soeker_pdl]

  // const grunnlagListe: IBehandlingsopplysning[] = state.behandlingReducer.grunnlag
  //     .filter(grunnlag => gyldigeTyper.includes(grunnlag.opplysningType))

  const [brevListe, setBrevListe] = useState<any[]>()

  const type = (opplysningType: OpplysningsType): string => {
    switch (opplysningType) {
      case OpplysningsType.innsender:
        return 'Innsender'
      case OpplysningsType.gjenlevende_forelder_pdl:
        return 'Forelder'
      case OpplysningsType.soeker_pdl:
        return 'Søker'
      default:
        return ''
    }
  }

  useEffect(() => {
    hentAlleBrev(behandlingId!!)
        .then(res => setBrevListe(res))
  }, [])

  return (
      <Content>
        <ContentHeader>
          <HeadingWrapper>
            <Heading spacing size={'xlarge'} level={'5'}>
              Brev
            </Heading>
            <div className="details">
              <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
              <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
            </div>
          </HeadingWrapper>
          <Soeknadsdato mottattDato={mottattDato} />
        </ContentHeader>

        <ContentContainer>
              <Table>
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell>ID</Table.HeaderCell>
                    <Table.HeaderCell>Filnavn</Table.HeaderCell>
                    <Table.HeaderCell>Mottaker navn</Table.HeaderCell>
                    <Table.HeaderCell>Fødselsnummer</Table.HeaderCell>
                    <Table.HeaderCell>Rolle</Table.HeaderCell>
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
                          {/*{type(grunnlag.opplysningType)}*/}
                          UKJENT
                        </Table.DataCell>
                        <Table.DataCell>
                          <Tag variant="warning" size={'small'} style={{width: '100%'}}>
                            Ikke sendt &nbsp;<Information />
                          </Tag>
                        </Table.DataCell>
                        <Table.DataCell>
                          <BrevModal brevId={brev.id}/>
                        </Table.DataCell>
                      </Table.Row>
                  ))}
                </Table.Body>
              </Table>
        </ContentContainer>

        <ContentContainer>
          <NyttBrev />
        </ContentContainer>

        <Border />

        <BehandlingHandlingKnapper>
          <Button variant={'primary'}>
            Fullfør behandling
          </Button>
        </BehandlingHandlingKnapper>
      </Content>
  )
}
