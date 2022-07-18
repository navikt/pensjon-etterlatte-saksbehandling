import { IVilkaarResultat, VilkaarsType, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentKommerBarnetTilgodeVurderingsTekst } from '../../utils'
import { VurderingsTitle, Undertekst, VurderingsContainer } from '../../styled'
import { CaseworkerfilledIcon } from "../../../../../shared/icons/caseworkerfilledIcon";
import styled from "styled-components";
import { useContext, useState } from "react";
import { Button, Radio, RadioGroup, Textarea } from "@navikt/ds-react";
import { hentBehandling, lagreBegrunnelseKommerBarnetTilgode } from "../../../../../shared/api/behandling";
import { AppContext } from "../../../../../store/AppContext";

export const KommerBarnetTilGodeVurdering =
  ({
     kommerSoekerTilgodeVurdering,
   }: {
    kommerSoekerTilgodeVurdering: IVilkaarResultat
  }) => {
    const behandlingId = useContext(AppContext).state.behandlingReducer.id

    const [redigeringsModus, setRedigeringsModus] = useState(false)
    const [svar, setSvar] = useState<ISvar>()
    const [radioError, setRadioError] = useState<string>()
    const [begrunnelse, setBegrunnelse] = useState<string>('')
    const [begrunnelseError, setBegrunnelseError] = useState<string>()

    enum ISvar {
      JA = 'JA',
      NEI = 'NEI',
    }

    function lagreBegrunnelseKlikket() {
      if (!behandlingId) throw new Error('Mangler behandlingsid')
      !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
      begrunnelse.length < 20 ? setBegrunnelseError('Begrunnelsen må være minst 20 tegn') : setBegrunnelseError(undefined)

      if (radioError === undefined && begrunnelseError === undefined && svar !== undefined)
        lagreBegrunnelseKommerBarnetTilgode(behandlingId, begrunnelse, svar.toString()).then((response) => {
          if (response.status === 200) {
            hentBehandling(behandlingId).then((response) => {
              if (response.status === 200) {
                window.location.reload()
              }
            })
          }
        })
    }

    function reset() {
      setRedigeringsModus(false)
      setSvar(undefined)
      setRadioError(undefined)
      setBegrunnelse('')
      setBegrunnelseError(undefined)
    }

    const hentTekst = (): any => {
      const sammeAdresse = kommerSoekerTilgodeVurdering?.vilkaar.find(
        (vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE
      )
      const barnIngenUtland = kommerSoekerTilgodeVurdering?.vilkaar.find(
        (vilkaar) => vilkaar.navn === VilkaarsType.BARN_INGEN_OPPGITT_UTLANDSADRESSE
      )
      const sammeAdresseAvdoed = kommerSoekerTilgodeVurdering?.vilkaar.find(
        (vilkaar) => vilkaar.navn === VilkaarsType.BARN_BOR_PAA_AVDOEDES_ADRESSE
      )

      return hentKommerBarnetTilgodeVurderingsTekst(
        sammeAdresse?.resultat,
        barnIngenUtland?.resultat,
        sammeAdresseAvdoed?.resultat
      )
    }

    const tittel =
      kommerSoekerTilgodeVurdering.resultat !== VurderingsResultat.OPPFYLT
        ? 'Ikke sannsynlig pensjonen kommer barnet til gode'
        : 'Sannsynlig pensjonen kommer barnet til gode'

    return (
      <VurderingsContainer>
        <div>
          {kommerSoekerTilgodeVurdering.resultat && (
            <GyldighetIcon status={kommerSoekerTilgodeVurdering.resultat} large={true}/>
          )}
        </div>
        {redigeringsModus ? (
          <div>
            <VurderingsTitle>Trenger avklaring</VurderingsTitle>
            <Undertekst gray={false}>
              Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
            </Undertekst>
            <RadioGroupWrapper>
              <RadioGroup
                legend=""
                size="small"
                className="radioGroup"
                onChange={(event) => {
                  setSvar(ISvar[event as ISvar])
                  setRadioError(undefined)
                }}
                error={radioError ? radioError : false}
              >
                <div className="flex">
                  <Radio value={ISvar.JA.toString()}>Ja</Radio>
                  <Radio value={ISvar.NEI.toString()}>Nei</Radio>
                </div>
              </RadioGroup>
            </RadioGroupWrapper>
            <Textarea
              style={{padding: '10px', marginBottom: '10px'}}
              label="Begrunnelse"
              hideLabel={false}
              placeholder="Forklar begrunnelsen"
              value={begrunnelse}
              onChange={(e) => {
                setBegrunnelse(e.target.value)
                begrunnelse.length > 19 && setBegrunnelseError(undefined)
              }}
              minRows={3}
              size="small"
              error={begrunnelseError ? begrunnelseError : false}
            />
            <Button
              style={{marginTop: '10px'}}
              variant={"primary"}
              size={"small"}
              onClick={() => lagreBegrunnelseKlikket()}
            >
              Lagre
            </Button>
            <Button
              style={{marginTop: '10px', marginLeft: '10px'}}
              variant={"secondary"}
              size={"small"}
              onClick={() => reset()}
            >
              Avbryt
            </Button>
          </div>
        ) : (
          <div>
            <VurderingsTitle>{tittel}</VurderingsTitle>
            <Undertekst gray={true}>
              Automatisk {format(new Date(kommerSoekerTilgodeVurdering.vurdertDato), 'dd.MM.yyyy')}
            </Undertekst>
            {kommerSoekerTilgodeVurdering?.resultat === VurderingsResultat.OPPFYLT && (
              <>
                <Undertekst gray={false}>{hentTekst()}</Undertekst>
                <RedigerWrapper onClick={() => setRedigeringsModus(true)}>
                  <CaseworkerfilledIcon/> <span
                  className={"text"}>Legg til vurdering</span>
                </RedigerWrapper>
              </>
            )}
          </div>
        )}

      </VurderingsContainer>
    )
  }

const RedigerWrapper = styled.div`
  display: inline-flex;
  cursor: pointer;
  color: #0056b4;

  .text {
    margin-left: 0.3em;
  }
  
  &:hover {
    text-decoration-line: underline;
  }
`

export const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }
`
