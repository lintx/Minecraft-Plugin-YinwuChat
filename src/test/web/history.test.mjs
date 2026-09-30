// Author: Soidraw. Run the actual inline Vue setup with deterministic browser/WS adapters.
// No requests, credentials, or production chat messages are used.
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import test from 'node:test';
import assert from 'node:assert/strict';

const root = new URL('../../../', import.meta.url);
const html = readFileSync(new URL('src/main/resources/web/index.html', root), 'utf8');
const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]).find(s => s.includes('createApp({'));
const epoch = Date.parse('2026-01-01T12:00:00Z');

function client(initial = {}, timeZone = 'UTC') {
    const storage = new Map(Object.entries(initial));
    const sockets = [], timers = [], errors = [], downloads = [];
    let app, history;
    class TestDate extends Date {
        toLocaleTimeString(locales, options) {
            return super.toLocaleTimeString('en-GB', { ...options, timeZone });
        }
    }
    class MockSocket {
        constructor(url) { this.url = url; this.readyState = 0; this.sent = []; sockets.push(this); }
        send(raw) { this.sent.push(JSON.parse(raw)); }
        open() { this.readyState = 1; this.onopen?.({}); }
        receive(data) { this.onmessage?.({ data: JSON.stringify(data) }); }
        close() { this.readyState = 3; } // Deliberately delayed onclose to exercise account-switch races.
    }
    const noop = () => {};
    const context = {
        Date: TestDate, URL, Blob, WebSocket: MockSocket,
        console: { log: noop, warn: noop, error: (...args) => errors.push(args) },
        localStorage: {
            getItem: k => storage.get(k) ?? null,
            setItem: (k, v) => storage.set(k, String(v)), removeItem: k => storage.delete(k),
            clear: () => { throw new Error('Do not clear unrelated origin storage'); }
        },
        window: { location: new URL('https://chat.example.test/new-chat/'), innerWidth: 1200, addEventListener: noop },
        document: {
            addEventListener: noop, removeEventListener: noop, querySelector: () => null,
            documentElement: { classList: { add: noop, toggle: noop } },
            createElement: () => { const link = { click: () => downloads.push(link) }; return link; }
        },
        lucide: { createIcons: noop },
        setTimeout: (fn, ms) => { timers.push({ fn, ms }); return timers.length; }, clearTimeout: noop,
        setInterval: noop, clearInterval: noop,
        Vue: {
            ref: value => ({ value }), computed: fn => ({ get value() { return fn(); } }),
            watch: noop, onMounted: noop, onBeforeUnmount: noop,
            nextTick: fn => { fn?.(); return Promise.resolve(); },
            createApp: options => ({ mount: () => { app = options.setup(); } })
        },
        capture: value => { history = value; }
    };
    runInNewContext(script + '\ncapture(ChatHistory);', context, { filename: 'web/index.html' });
    const login = async (player = 'Alice', account = 'web-a') => {
        app.currentUser.value = account;
        await app.connect();
        const socket = sockets.at(-1);
        socket.open();
        socket.receive({ action: 'check_token', status: 0, isbind: true });
        socket.receive({ action: 'self_info', player, isAdmin: false });
        socket.receive({ action: 'read_cursor_snapshot', public_last_read_id: 0, private_last_read: {} });
        assert.equal(app.isConnected.value, true);
        return socket;
    };
    return { app, history, storage, sockets, timers, errors, downloads, login };
}

const message = (id, extra = {}) => ({ action: 'send_message', message_id: id, player: 'Bob', message: `text ${id}`, time: epoch + id * 1000, ...extra });
const ids = app => Array.from(app.messages.value, m => m.messageId);
const cacheKey = c => [...c.storage.keys()].find(k => k.startsWith('yinwuchat_history_v2:') && !k.endsWith(':read'));

test('public/private replay is sorted by server ID, duplicates enrich data without adding unread counts', async () => {
    const c = client(), ws = await c.login();
    ws.receive(message(102, { time: undefined }));
    ws.receive(message(100, { replay: true }));
    ws.receive(message(101, { action: 'private_message', to: 'Alice', replay: true }));
    ws.receive(message(102, { replay: true, raw_chat: 'corrected text' }));
    ws.receive(message(101, { action: 'private_message', to: 'Alice', replay: true }));
    assert.deepEqual(ids(c.app), [100, 101, 102]);
    assert.equal(c.app.messages.value[2].timestamp, epoch + 102000);
    assert.equal(c.app.messages.value[2].chat, 'corrected text');
    assert.equal(c.app.unreadCount.value.Bob, 1);
    assert.equal(c.app.toasts.value.filter(t => t.msg.includes('私聊')).length, 0);
});

test('raw timestamps survive cache reload and render in the current timezone', async () => {
    const a = client(), ws = await a.login();
    ws.receive(message(1));
    const record = a.app.messages.value[0];
    assert.equal(a.app.formatMessageTime(record.timestamp), '12:00');
    const b = client(Object.fromEntries(a.storage), 'Asia/Shanghai');
    await b.login();
    assert.equal(b.app.messages.value[0].timestamp, epoch + 1000);
    assert.equal(b.app.formatMessageTime(b.app.messages.value[0].timestamp), '20:00');
    assert.equal(b.app.formatMessageTime(undefined), '时间未知');
    assert.equal(b.app.formatMessageTime(undefined, '08:59 AM'), '08:59 AM（旧记录）');
    for (const value of [null, undefined, '', '08:59 AM', '2026-01-01 12:00', Infinity, true, {}]) {
        assert.equal(b.history.timestamp(value), null);
    }
    assert.equal(b.history.timestamp('2026-01-01T20:00:00+08:00'), epoch);
});

test('acknowledgements use the maximum displayed ID, never regress, and do not mark receipt as read', async () => {
    const c = client(), ws = await c.login();
    ws.receive(message(102));
    ws.receive(message(101, { replay: true }));
    // Exercise the ack independently of sorting as well.
    c.app.messages.value.reverse();
    c.app.acknowledgeUnreadForCurrentChat();
    c.app.acknowledgeUnreadForCurrentChat();
    assert.deepEqual(ws.sent.filter(m => m.action === 'read_cursor'), [{ action: 'read_cursor', chat: 'public', message_id: 102 }]);
    ws.receive(message(103));
    c.app.logout();
    const anchor = JSON.parse(c.storage.get([...c.storage.keys()].find(k => k.endsWith(':read'))));
    assert.equal(anchor.public, 102);
    assert.equal(c.app.messages.value.length, 0);
});

test('clearing history resets deduplication while preserving login, settings and unrelated data', async () => {
    const c = client({ unrelated: 'keep', yinwuchat_token: 'test-token', yinwuchat_dark: 'true' }), ws = await c.login();
    ws.receive(message(5));
    const key = cacheKey(c);
    c.app.confirmClearCache();
    assert.equal(c.storage.has(key), false);
    assert.equal(c.app.messages.value.length, 0);
    assert.equal(c.storage.get('unrelated'), 'keep');
    assert.equal(c.storage.get('yinwuchat_token'), 'test-token');
    assert.equal(c.storage.get('yinwuchat_dark'), 'true');
    ws.receive(message(5, { replay: true }));
    assert.deepEqual(ids(c.app), [5]);
});

test('switching players hides old private messages, ignores late old-socket callbacks, restores own cache', async () => {
    const c = client(), old = await c.login();
    old.receive(message(1, { action: 'private_message', player: 'Alice', to: 'Bob', is_self: true }));
    const oldKey = cacheKey(c);
    await c.app.switchGameAccountFromDrawer({ token: 'test-charlie' });
    assert.equal(c.app.messages.value.length, 0);
    const fresh = c.sockets.at(-1);
    fresh.open();
    fresh.receive({ action: 'check_token', status: 0 });
    fresh.receive({ action: 'self_info', player: 'Charlie' });
    old.receive(message(2));
    old.onclose({ code: 1000 });
    old.onopen({});
    assert.equal(c.app.isConnected.value, true);
    assert.equal(c.app.messages.value.length, 0);
    fresh.receive(message(3, { action: 'private_message', player: 'Alice', to: 'Bob' }));
    assert.equal(c.app.messages.value.length, 0);
    fresh.receive(message(4));
    c.app.currentChat.value = 'Bob';
    assert.equal(c.app.currentMessages.value.length, 0);
    c.app.confirmClearCache();
    assert.ok(c.storage.has(oldKey));
    await c.login();
    c.app.currentChat.value = 'bOB';
    assert.equal(c.app.currentMessages.value.length, 1);
    assert.equal(c.app.currentMessages.value[0].isSelf, true);
});

test('cache scopes distinguish server, web account and game account without including tokens', async () => {
    const c = client(), ws = await c.login();
    ws.receive(message(1));
    await c.login('Alice', 'web-b');
    assert.equal(c.app.messages.value.length, 0);
    c.app.manualWsUrl.value = 'wss://other.example.test/new-chat/ws';
    await c.login();
    assert.equal(c.app.messages.value.length, 0);
    c.app.manualWsUrl.value = 'wss://chat.example.test/new-chat/ws';
    await c.login();
    assert.deepEqual(ids(c.app), [1]);
});

test('cache limit keeps the latest 100 by ID even when replay arrives out of order', async () => {
    const c = client(), ws = await c.login();
    for (let id = 150; id >= 1; id--) ws.receive(message(id, { replay: true }));
    const cached = JSON.parse(c.storage.get(cacheKey(c)));
    assert.equal(cached.length, 100);
    assert.equal(cached[0].messageId, 51);
    assert.equal(cached.at(-1).messageId, 150);
});

test('unscoped legacy cache is retained/exportable but never silently assigned to a new account', async () => {
    const legacy = JSON.stringify([{ messageId: 1, chat: 'private legacy', player: 'Someone', to: 'Else', time: '08:59 AM' }]);
    const c = client({ yinwuchat_msg_cache: legacy });
    assert.equal(c.app.messages.value.length, 0);
    await c.login();
    assert.equal(c.app.messages.value.length, 0);
    c.app.exportLegacyCache();
    assert.equal(c.downloads[0].download, 'yinwuchat-legacy-history.json');
    c.app.confirmClearCache();
    assert.equal(c.storage.get('yinwuchat_msg_cache'), legacy);
});

test('malformed cached data does not prevent login or live message processing', async () => {
    const seed = client();
    const key = seed.history.key('wss://chat.example.test/new-chat/ws', 'web-a', 'Alice');
    for (const malformed of ['{', '{}', '[null,1,"x",{}, {"type":"token_bind","chat":"secret"}]']) {
        const c = client({ [key]: malformed }), ws = await c.login();
        assert.equal(c.app.messages.value.length, 0);
        ws.receive(message(7));
        assert.deepEqual(ids(c.app), [7]);
    }
});

test('stale reconnect timer cannot reconnect after logout or after switching accounts', async () => {
    const c = client(), ws = await c.login();
    ws.onclose({ code: 1006 });
    const reconnect = c.timers.find(t => t.ms === 5000);
    assert.ok(reconnect);
    c.app.logout();
    reconnect.fn();
    assert.equal(c.sockets.length, 1);
    await c.login('Charlie');
    reconnect.fn();
    assert.equal(c.sockets.length, 2);
});

test('all client bundles match the server HTML and clear button calls an exposed handler', () => {
    for (const file of ['mobile-app/www/index.html', 'harmonyos-app/entry/src/main/resources/rawfile/index.html']) {
        assert.equal(readFileSync(new URL(file, root), 'utf8'), html);
    }
    assert.ok(!html.includes('messages = []; saveLocalMessages()'));
    assert.equal(typeof client().app.clearCache, 'function');
});
