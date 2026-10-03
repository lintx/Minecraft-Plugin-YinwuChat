// Author: Soidraw. Real Vue/Chromium smoke test; all chat traffic is synthetic and local.
// node src/test/web/browser-smoke.mjs <chrome executable> <vue.global.js>
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { readFileSync, mkdtempSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import assert from 'node:assert/strict';

const [chrome, vuePath] = process.argv.slice(2);
if (!chrome || !vuePath) throw new Error('Pass a Chromium executable and a local Vue global build.');
const root = fileURLToPath(new URL('../../../', import.meta.url));
let html = readFileSync(join(root, 'src/main/resources/web/index.html'), 'utf8');
html = html.replace(/<script src="[^"]+"><\/script>/g, tag => tag.includes('vue@3') ? '<script src="/vue.js"></script>' : '');
html = html.replace('<head>', `<head><script>
window.tailwind = {}; window.lucide = { createIcons() {} };
window.__YINWUCHAT_WS_URL__ = 'wss://fixture.example.test/new-chat/ws';
window.testSockets = [];
window.WebSocket = class {
    constructor(url) { this.url = url; this.readyState = 0; this.sent = []; testSockets.push(this); }
    send(raw) { this.sent.push(JSON.parse(raw)); }
    close() { this.readyState = 3; }
    open() { this.readyState = 1; this.onopen?.({}); }
    receive(data) { this.onmessage?.({data: JSON.stringify(data)}); }
};
</script>`);
const vue = readFileSync(vuePath);
const server = createServer((req, res) => {
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'none'");
    if (req.url === '/vue.js') { res.setHeader('Content-Type', 'text/javascript'); res.end(vue); }
    else if (req.url === '/new-chat/') { res.setHeader('Content-Type', 'text/html; charset=utf-8'); res.end(html); }
    else { res.writeHead(204); res.end(); }
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const profile = mkdtempSync(join(root, 'target/browser-smoke-'));
const child = spawn(chrome, ['--headless=new', '--disable-gpu', '--no-first-run', '--disable-background-networking',
    '--remote-debugging-port=0', '--remote-allow-origins=*', `--user-data-dir=${profile}`, 'about:blank'], { windowsHide: true, stdio: 'ignore' });
let socket;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
try {
    const portFile = join(profile, 'DevToolsActivePort');
    for (let i = 0; i < 200 && !existsSync(portFile); i++) await delay(50);
    const port = readFileSync(portFile, 'utf8').split('\n')[0];
    const target = await (await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })).json();
    socket = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
    let sequence = 0;
    const pending = new Map(), exceptions = [], vueWarnings = [];
    socket.onmessage = event => {
        const message = JSON.parse(event.data);
        if (message.id && pending.has(message.id)) {
            const { resolve, reject, timer } = pending.get(message.id);
            clearTimeout(timer); pending.delete(message.id);
            message.error ? reject(new Error(JSON.stringify(message.error))) : resolve(message.result);
        }
        if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails);
        if (message.method === 'Runtime.consoleAPICalled' && ['error', 'warning'].includes(message.params.type)) {
            const values = message.params.args.map(arg => arg.value || arg.description).join(' ');
            if (values.includes('[Vue warn]')) vueWarnings.push(values);
        }
    };
    const cdp = (method, params = {}) => new Promise((resolve, reject) => {
        const id = ++sequence;
        const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timeout: ${method}`)); }, 10000);
        pending.set(id, { resolve, reject, timer });
        socket.send(JSON.stringify({ id, method, params }));
    });
    const evaluate = async expression => {
        const result = await cdp('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
        if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
        return result.result.value;
    };
    await cdp('Runtime.enable');
    await cdp('Page.navigate', { url: `http://127.0.0.1:${server.address().port}/new-chat/` });
    let mounted = false;
    for (let i = 0; i < 100; i++) {
        mounted = await evaluate("!!document.querySelector('#app')?.__vue_app__?._instance?.proxy");
        if (mounted) break;
        await delay(50);
    }
    assert.ok(mounted, 'Vue should mount');
    await evaluate(`(async () => {
        window.app = document.querySelector('#app').__vue_app__._instance.proxy;
        app.currentUser = 'test-web'; await app.connect();
        window.oldSocket = testSockets.at(-1); oldSocket.open();
        oldSocket.receive({action:'check_token', status:0});
        oldSocket.receive({action:'self_info', player:'Alice'});
        oldSocket.receive({action:'read_cursor_snapshot', public_last_read_id:0});
        oldSocket.receive({action:'send_message', message_id:102, player:'Bob', message:'newer-message', time:1767268802000});
        oldSocket.receive({action:'send_message', message_id:101, player:'Bob', message:'older-message', time:1767268801000, replay:true});
        oldSocket.receive({action:'send_message', message_id:102, player:'Bob', message:'newer-message', time:1767268802000, replay:true});
        await Vue.nextTick();
    })()`);
    const text = await evaluate('document.body.innerText');
    assert.ok(text.includes('older-message') && text.indexOf('older-message') < text.indexOf('newer-message'));
    assert.equal(text.split('newer-message').length - 1, 1);
    await evaluate(`(async () => {
        [...document.querySelectorAll('button')].find(b => b.textContent.trim() === '清除缓存').click();
        await Vue.nextTick();
        [...document.querySelectorAll('button')].find(b => b.textContent.trim() === '确认清除').click();
        await Vue.nextTick();
    })()`);
    assert.equal(await evaluate('app.messages.length'), 0);
    await evaluate(`(async () => {
        oldSocket.receive({action:'send_message', message_id:102, player:'Bob', message:'replayed-after-clear', time:1767268802000, replay:true});
        await Vue.nextTick();
    })()`);
    assert.ok((await evaluate('document.body.innerText')).includes('replayed-after-clear'));
    await evaluate(`(async () => {
        await app.switchGameAccountFromDrawer({token:'synthetic-charlie'});
        const fresh = testSockets.at(-1); fresh.open();
        fresh.receive({action:'check_token', status:0}); fresh.receive({action:'self_info', player:'Charlie'});
        oldSocket.receive({action:'send_message', message_id:999, player:'Bob', message:'must-not-leak', time:1767268802000});
        oldSocket.onclose({code:1000});
        await Vue.nextTick();
    })()`);
    assert.equal(await evaluate('app.isConnected'), true);
    assert.equal(await evaluate('app.messages.length'), 0);
    assert.ok(!(await evaluate('document.body.innerText')).includes('must-not-leak'));
    assert.deepEqual(exceptions, []);
    assert.deepEqual(vueWarnings, []);
    console.log('Chromium/Vue smoke passed: mount, ordered replay, deduplication, clear button, replay after clear, account switch, stale callbacks; no Vue warnings or JS exceptions.');
} finally {
    socket?.close();
    child.kill();
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
}
