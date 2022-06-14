import { Button, Cell, Grid, Loader, Modal, Select, TextField } from '@navikt/ds-react'
import { useContext, useEffect, useState } from 'react'
import { Add } from '@navikt/ds-icons'
import styled from 'styled-components'
import { Adresse, hentMaler, hentMottakere, Mottaker, nyttBrevForBehandling } from '../../../shared/api/brev'
import { useParams } from 'react-router-dom'
import { Border } from '../soeknadsoversikt/styled'
import { IBehandlingsopplysning, OpplysningsType } from '../../../store/reducers/BehandlingReducer'
import { AppContext } from '../../../store/AppContext'
import { CheckboksPanel } from 'nav-frontend-skjema'

const CustomModal = styled(Modal)`
  width: 540px;
`

interface DefaultMottaker {
  id?: string
  idType?: string
  navn?: string
  land?: string
}

export default function NyttBrev({ leggTilNytt }: { leggTilNytt: (brev: any) => void }) {
  const { behandlingId } = useParams()
  const { state } = useContext(AppContext)

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [benyttAdresse, setBenyttAdresse] = useState<boolean>(false)
  const [adresse, setAdresse] = useState<Adresse | undefined>(undefined)
  const [fnrMottaker, setFnrMottaker] = useState<string | undefined>(undefined)
  const [orgMottaker, setOrgMottaker] = useState<string | undefined>(undefined)
  const [mottakere, setMottakere] = useState<DefaultMottaker[]>([])
  const [mal, setMal] = useState<any>({})
  const [maler, setMaler] = useState<any>([])
  const [laster, setLaster] = useState(false)

  useEffect(() => {
    hentMaler().then((res) => setMaler(res))
    hentMottakere().then((res) => setMottakere(res))
  }, [])

  const gyldigeTyper = [OpplysningsType.innsender, OpplysningsType.soeker_pdl, OpplysningsType.gjenlevende_forelder_pdl]

  const grunnlagListe: IBehandlingsopplysning[] = state.behandlingReducer.grunnlag.filter((grunnlag) =>
    gyldigeTyper.includes(grunnlag.opplysningType)
  )

  const opprett = () => {
    if (!mal) return

    setLaster(true)

    const brevMottaker: Mottaker = {
      foedselsnummer: fnrMottaker,
      orgnummer: orgMottaker,
      adresse: benyttAdresse ? adresse : undefined,
    }

    nyttBrevForBehandling(behandlingId!!, brevMottaker, {
      tittel: maler.find((m: any) => m.navn === mal).tittel,
      navn: mal,
    })
      .then((brev) => leggTilNytt(brev))
      .finally(() => {
        setAdresse(undefined)
        setFnrMottaker(undefined)
        setOrgMottaker(undefined)
        setLaster(false)
        setIsOpen(false)
      })
  }

  const oppdaterMottaker = (id: string, idType: string) => {
    //todo: fiks at man ikke kan velge org og fnr samtidig.
    setFnrMottaker(idType === 'FNR' ? id : undefined)
    setOrgMottaker(idType === 'ORGNR' ? id : undefined)
    setBenyttAdresse(false)
    setAdresse(undefined)
  }

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

  return (
    <>
      <Button variant={'secondary'} onClick={() => setIsOpen(true)}>
        Nytt brev &nbsp;
        <Add />
      </Button>

      <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Content>
          <h1>Opprett nytt brev</h1>

          <br />

          <Select label={'Mal'} size={'medium'} onChange={(e) => setMal(e.target.value)}>
            <option value={undefined}>Velg mal ...</option>
            {maler.map((mal: any, i: number) => (
              <option key={i} value={mal.navn}>
                {mal.tittel}
              </option>
            ))}
          </Select>

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
                {grunnlagListe.map((v, i) => (
                  <option key={i} value={v.opplysning.foedselsnummer}>
                    {v.opplysning.fornavn} {v.opplysning.etternavn} ({type(v.opplysningType)})
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
                {mottakere.map((m, i) => (
                  <option key={i} value={m.id}>
                    {m.navn} ({m.id})
                  </option>
                ))}
              </Select>
            </>
          )}

          <br />
          <Border />

          <CheckboksPanel
            label={'Skriv inn mottaker og adresse manuelt'}
            checked={benyttAdresse}
            onChange={(event) => {
              const benyttAdresse = event.target.checked
              setAdresse(benyttAdresse ? {} : undefined)
              setOrgMottaker(undefined)
              setFnrMottaker(undefined)
              setBenyttAdresse(benyttAdresse)
            }}
          />
          <br />
          <br />

          {adresse !== undefined && (
            <>
              <Grid>
                <h3>Adresse</h3>
                <Cell xs={12}>
                  <TextField
                    label={'Fornavn'}
                    value={adresse?.fornavn || ''}
                    onChange={(e) =>
                      setAdresse({
                        ...adresse,
                        fornavn: e.target.value,
                      })
                    }
                  />
                </Cell>
                <Cell xs={12}>
                  <TextField
                    label={'Etternavn'}
                    value={adresse?.etternavn || ''}
                    onChange={(e) =>
                      setAdresse({
                        ...adresse,
                        etternavn: e.target.value,
                      })
                    }
                  />
                </Cell>
              </Grid>

              <br />

              <Grid>
                <Cell xs={12}>
                  <TextField
                    label={'Adresse'}
                    value={adresse?.adresse || ''}
                    onChange={(e) =>
                      setAdresse({
                        ...adresse,
                        adresse: e.target.value,
                      })
                    }
                  />
                </Cell>

                <Cell xs={4}>
                  <TextField
                    label={'Postnummer'}
                    value={adresse?.postnummer || ''}
                    onChange={(e) =>
                      setAdresse({
                        ...adresse,
                        postnummer: e.target.value,
                      })
                    }
                  />
                </Cell>

                <Cell xs={8}>
                  <TextField
                    label={'Poststed'}
                    value={adresse?.poststed || ''}
                    onChange={(e) =>
                      setAdresse({
                        ...adresse,
                        poststed: e.target.value,
                      })
                    }
                  />
                </Cell>
              </Grid>
            </>
          )}

          <br />
          <br />

          <Button variant={'primary'} style={{ float: 'right' }} onClick={opprett} disabled={laster}>
            Lagre {laster && <Loader />}
          </Button>
          <br />
          <br />
        </Modal.Content>
      </CustomModal>
    </>
  )
}
