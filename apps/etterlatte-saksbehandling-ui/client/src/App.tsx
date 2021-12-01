import React, { useContext, useEffect } from "react";
import { Grid, Cell } from "@navikt/ds-react";
import "@navikt/ds-css";
import "./App.css";
import { AppContext, IAppContext } from "./store/AppContext";
import { login } from "./shared/api/user";
import { Decorator } from "./components/decorator";
import { ws } from "./mocks/wsmock";

ws()

function App() {
    const ctx = useContext<IAppContext>(AppContext);   
    useEffect(() => {
        (async () => {
            //const res = await login();
            //console.log(res);
            ctx.dispatch({type: "login", data: {loggedIn: true, fnr: "09038829766"} })
        })();
    }, []);


    return (
        <div className="app">
            {ctx.state.userReducer.loggedIn ? (
                <>
                    <Decorator />
                    <div className="container">
                        <Grid>
                            <Cell className={"navds-story-cell"} xs={12} sm={6} lg={4}>
                                <h1>De etterlatte</h1>
                            </Cell>
                        </Grid>
                    </div>
                </>
            ): (
                <div>Ikke logget inn</div>
            )}
        </div>
    );
}

export default App;
