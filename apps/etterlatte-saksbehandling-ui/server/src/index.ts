import express from "express";
import path from "path";
import { loggers } from "winston";
import { appConf } from "./config/config";
import { authMiddleware, authenticateUser } from "./middleware/auth";
import { healthRouter } from "./routers/health";
import { modiaRouter } from "./routers/modia";
import { proxy } from "./routers/proxy";
import { logger } from "./utils/logger";

const app = express();

const clientPath = path.resolve(__dirname, "../client");

app.set("trust proxy", 1);

app.use("/", express.static(clientPath));

// requestlogger-middleware
app.use((req, res, next) => {
    logger.info("Request", {...req.headers, url: req.url})
    next();
});

app.use(express.json());

// cors-middleware
app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "http://localhost:3000"); //Todo: fikse domene
    res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.header("Access-Control-Allow-Credentials", "true");
    next();
});

app.use("/health", healthRouter); // åpen rute
if (process.env.DEVELOPMENT !== "true") {
    app.use(authenticateUser); // Alle ruter etter denne er authenticated
}
app.use("/modiacontextholder/api/", modiaRouter);
app.use("/proxy", authMiddleware, proxy);
app.get("/logintest", authenticateUser, (req: any, res: any) => {
    logger.info("Headers", { ...req.headers });
    res.json("ok");
});

app.listen(appConf.port, () => {
    console.log(`Server kjører på port ${appConf.port}`);
});
