import { Button, Checkbox, Loader, Modal, Panel, Select } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Adresse, hentMottakere, Mottaker, opprettBrevFraPDF } from '../../../../shared/api/brev'
import { useParams } from 'react-router-dom'
import { Border } from '../../soeknadsoversikt/styled'
import { Column, GridContainer } from '../../../../shared/styled'
import { ManuellAdresse } from './manuell-adresse'
import { PdfVisning } from '../pdf-visning'

const CustomModal = styled(Modal)`
  min-width: 540px;
`

interface DefaultMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

export default function LastOppBrev({ leggTilNytt }: { leggTilNytt: (brev: any) => void }) {
  const { behandlingId } = useParams()

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [benyttAdresse, setBenyttAdresse] = useState<boolean>(false)
  const [klarForLagring, setKlarforLagring] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse | undefined>(undefined)
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [laster, setLaster] = useState(false)
  const [error, setError] = useState<string>()
  const [fileURL, setFileURL] = useState<string>()
  const [selectedFile, setSelectedFile] = useState<any>()

  useEffect(() => {
    hentMottakere().then((res) => setMottakere(res))
  }, [])

  const opprett = () => {
    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker,
      orgnummer: orgMottaker,
      adresse: benyttAdresse ? adresse : undefined,
    }

    const formData = new FormData()

    formData.append('file', selectedFile, selectedFile.name)
    formData.append('mottaker', JSON.stringify(brevMottaker))

    opprettBrevFraPDF(behandlingId!!, brevMottaker, formData)
      .then((data) => leggTilNytt(data))
      .catch((error) => console.error(error))
      .finally(() => setIsOpen(false))
  }

  const oppdaterMottaker = (id: string, idType: string) => {
    //todo: fiks at man ikke kan velge org og fnr samtidig.
    setFnrMottaker(idType === 'FNR' ? id : undefined)
    setOrgMottaker(idType === 'ORGNR' ? id : undefined)
    setBenyttAdresse(false)
    setAdresse(undefined)
    setKlarforLagring(false)
  }

  const onFileChange = (event: any) => {
    // Update the state
    const file = event.target.files[0]
    setSelectedFile(file)
    const pdfUrl = URL.createObjectURL(file)
    setFileURL(pdfUrl)
  }

  const fileData = () => {
    if (selectedFile) {
      return (
        <div>
          <h2>Brev detaljer:</h2>
          <p>Filnavn: {selectedFile.name}</p>
          <p>Filtype: {selectedFile.type}</p>
        </div>
      )
    } else {
      return (
        <div>
          <br />
          <h4>Velg en fil å laste opp</h4>
        </div>
      )
    }
  }

  useEffect(() => {
    if (fileURL && (fnrMottaker || orgMottaker || adresse)) setKlarforLagring(true)
  }, [fileURL, fnrMottaker, orgMottaker, adresse])

  return (
    <>
      <Button variant={'secondary'} onClick={() => setIsOpen(true)}>
        Last opp brev &nbsp;
        <Add />
      </Button>

      <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <GridContainer>
            <Column style={{ width: '500px', paddingRight: '20px' }}>
              <h1>Last opp brev</h1>

              <br />

              <div>
                <input type="file" onChange={onFileChange} accept={'application/pdf'} />
              </div>
              {fileData()}

              <br />
              {!benyttAdresse && (
                <>
                  <Border />
                  <br />
                  <h2>Mottaker</h2>

                  <Select
                    label={'Velg person fra behandlingen'}
                    value={fnrMottaker}
                    onChange={(e) => oppdaterMottaker(e.target.value, 'FNR')}
                  >
                    <option value={undefined}></option>
                    {mottakere
                      .filter((m) => m.idType === 'FNR')
                      .map((m, i) => (
                        <option key={i} value={m.id}>
                          {m.navn} ({m.id})
                        </option>
                      ))}
                  </Select>
                  <br />

                  <Select
                    label={'Velg organisasjon'}
                    value={orgMottaker}
                    onChange={(e) => oppdaterMottaker(e.target.value, 'ORGNR')}
                  >
                    <option value={undefined}></option>
                    {mottakere
                      .filter((m) => m.idType === 'ORGNR')
                      .map((m, i) => (
                        <option key={i} value={m.id}>
                          {m.navn} ({m.id})
                        </option>
                      ))}
                  </Select>
                </>
              )}

              <br />
              <Border />

              <Panel border>
                <Checkbox
                  checked={benyttAdresse}
                  onChange={(event) => {
                    const benyttAdresse = event.target.checked
                    setAdresse(benyttAdresse ? {} : undefined)
                    setOrgMottaker(undefined)
                    setFnrMottaker(undefined)
                    setBenyttAdresse(benyttAdresse)
                    setKlarforLagring(false)
                  }}
                >
                  Skriv inn mottaker og adresse manuelt
                </Checkbox>
              </Panel>
              <br />
              <br />

              {adresse !== undefined && <ManuellAdresse adresse={adresse} setAdresse={setAdresse} />}

              <br />
              <br />

              <Button
                variant={'primary'}
                style={{ float: 'right' }}
                onClick={opprett}
                disabled={laster || !klarForLagring}
              >
                Lagre {laster && <Loader />}
              </Button>
              <br />
              <br />
            </Column>
            <Column style={{ paddingLeft: '20px', marginTop: '100px' }}>
              <PdfVisning fileUrl={fileURL} error={error} />
            </Column>
          </GridContainer>
        </Modal.Content>
      </CustomModal>
    </>
  )
}
