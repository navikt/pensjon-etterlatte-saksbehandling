import { BodyShort, Button, Cell, Grid, Modal, Select, TextField } from "@navikt/ds-react";
import { useContext, useState } from "react";
import { Add } from "@navikt/ds-icons";
import styled from "styled-components";
import { opprettBrev } from "../../../shared/api/brev";
import { useParams } from "react-router-dom";
import { Border } from "../soeknadsoversikt/styled";
import { IBehandlingsopplysning, OpplysningsType } from "../../../store/reducers/BehandlingReducer";
import { AppContext } from "../../../store/AppContext";

const CustomModal = styled(Modal)`
  width: 540px;
`

export default function NyttBrev() {
  const { behandlingId } = useParams()
  const { state } = useContext(AppContext)

  const [isOpen, setIsOpen] = useState<boolean>(false)
  const [mottaker, setMottaker] = useState<any>({})
  const [mal, setMal] = useState<any>({})

  const gyldigeTyper = [OpplysningsType.innsender, OpplysningsType.soeker_pdl]

  const grunnlagListe: IBehandlingsopplysning[] = state.behandlingReducer.grunnlag
      .filter(grunnlag => gyldigeTyper.includes(grunnlag.opplysningType))

  const opprett = () => {
    if (!mal) return

    opprettBrev(behandlingId!!, mottaker, mal)
        .then(res => console.log(res))
        .finally(() => setIsOpen(false))
  }

  const oppdaterMottaker = (fnr: string) => {
    const opplysning = grunnlagListe.find(v => v.opplysning.foedselsnummer === fnr)!!.opplysning

    setMottaker({
      ...mottaker,
      fornavn: opplysning.fornavn,
      etternavn: opplysning.etternavn
    })
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
          Nytt brev &nbsp;<Add/>
        </Button>

        <CustomModal open={isOpen} onClose={() => setIsOpen(false)}>
          <Modal.Content>
            <h1>Opprett nytt brev</h1>

            <BodyShort>
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur sed ante sit amet tellus aliquet
              mattis. Donec blandit, urna ac vulputate tincidunt, lorem massa tempor lectus, nec porttitor velit nunc ac
              ex. Vivamus vel elementum magna. Nullam tristique nisl sit amet ante interdum, vitae tincidunt libero
              placerat.
            </BodyShort>

            <br/>
            <br/>

            <Select label={'Mal'} size={'medium'} onChange={(e) => setMal(e.target.value)}>
              <option value={undefined}>Velg mal ...</option>
              <option value={'verge-dokumentasjon-nb'}>Dokumentasjon om vergemål</option>
              <option value={'innvilget-nb'}>Vedtak om innvilget barnepensjon</option>
              <option value={'avslag-nb'}>Vedtak om avslått barnepensjon</option>
            </Select>

            <br/>
            <br/>
            <Border/>
            <br/>

            <Select label={'Velg mottaker'} onChange={(e) => oppdaterMottaker(e.target.value)}>
              <option value={''}></option>
              {grunnlagListe.map((v, i) => (
                  <option key={i} value={v.opplysning.foedselsnummer}>
                    {v.opplysning.fornavn} {v.opplysning.etternavn} ({type(v.opplysningType)})
                  </option>
              ))}
            </Select>

            <br/>
            <br/>
            <Border/>
            <br/>

            <>
              <Grid>
                <Cell xs={12}>
                  <TextField
                      label={'Fornavn'}
                      value={mottaker.fornavn || ''}
                      onChange={(e) => setMottaker({ ...mottaker, fornavn: e.target.value })}/>
                </Cell>
                <Cell xs={12}>
                  <TextField
                      label={'Etternavn'}
                      value={mottaker.etternavn || ''}
                      onChange={(e) => setMottaker({ ...mottaker, etternavn: e.target.value })}/>
                </Cell>
              </Grid>

              <br/>

              <Grid>
                <Cell xs={12}>
                  <TextField
                      label={'Adresse'}
                      value={mottaker.adresse?.adresse || ''}
                      onChange={(e) => setMottaker({
                        ...mottaker,
                        adresse: { ...mottaker.adresse, adresse: e.target.value }
                      })}/>
                </Cell>

                <Cell xs={4}>
                  <TextField
                      label={'Postnummer'}
                      value={mottaker.adresse?.postnummer || ''}
                      onChange={(e) => setMottaker({
                        ...mottaker,
                        adresse: { ...mottaker.adresse, postnummer: e.target.value }
                      })}/>
                </Cell>

                <Cell xs={8}>
                  <TextField
                      label={'Poststed'}
                      value={mottaker.adresse?.poststed || ''}
                      onChange={(e) => setMottaker({
                        ...mottaker,
                        adresse: { ...mottaker.adresse, poststed: e.target.value }
                      })}/>
                </Cell>
              </Grid>
            </>

            <br/>
            <br/>

            <Button variant={'primary'} style={{ float: 'right' }} onClick={opprett}>
              Lagre
            </Button>
            <br/>
            <br/>
          </Modal.Content>
        </CustomModal>
      </>
  )
}
