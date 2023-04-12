import { Adresse } from '~shared/api/brev'
import { Cell, Grid, TextField, Tabs, Select } from '@navikt/ds-react'
import { useState } from 'react'
import { DefaultMottaker } from './last-opp'
import styled from 'styled-components'

const TabDiv = styled.div`
  margin: 1rem 0px;
`

export const MottakerComponent = ({
  adresse,
  mottakere,
  fnrMottaker,
  orgMottaker,
  oppdaterMottaker,
}: {
  adresse?: Adresse
  mottakere: DefaultMottaker[]
  fnrMottaker: string | undefined
  orgMottaker: string | undefined
  oppdaterMottaker: (value: string, id: string, section?: string) => void
}) => {
  const [tabValue, setTabValue] = useState<string>('person')

  return (
    <>
      <Tabs value={tabValue} onChange={setTabValue}>
        <Tabs.List>
          <Tabs.Tab value="person" label="Person" id="person-tab" aria-controls="person-panel" />
          <Tabs.Tab value="orgnr" label="Organisasjon" id="orgnr-tab" aria-controls="orgnr-panel" />
          <Tabs.Tab value="manuell" label="Manuel utfylling" id="manuell-tab" aria-controls="manuell-panel" />
        </Tabs.List>
      </Tabs>
      <TabDiv
        role="tabpanel"
        hidden={tabValue !== 'person'}
        aria-labelledby="person-tab"
        id="person-panel"
        tabIndex={0}
      >
        <Select
          label={'Velg person fra behandlingen'}
          value={fnrMottaker}
          onChange={(e) => oppdaterMottaker(e.target.value, 'FNR')}
        >
          <option value={''}>Velg en person</option>
          {mottakere
            .filter((m) => m.idType === 'FNR')
            .map((m, i) => (
              <option key={i} value={m.id}>
                {m.navn} ({m.id})
              </option>
            ))}
        </Select>
      </TabDiv>
      <TabDiv role="tabpanel" hidden={tabValue !== 'orgnr'} aria-labelledby="orgnr-tab" id="orgnr-panel" tabIndex={0}>
        <Select
          label={'Velg organisasjon'}
          value={orgMottaker}
          onChange={(e) => oppdaterMottaker(e.target.value, 'ORGNR')}
        >
          <option value={''}>Velg en organisasjon</option>
          {mottakere
            .filter((m) => m.idType === 'ORGNR')
            .map((m, i) => (
              <option key={i} value={m.id}>
                {m.navn} ({m.id})
              </option>
            ))}
        </Select>
      </TabDiv>
      <TabDiv
        role="tabpanel"
        hidden={tabValue !== 'manuell'}
        aria-labelledby="manuell-tab"
        id="manuell-panel"
        tabIndex={0}
      >
        {/*        <Grid>
          <Cell xs={12}>
            <TextField
              label={'Fornavn'}
              value={adresse?.fornavn || ''}
              onChange={(e) => oppdaterMottaker(e.target.value, 'ADRESSE', 'fornavn')}
            />
          </Cell>
          <Cell xs={12}>
            <TextField
              label={'Etternavn'}
              value={adresse?.etternavn || ''}
              onChange={(e) => oppdaterMottaker(e.target.value, 'ADRESSE', 'etternavn')}
            />
          </Cell>
        </Grid>*/}

        <br />

        <Grid>
          <Cell xs={12}>
            <TextField
              label={'Adresse'}
              value={adresse?.adresselinje1 || ''}
              onChange={(e) => oppdaterMottaker(e.target.value, 'ADRESSE', 'adresse')}
            />
          </Cell>

          <Cell xs={4}>
            <TextField
              label={'Postnummer'}
              value={adresse?.postnummer || ''}
              onChange={(e) => oppdaterMottaker(e.target.value, 'ADRESSE', 'postnummer')}
            />
          </Cell>

          <Cell xs={8}>
            <TextField
              label={'Poststed'}
              value={adresse?.poststed || ''}
              onChange={(e) => oppdaterMottaker(e.target.value, 'ADRESSE', 'poststed')}
            />
          </Cell>
        </Grid>
      </TabDiv>
    </>
  )
}
