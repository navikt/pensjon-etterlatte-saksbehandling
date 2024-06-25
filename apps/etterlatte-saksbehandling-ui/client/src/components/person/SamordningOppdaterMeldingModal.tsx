import {
    BodyShort,
    Button,
    Heading,
    HStack, Label,
    Modal,
    Radio,
    RadioGroup,
    VStack
} from "@navikt/ds-react";
import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {useApiCall} from "~shared/hooks/useApiCall";
import {isPending, isSuccess, mapFailure} from "~shared/api/apiUtils";
import {ApiErrorAlert} from "~ErrorBoundary";
import {oppdaterSamordningsmeldingForSak} from "~shared/api/vedtaksvurdering";
import {DocPencilIcon} from "@navikt/aksel-icons";
import {Samordningsmelding} from "~components/vedtak/typer";
import {JaNei} from "~shared/types/ISvar";
import {Toast} from "~shared/alerts/Toast";

export default function SamordningOppdaterMeldingModal({fnr, sakId, mld, refresh}: {
    fnr: string,
    sakId: number,
    mld: Samordningsmelding,
    refresh: () => void
}) {
    const [open, setOpen] = useState(false)
    const navigate = useNavigate()

    const [erRefusjonskrav, setErRefusjonskrav] = useState<JaNei>()
    const [oppdaterSamordningsmeldingStatus, oppdaterSamordningsmelding] = useApiCall(oppdaterSamordningsmeldingForSak)

    const oppdaterMelding = () => {
        oppdaterSamordningsmelding({
            sakId: sakId,
            oppdaterSamordningsmelding: {
                samId: mld.samId,
                pid: fnr,
                tpNr: mld.tpNr,
                refusjonskrav: erRefusjonskrav === JaNei.JA,
                periodisertBelopListe: []
            },
        }, () => {
            setOpen(false)
            refresh()
        })
    }

    return (
        <>
            <Button
                variant="primary"
                icon={<DocPencilIcon title="Overstyr"/>}
                onClick={() => setOpen(true)}
            />

            <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
                <Modal.Body>
                    <Heading size="medium" id="modal-heading" spacing>
                        Overstyr samordningsmelding
                    </Heading>

                    <VStack gap="4">
                        <HStack gap="4">
                            <Label>SamordningsmeldingID:</Label>
                            <BodyShort>{mld.samId}</BodyShort>
                        </HStack>
                        <HStack gap="4">
                            <Label>Tjenestepensjonsordning:</Label>
                            <BodyShort>{mld.tpNr}</BodyShort>
                        </HStack>

                        <RadioGroup
                            legend="Refusjonskrav?"
                            onChange={(event) => {
                                setErRefusjonskrav(JaNei[event as JaNei])
                            }}
                            value={erRefusjonskrav || ''}
                        >
                            <HStack gap="4">
                                <Radio size="small" value={JaNei.JA}>Ja</Radio>
                                <Radio size="small" value={JaNei.NEI}>Nei</Radio>
                            </HStack>
                        </RadioGroup>

                        {isSuccess(oppdaterSamordningsmeldingStatus) ? (
                            <>
                                <Toast melding="Melding oppdatert"/>

                                <HStack gap="4" justify="center">
                                    <Button variant="primary"
                                            onClick={() => navigate(`/person/${fnr}/?fane=SAMORDNING`)}>
                                        Gå til samordningsoversikten
                                    </Button>
                                </HStack>
                            </>
                        ) : (
                            <HStack gap="4" justify="center">
                                <Button variant="secondary" onClick={() => setOpen(false)}
                                        disabled={isPending(oppdaterSamordningsmeldingStatus)}>
                                    Avbryt
                                </Button>
                                <Button
                                    variant="primary"
                                    disabled={erRefusjonskrav === undefined}
                                    onClick={oppdaterMelding}
                                    loading={isPending(oppdaterSamordningsmeldingStatus)}
                                >
                                    Lagre
                                </Button>
                            </HStack>
                        )}

                        {mapFailure(oppdaterSamordningsmeldingStatus, (error) => (
                            <Modal.Footer>
                                <ApiErrorAlert>{error.detail || 'Det oppsto en feil ved oppdatering av samordningsmelding'}</ApiErrorAlert>
                            </Modal.Footer>
                        ))}
                    </VStack>
                </Modal.Body>
            </Modal>
        </>
    )
}
