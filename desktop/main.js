const { app, BrowserWindow, protocol, net, ipcMain, shell } = require("electron");
const path = require("path");
const { pathToFileURL } = require("url");

// "app://gastei/" serve os arquivos do app: assim os dados (localStorage) ficam salvos no PC.
protocol.registerSchemesAsPrivileged([{ scheme: "app", privileges: { standard: true, secure: true, supportFetchAPI: true } }]);
app.setName("Gastei");
if (process.platform === "win32") app.setAppUserModelId("com.gastei.pc");
// só uma janela do Gastei aberta por vez
if (!app.requestSingleInstanceLock()) app.quit();
else app.on("second-instance", () => { const w = BrowserWindow.getAllWindows()[0]; if (w) { if (w.isMinimized()) w.restore(); w.focus(); } });

function createWindow() {
  const win = new BrowserWindow({
    width: 430, height: 880, minWidth: 360, minHeight: 640,
    frame: false, transparent: true, resizable: true, hasShadow: false,
    backgroundColor: "#00000000", title: "Gastei", icon: path.join(__dirname, "app", "icon-512.png"),
    webPreferences: { nodeIntegration: true, contextIsolation: false }
  });
  win.loadURL("app://gastei/phone.html");
  win.webContents.setWindowOpenHandler(({ url }) => { if (/^https?:/.test(url)) shell.openExternal(url); return { action: "deny" }; });
  ipcMain.on("win", (e, cmd) => { if (cmd === "close") win.close(); if (cmd === "min") win.minimize(); });
}

app.whenReady().then(() => {
  protocol.handle("app", (req) => {
    let p = decodeURIComponent(new URL(req.url).pathname);
    if (p === "/" || p === "") p = "/app/index.html";
    const file = p === "/phone.html" ? path.join(__dirname, "phone.html") : path.join(__dirname, p.startsWith("/app/") ? p : "/app" + p);
    return net.fetch(pathToFileURL(file).toString());
  });
  createWindow();
});
app.on("window-all-closed", () => app.quit());
