import {Adresse} from "../../../../shared/api/brev";
import {Cell, Grid, TextField} from "@navikt/ds-react";
import {Dispatch, SetStateAction} from "react";

export default function ManuellAdresse(adresse: Adresse, setAdresse: Dispatch<SetStateAction<Adresse | undefined>>) {

    return (
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

            <br/>

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
    )
}