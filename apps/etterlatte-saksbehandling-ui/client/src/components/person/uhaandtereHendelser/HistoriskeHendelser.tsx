import { BodyShort, Button, Detail, Heading, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import React, { useEffect, useState } from 'react'
import { CheckmarkCircleIcon, ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import {
  Grunnlagsendringshendelse,
  GrunnlagsendringsType,
  HISTORISK_REVURDERING,
  STATUS_IRRELEVANT,
} from '~components/person/typer'
import { formaterDatoMedTidspunkt, formaterStringDato } from '~utils/formattering'
import { JaNei } from '~shared/types/ISvar'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentInstitusjonsoppholdData } from '~shared/api/behandling'
import { VilkaarVurdertInformasjon } from '~components/behandling/vilkaarsvurdering/Vurdering'
import Spinner from '~shared/Spinner'
import { AGreen500 } from '@navikt/ds-tokens/dist/tokens'

import { mapApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~shared/error/ApiErrorAlert'

type Props = {
  hendelser: Grunnlagsendringshendelse[]
}
const HistoriskeHendelser = (props: Props) => {
  const [aapenhistorikk, setLastetBehandlingliste] = useState<boolean>(false)

  const lukkedeHendelser = props.hendelser.filter((h) => h.status === STATUS_IRRELEVANT)
  const historiskRevurderingsHendelse = props.hendelser.filter((h) => h.status === HISTORISK_REVURDERING)

  const historiskeHendelser = lukkedeHendelser.concat(historiskRevurderingsHendelse)

  return (
    <HistoriskeHendelserWrapper>
      <Heading spacing size="medium" level="2">
        Tidligere hendelser
      </Heading>

      {!historiskeHendelser.length ? (
        <BodyShort>
          <i>Ingen historiske hendelser</i>
        </BodyShort>
      ) : (
        <div>
          <Button variant="tertiary" onClick={() => setLastetBehandlingliste(!aapenhistorikk)}>
            <MarginRightChevron>
              {aapenhistorikk ? <ChevronUpIcon fontSize="1.5rem" /> : <ChevronDownIcon fontSize="1.5rem" />}
            </MarginRightChevron>
            Vis historikk
          </Button>
          {aapenhistorikk && (
            <Table>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope="col">Sakid</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                  <Table.HeaderCell scope="col">GjelderPerson</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Opprettet</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Kommentar</Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {historiskeHendelser.map(({ sakId, type, gjelderPerson, id, opprettet, kommentar }) => (
                  <Table.Row key={id}>
                    <Table.HeaderCell>{sakId}</Table.HeaderCell>
                    <Table.DataCell>{type}</Table.DataCell>
                    <Table.DataCell>{gjelderPerson}</Table.DataCell>
                    <Table.DataCell>{formaterStringDato(opprettet)}</Table.DataCell>
                    <Table.DataCell>
                      {type !== GrunnlagsendringsType.INSTITUSJONSOPPHOLD &&
                        (kommentar ? kommentar : 'Ingen kommentar')}
                      {type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD && <InstitusjonsoppholdBegrunnelse id={id} />}
                    </Table.DataCell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table>
          )}
        </div>
      )}
    </HistoriskeHendelserWrapper>
  )
}

const InstitusjonsoppholdBegrunnelse = ({ id }: { id: string }) => {
  const [begrunnelse, hentInstbegrunnelseData] = useApiCall(hentInstitusjonsoppholdData)

  useEffect(() => {
    hentInstbegrunnelseData(id)
  }, [])

  return (
    <>
      {mapApiResult(
        begrunnelse,
        <Spinner visible={true} label="Henter vurdering" />,
        () => (
          <ApiErrorAlert>Fant ingen begrunnelse for hendelsen</ApiErrorAlert>
        ),
        (begrunnelse) => (
          <div>
            <Heading spacing size="small" level="3">
              Institusjonsoppholdshendelsen er vurdert
              <CheckmarkCircleIcon color={AGreen500} aria-hidden="true" />
            </Heading>
            <p>
              Er dette en institusjon som kan gi reduksjon av ytelsen? - <b>{begrunnelse.kanGiReduksjonAvYtelse}</b>
            </p>
            <p>Kommentar - {begrunnelse.kanGiReduksjonAvYtelseBegrunnelse}</p>
            <p>
              Er oppholdet forventet å vare lenger enn innleggelsesmåned + tre måneder? -
              <b>{begrunnelse.forventetVarighetMerEnn3Maaneder}</b>
            </p>
            <p>Kommentar - {begrunnelse.forventetVarighetMerEnn3MaanederBegrunnelse}</p>
            <VilkaarVurdertInformasjon>
              <Detail>Manuelt av {begrunnelse.saksbehandler.ident}</Detail>
              <Detail>
                Dato{' '}
                {begrunnelse.saksbehandler.tidspunkt
                  ? formaterDatoMedTidspunkt(new Date(begrunnelse.saksbehandler.tidspunkt))
                  : '-'}
              </Detail>
            </VilkaarVurdertInformasjon>
          </div>
        )
      )}
    </>
  )
}

export interface InstitusjonsoppholdMedKilde {
  kanGiReduksjonAvYtelse: JaNei
  kanGiReduksjonAvYtelseBegrunnelse: string
  forventetVarighetMerEnn3Maaneder: JaNei
  forventetVarighetMerEnn3MaanederBegrunnelse: string
  saksbehandler: KildeSaksbehandler
}

const HistoriskeHendelserWrapper = styled.div`
  margin-top: 3rem;
`

const MarginRightChevron = styled.span`
  margin-right: 5px;
`

export default HistoriskeHendelser
