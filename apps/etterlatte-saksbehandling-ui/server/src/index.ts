import express from "express";
import path from "path";
import { appConf } from "./config/config";
import { authMiddleware, authenticateUser } from "./middleware/auth";
import { healthRouter } from "./routers/health";
import { modiaRouter } from "./routers/modia";
import { proxy } from "./routers/proxy";
import { parseJwt } from "./utils/parsejwt";

const app = express();

const clientPath = path.resolve(__dirname, "../client");

app.set("trust proxy", 1);

/*if(process.env.TS_NODE_DEV !== "true") {
    app.use(authenticateUser);
}*/
app.use("/", express.static(clientPath));

app.use(express.json());

app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "http://localhost:3000"); //Todo: fikse domene
    res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.header("Access-Control-Allow-Credentials", "true");
    next();
});

app.use("/health", healthRouter);
app.use("/modiacontextholder/api/", modiaRouter);
app.use("/proxy", authMiddleware, proxy);
app.get("/logintest", authenticateUser, (req: any, res: any) => {
    console.log(req.headers);
    res.json("ok");
});

app.listen(appConf.port, () => {
    console.log(`Server kjører på port ${appConf.port}`);
});
